# Infrastructure Design — Unit 3: AI + Infra
# 배달비 절약을 위한 음식 공동 구매 앱

**작성일**: 2026-05-24
**단계**: CONSTRUCTION — Infrastructure Design

---

## 확정 인프라 결정

### Q1: S3 버킷 구성 → **B (버킷 2개 분리)**
- `acc1-menu-images-{env}` — 메뉴 이미지 업로드 (Unit 1 연계)
- `acc1-lambda-deploy-{env}` — Lambda 배포 아티팩트(.zip)
- 이미지 버킷: CORS 허용 (모바일 직접 업로드)
- 배포 버킷: 퍼블릭 액세스 차단

### Q2: Lambda 호출 방식 → **Lambda Function URL (auth=NONE)**
- 원인: HTTP API Gateway는 API Key(Usage Plan)를 지원하지 않음
- 대안: Lambda Function URL + 커스텀 시크릿 헤더(`X-Internal-Key`)
- Spring Boot application.yml에 URL + 키 환경변수로 관리
- Lambda 핸들러에서 `X-Internal-Key` 헤더 검증 (일치하지 않으면 403)
- 외부 노출 없음 — URL은 Spring Boot 서버에서만 사용

### Q3: 리전 → **us-east-1**
- 사용자 선호 (익숙한 리전)

---

## 아키텍처 다이어그램

```
[Mini PC — Spring Boot]
        |
        | HTTP POST (X-Internal-Key: {secret})
        v
[Lambda Function URL]
  functions/ai-recommend/
        |
        |-- [Rule Engine] — 규칙 기반 추천 점수 계산
        |-- [Gemini API] — 자연어 설명 텍스트 생성
        v
[Response JSON]
  { restaurants: [...], explanation: "..." }
```

---

## AWS 리소스 목록

| 리소스 | 이름 | 설정 |
|--------|------|------|
| Lambda | `acc1-ai-recommend` | Node.js 20.x, 512MB, 30초 타임아웃 |
| Lambda Function URL | 자동생성 | auth=NONE, CORS 허용 |
| S3 | `acc1-menu-images-prod` | 이미지 스토리지, CORS |
| S3 | `acc1-lambda-deploy-prod` | 배포 아티팩트, 퍼블릭 차단 |

---

## 환경변수 관리

### Lambda 환경변수
```
GEMINI_API_KEY=...
INTERNAL_SECRET_KEY=...   # X-Internal-Key 검증용
```

### Spring Boot application.yml (prod)
```yaml
ai:
  lambda:
    function-url: https://xxxx.lambda-url.us-east-1.on.aws/
    internal-key: ${AI_LAMBDA_INTERNAL_KEY}
```

---

## 배포 방법 (AWS CLI)

```bash
# 1. Lambda 패키징
cd functions/ai-recommend
npm ci && zip -r function.zip .

# 2. S3에 업로드
aws s3 cp function.zip s3://acc1-lambda-deploy-prod/ai-recommend/function.zip --region us-east-1

# 3. Lambda 업데이트
aws lambda update-function-code \
  --function-name acc1-ai-recommend \
  --s3-bucket acc1-lambda-deploy-prod \
  --s3-key ai-recommend/function.zip \
  --region us-east-1

# 4. 최초 생성 시
aws lambda create-function \
  --function-name acc1-ai-recommend \
  --runtime nodejs20.x \
  --role arn:aws:iam::{ACCOUNT}:role/acc1-lambda-role \
  --handler index.handler \
  --s3-bucket acc1-lambda-deploy-prod \
  --s3-key ai-recommend/function.zip \
  --timeout 30 \
  --memory-size 512 \
  --environment Variables="{GEMINI_API_KEY=xxx,INTERNAL_SECRET_KEY=xxx}" \
  --region us-east-1

# 5. Function URL 활성화 (최초 1회)
aws lambda create-function-url-config \
  --function-name acc1-ai-recommend \
  --auth-type NONE \
  --region us-east-1
```

---

## 보안 고려사항

| 항목 | 조치 |
|------|------|
| Lambda URL 외부 노출 | Spring Boot에서만 사용, 환경변수 관리 |
| X-Internal-Key 유출 | Lambda env var 분리, AWS Secrets Manager 고려 (post-MVP) |
| S3 이미지 직접 접근 | Presigned URL 방식 (5분 TTL) |
| 이미지 버킷 퍼블릭 | 차단 유지, CloudFront 통해서만 접근 (Unit 1 연계) |
