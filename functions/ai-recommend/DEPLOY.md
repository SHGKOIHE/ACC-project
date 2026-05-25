# AI Recommend Lambda — 배포 가이드

## 사전 준비

```bash
# AWS CLI 설치 확인
aws --version

# 자격증명 설정 (Lambda 배포 권한 필요)
aws configure
# AWS Access Key ID:
# AWS Secret Access Key:
# Default region name: us-east-1
# Default output format: json

# Node.js 20.x 확인
node --version
```

## 환경변수 준비

```bash
export FUNCTION_NAME=ai-recommend
export REGION=us-east-1
export DEPLOY_BUCKET=acc1-lambda-deploy-prod
export GEMINI_API_KEY=<Gemini API 키>
export INTERNAL_SECRET_KEY=<Spring Boot와 공유하는 내부 시크릿 키>
```

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
  --timeout 5 \
  --memory-size 256 \
  --region ${REGION}
```

### 4. 환경변수 설정

```bash
aws lambda update-function-configuration \
  --function-name ${FUNCTION_NAME} \
  --environment "Variables={GEMINI_API_KEY=${GEMINI_API_KEY},INTERNAL_SECRET_KEY=${INTERNAL_SECRET_KEY}}" \
  --region ${REGION}
```

### 5. Function URL 활성화 (인증 없음 — X-Internal-Key로 자체 검증)

```bash
aws lambda create-function-url-config \
  --function-name ${FUNCTION_NAME} \
  --auth-type NONE \
  --cors '{"AllowOrigins":["*"],"AllowMethods":["POST"],"AllowHeaders":["x-internal-key","content-type"]}' \
  --region ${REGION}
```

Function URL 확인:
```bash
aws lambda get-function-url-config \
  --function-name ${FUNCTION_NAME} \
  --region ${REGION} \
  --query FunctionUrl \
  --output text
```

출력된 URL을 Spring Boot 환경변수 `AI_LAMBDA_FUNCTION_URL`에 설정.

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

Makefile 등록 예시:
```makefile
deploy-lambda:
	cd functions/ai-recommend && \
	npm ci --omit=dev && \
	zip -r function.zip . -x "*.test.js" "*.md" && \
	aws s3 cp function.zip s3://$(DEPLOY_BUCKET)/ai-recommend/function.zip && \
	aws lambda update-function-code \
	  --function-name ai-recommend \
	  --s3-bucket $(DEPLOY_BUCKET) \
	  --s3-key ai-recommend/function.zip \
	  --region us-east-1
```

---

## 동작 확인

```bash
# Function URL 직접 테스트
curl -X POST https://<function-url> \
  -H "Content-Type: application/json" \
  -H "X-Internal-Key: ${INTERNAL_SECRET_KEY}" \
  -d '{
    "roomId": 1,
    "participants": [{"nickname":"짱구","orderItems":[{"name":"치킨","price":15000}]}],
    "filters": {}
  }'
```

예상 응답:
```json
{
  "recommendations": [
    {"rank":1,"restaurantName":"치킨집","score":80,"reason":""},
    {"rank":2,"restaurantName":"피자집","score":50,"reason":""},
    {"rank":3,"restaurantName":"한식집","score":45,"reason":""}
  ],
  "explanation": "..."
}
```
