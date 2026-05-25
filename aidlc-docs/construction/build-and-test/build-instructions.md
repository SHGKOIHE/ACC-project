# Build Instructions
# 배달비 절약을 위한 음식 공동 구매 앱

**작성일**: 2026-05-24

---

## 사전 요구사항

| 도구 | 버전 | 확인 명령 |
|------|------|-----------|
| Java | 17+ | `java -version` |
| Gradle | 8.x (wrapper 사용) | `./gradlew --version` |
| Node.js | 20.x | `node --version` |
| npm | 10.x | `npm --version` |
| AWS CLI | 2.x | `aws --version` |
| Expo CLI | 최신 | `npx expo --version` |

---

## Unit 1 + 2: Spring Boot 백엔드

```bash
cd /home/sohegi/projects/ACC_1/backend

# 의존성 다운로드 + 컴파일
./gradlew build -x test

# 로컬 실행 (H2 인메모리 DB)
./gradlew bootRun

# 또는 JAR 직접 실행
./gradlew bootJar
java -jar build/libs/backend-0.0.1-SNAPSHOT.jar
```

### 환경변수 (로컬 실행 시)
```bash
export AI_LAMBDA_FUNCTION_URL=http://localhost:8081/mock
export AI_LAMBDA_INTERNAL_KEY=dev-secret
# FCM은 로컬에서 NoOpNotificationAdapter가 자동 사용됨
```

---

## Unit 3: Lambda 함수

```bash
cd /home/sohegi/projects/ACC_1/functions/ai-recommend

# 의존성 설치
npm ci

# 로컬 테스트 실행
npm test

# 배포 패키징
npm run build   # npm ci + zip
```

### Lambda 최초 배포 (AWS CLI)
```bash
# 1. S3 버킷 생성 (최초 1회)
aws s3 mb s3://acc1-lambda-deploy-prod --region us-east-1
aws s3 mb s3://acc1-menu-images-prod --region us-east-1

# 2. 패키징
zip -r function.zip . --exclude "*.test.js" --exclude "node_modules/.cache/*"

# 3. Lambda 생성 (최초 1회)
aws lambda create-function \
  --function-name acc1-ai-recommend \
  --runtime nodejs20.x \
  --role arn:aws:iam::{ACCOUNT_ID}:role/acc1-lambda-role \
  --handler index.handler \
  --zip-file fileb://function.zip \
  --timeout 30 \
  --memory-size 512 \
  --environment Variables="{GEMINI_API_KEY=xxx,INTERNAL_SECRET_KEY=xxx}" \
  --region us-east-1

# 4. Function URL 활성화 (최초 1회)
aws lambda create-function-url-config \
  --function-name acc1-ai-recommend \
  --auth-type NONE \
  --region us-east-1

# 5. 이후 업데이트 배포
aws lambda update-function-code \
  --function-name acc1-ai-recommend \
  --zip-file fileb://function.zip \
  --region us-east-1
```

---

## Unit 4: React Native 모바일 앱

```bash
cd /home/sohegi/projects/ACC_1/mobile

# 의존성 설치
npm install

# Expo 개발 서버 실행
npx expo start

# EAS 빌드 (Android APK)
eas build --profile preview --platform android

# EAS 빌드 (iOS)
eas build --profile preview --platform ios
```

### 환경변수
```bash
# mobile/.env
API_BASE_URL=http://{MINI_PC_IP}:8080
KAKAO_APP_KEY=xxx
```

---

## Docker Compose (통합 로컬 실행)

```bash
cd /home/sohegi/projects/ACC_1
docker compose up -d

# 서비스 확인
docker compose ps
docker compose logs backend -f
```
