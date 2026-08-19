# AI Recommend Lambda — 배포 가이드

## 현재 구성

- 함수명: `food-recommend-api`
- 리전: `ap-northeast-2`
- 호출 방식: Spring Boot 백엔드가 AWS SDK로 **직접 Lambda invoke** (Function URL 불필요)
- S3 버킷: `food-app-assets-sj`

## 사전 준비

```bash
aws --version
node --version  # 20.x
```

## 환경변수 준비

```bash
export FUNCTION_NAME=food-recommend-api
export REGION=ap-northeast-2
export DEPLOY_BUCKET=food-app-assets-sj
export INTERNAL_SECRET_KEY=<백엔드 AI_LAMBDA_INTERNAL_KEY와 동일한 값>
export KAKAO_REST_API_KEY=<Kakao Developers REST API 키> # 선택. 미설정 시 카테고리명 기반 폴백 유지
```

> **AI 엔진**: Bedrock (`anthropic.claude-3-haiku-20240307-v1:0`, 리전 `ap-northeast-2`)이 추천 결과 JSON과 설명을 직접 생성합니다. Bedrock 호출 실패 시 Lambda는 기존 규칙 엔진 결과로 폴백합니다.
> API 키 불필요 — Lambda 실행 Role의 IAM 권한으로 접근.

> **실제 식당 후보 (카카오맵 로컬 API)**: `KAKAO_REST_API_KEY` 설정 시, 경희대학교 국제캠퍼스 인근 실제 식당을 카테고리별로 조회해 Bedrock 프롬프트와 규칙 엔진 폴백에 모두 반영합니다(허구 상호명 방지). 키 미설정 시 이 단계는 건너뛰고 기존처럼 카테고리명만 반환합니다. 앱이 이미 지도 표시용 Kakao 지도 SDK(JS 키)를 사용 중이므로, 같은 Kakao Developers 애플리케이션에서 **REST API 키**를 추가로 발급받아 사용하면 됩니다.

---

## Lambda 최초 생성

### 1. 배포 패키지 빌드

```bash
cd functions/ai-recommend
npm ci --omit=dev
zip -r function.zip . -x "*.test.js" "*.md" "node_modules/jest*" "node_modules/.bin*"
```

### 2. S3에 업로드

```bash
aws s3 cp function.zip s3://${DEPLOY_BUCKET}/ai-recommend/function.zip
```

### 3. Lambda 함수 생성

```bash
aws lambda create-function \
  --function-name ${FUNCTION_NAME} \
  --runtime nodejs20.x \
  --role arn:aws:iam::<ACCOUNT_ID>:role/lambda-basic-execution \
  --handler index.handler \
  --code S3Bucket=${DEPLOY_BUCKET},S3Key=ai-recommend/function.zip \
  --timeout 10 \
  --memory-size 256 \
  --region ${REGION}
```

### 4. 환경변수 설정

```bash
aws lambda update-function-configuration \
  --function-name ${FUNCTION_NAME} \
  --environment "Variables={INTERNAL_SECRET_KEY=${INTERNAL_SECRET_KEY},KAKAO_REST_API_KEY=${KAKAO_REST_API_KEY}}" \
  --region ${REGION}
```

### 5. Lambda 실행 Role에 Bedrock 권한 부여

`food-recommend-api`가 추천 생성을 위해 Bedrock을 호출하려면 실행 Role에 권한이 필요:

```bash
aws iam put-role-policy \
  --role-name lambda-basic-execution \
  --policy-name bedrock-invoke-claude \
  --policy-document '{
    "Version":"2012-10-17",
    "Statement":[{
      "Effect":"Allow",
      "Action":"bedrock:InvokeModel",
      "Resource":"arn:aws:bedrock:ap-northeast-2::foundation-model/anthropic.claude-3-haiku-20240307-v1:0"
    }]
  }'
```

> Bedrock 모델 접근은 AWS 콘솔 → Bedrock → Model access에서 `Claude 3 Haiku`를 먼저 활성화해야 함.

### 6. 백엔드 Lambda에 invoke 권한 부여

백엔드(`foodgroup-backend`) Lambda가 이 함수를 직접 호출하므로 실행 Role에 권한 필요:

```bash
# foodgroup-backend의 Role에 아래 정책 추가
aws iam put-role-policy \
  --role-name lambda-backend-role \
  --policy-name invoke-ai-recommend \
  --policy-document '{
    "Version":"2012-10-17",
    "Statement":[{
      "Effect":"Allow",
      "Action":"lambda:InvokeFunction",
      "Resource":"arn:aws:lambda:ap-northeast-2:<ACCOUNT_ID>:function:food-recommend-api"
    }]
  }'
```

---

## 업데이트 배포

```bash
cd functions/ai-recommend
npm ci --omit=dev
zip -r function.zip . -x "*.test.js" "*.md" "node_modules/jest*" "node_modules/.bin*"
aws s3 cp function.zip s3://${DEPLOY_BUCKET}/ai-recommend/function.zip
aws lambda update-function-code \
  --function-name ${FUNCTION_NAME} \
  --s3-bucket ${DEPLOY_BUCKET} \
  --s3-key ai-recommend/function.zip \
  --region ${REGION}
```

---

## 백엔드 환경변수 설정

백엔드 Lambda(`foodgroup-backend`)에 설정해야 할 환경변수:

| 키 | 값 |
|---|---|
| `AI_LAMBDA_FUNCTION_NAME` | `food-recommend-api` |
| `AI_LAMBDA_INTERNAL_KEY` | INTERNAL_SECRET_KEY와 동일한 값 |

```bash
# 현재 환경변수 확인
aws lambda get-function-configuration \
  --function-name foodgroup-backend \
  --region ap-northeast-2 \
  --query 'Environment.Variables'
```

---

## 동작 확인

```bash
# AWS CLI로 직접 invoke 테스트
aws lambda invoke \
  --function-name ${FUNCTION_NAME} \
  --region ${REGION} \
  --payload "$(echo '{
    "headers": {"x-internal-key": "'${INTERNAL_SECRET_KEY}'"},
    "body": "{\"participants\":[{\"nickname\":\"짱구\",\"orderItems\":[{\"name\":\"치킨\",\"price\":15000}]}],\"filters\":{}}"
  }' | base64)" \
  /tmp/ai-response.json && cat /tmp/ai-response.json
```

예상 응답 (Lambda가 API Gateway 형식으로 반환):
```json
{
  "statusCode": 200,
  "body": "{\"recommendations\":[{\"rank\":1,\"restaurantName\":\"치킨집\",\"matchScore\":80,\"reason\":\"\"}],\"explanation\":\"...\"}"
}
```
