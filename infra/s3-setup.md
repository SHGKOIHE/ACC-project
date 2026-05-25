# S3 버킷 설정 가이드

## 버킷 구성

| 버킷명 | 용도 | 접근 |
|--------|------|------|
| `acc1-menu-images-prod` | 음식 이미지 에셋 | CloudFront OAC |
| `acc1-lambda-deploy-prod` | Lambda 배포 아티팩트 | Private (서버만) |

---

## 1. 이미지 버킷 생성 (acc1-menu-images-prod)

```bash
# 버킷 생성
aws s3api create-bucket \
  --bucket acc1-menu-images-prod \
  --region us-east-1

# 퍼블릭 액세스 차단 (CloudFront OAC로만 접근)
aws s3api put-public-access-block \
  --bucket acc1-menu-images-prod \
  --public-access-block-configuration \
    "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

# CORS 설정 (CloudFront 경유 업로드용)
aws s3api put-bucket-cors \
  --bucket acc1-menu-images-prod \
  --cors-configuration '{
    "CORSRules": [{
      "AllowedOrigins": ["https://<cloudfront-domain>"],
      "AllowedMethods": ["GET", "HEAD"],
      "AllowedHeaders": ["*"],
      "MaxAgeSeconds": 3600
    }]
  }'
```

### CloudFront OAC 연결 (콘솔 권장)

1. CloudFront 콘솔 → Origins → acc1-menu-images-prod 선택
2. Origin access → **Origin access control (OAC)** 선택
3. 새 OAC 생성 → Sign requests 활성화
4. S3 버킷 정책 업데이트 (CloudFront에서 자동 생성된 정책 복사)

---

## 2. Lambda 배포 버킷 생성 (acc1-lambda-deploy-prod)

```bash
# 버킷 생성
aws s3api create-bucket \
  --bucket acc1-lambda-deploy-prod \
  --region us-east-1

# 퍼블릭 액세스 완전 차단
aws s3api put-public-access-block \
  --bucket acc1-lambda-deploy-prod \
  --public-access-block-configuration \
    "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

# 수명주기 정책: 30일 후 오래된 배포 아티팩트 자동 삭제
aws s3api put-bucket-lifecycle-configuration \
  --bucket acc1-lambda-deploy-prod \
  --lifecycle-configuration '{
    "Rules": [{
      "ID": "delete-old-artifacts",
      "Status": "Enabled",
      "Filter": {"Prefix": ""},
      "Expiration": {"Days": 30}
    }]
  }'

# Lambda 실행 역할에 버킷 읽기 권한 부여 (인라인 정책 예시)
aws iam put-role-policy \
  --role-name lambda-basic-execution \
  --policy-name s3-deploy-read \
  --policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Action": ["s3:GetObject"],
      "Resource": "arn:aws:s3:::acc1-lambda-deploy-prod/*"
    }]
  }'
```

---

## 3. 기존 백업 버킷 확인

`infrastructure/scripts/backup.sh`에서 사용하는 백업 버킷은 별도로 관리됩니다.  
백업 버킷도 퍼블릭 액세스 차단 및 수명주기 정책(30일)을 동일하게 적용할 것을 권장합니다.

```bash
# 기존 백업 버킷 수명주기 정책 적용 예시
aws s3api put-bucket-lifecycle-configuration \
  --bucket <backup-bucket-name> \
  --lifecycle-configuration '{
    "Rules": [{
      "ID": "delete-old-backups",
      "Status": "Enabled",
      "Filter": {"Prefix": ""},
      "Expiration": {"Days": 30}
    }]
  }'
```
