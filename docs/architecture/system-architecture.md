# System Architecture
# 배달비 절약 음식 공동구매 앱

**최종 수정**: 2026-06-01
**기술 스택**: Spring Boot 3.3 / Java 17 (AWS Lambda), React Native (Expo), AWS Lambda (Node.js 20), DynamoDB, Lightsail

---

## 1. 전체 시스템 구성도

```
                        [React Native App]
                        (Expo / Android)
                               |
              +----------------+------------------+
              |                |                  |
              v                v                  v
     [API Gateway]      [Lightsail]         [Kakao 지도 SDK]
  execute-api.ap-      43.201.33.167        (WebView 직접 호출)
  northeast-2.aws       (WebSocket)
              |                |
              v                v
        [Lambda]         [chat-server]
    foodgroup-backend   (Spring Boot WebSocket)
      (java17, 1GB)             |
              |                 v
              v          [DynamoDB]
        [DynamoDB]       ChatMessages
   Members / Rooms /      (roomId HASH,
   RoomParticipants /     createdAtId RANGE,
   OrderItems /           TTL 30일)
   Settlements /
   MemberSettlements
              |
              v
        [Lambda]
    food-recommend-api
      (nodejs20.x)
              |
              v
     [Bedrock Claude 3 Haiku]
      (anthropic.claude-3-haiku,
       ap-northeast-2)
              |
         [S3 Bucket]
       food-app-assets-sj
       (Lambda 배포 JAR)

[AWS SES] — 이메일 인증
[FCM]     — 푸시 알림
```

---

## 2. 컴포넌트 간 통신 흐름

### 2-1. 일반 API 요청

```
React Native
    |
    | HTTPS  X-Device-Token 헤더
    v
API Gateway (40ocxlcwfl.execute-api.ap-northeast-2.amazonaws.com)
    |
    | Lambda invoke
    v
Spring Boot on Lambda (foodgroup-backend, java17, 1GB, 60s timeout)
    |
    +---> DynamoDB (AWS SDK Enhanced Client)
    |
    v
ApiResponse<T> JSON 응답
```

### 2-2. 실시간 채팅 (WebSocket)

```
React Native (STOMP over WebSocket)
    |
    | ws://43.201.33.167
    v
Lightsail (nginx) → chat-server (Spring Boot WebSocket)
    |
    +---> /topic/room/{roomId}/chat       채팅 메시지 브로드캐스트
    +---> /topic/room/{roomId}/members    참여자 변경 알림
    |
    v
DynamoDB ChatMessages (읽기/쓰기, TTL 30일)
```

### 2-3. AI 추천 요청

```
React Native
    |
    | POST /api/ai/recommend
    v
API Gateway → Lambda (foodgroup-backend)
    |
    | AWS SDK 직접 invoke (X-Internal-Key 헤더 포함)
    v
Lambda (food-recommend-api, nodejs20.x)
    |
    | 규칙 엔진 → Bedrock Claude 3 Haiku (설명 생성)
    v
{ recommendations, explanation } 반환
    |
    v
React Native
```

---

## 3. DynamoDB 테이블 구성

| 테이블 | PK | SK | 비고 |
|--------|----|----|------|
| Members | id (S, HASH) | - | deviceToken GSI |
| Rooms | id (S, HASH) | - | |
| RoomParticipants | id (S, HASH) `roomId#memberId` | - | roomId GSI |
| OrderItems | id (S, HASH) | - | roomId GSI |
| Settlements | roomId (S, HASH) | - | |
| MemberSettlements | settlementId (S, HASH) | memberId (S, RANGE) | |
| ChatMessages | roomId (S, HASH) | createdAtId (S, RANGE) | TTL 30일 |

---

## 4. 인증 방식

- 디바이스 최초 실행 시 UUID 생성 → SecureStore 저장
- 모든 API 요청에 `X-Device-Token` 헤더 포함
- 백엔드가 Members 테이블에서 deviceToken으로 조회

---

## 5. 배포 환경

| 구분 | 서비스 | 비고 |
|------|--------|------|
| API 서버 | AWS Lambda (java17) | S3 JAR 배포 (`./scripts/build-lambda.sh`) |
| DB | DynamoDB (ap-northeast-2) | PAY_PER_REQUEST |
| WebSocket | Lightsail (43.201.33.167) | nginx + chat-server |
| AI 추천 | Lambda (nodejs20.x) | Bedrock Claude 3 Haiku |
| 이메일 인증 | AWS SES | feella001@gmail.com |
| 푸시 알림 | FCM | |
| 지도 | Kakao 지도 SDK | WebView, 앱 내 JS 키 |
| 파일 저장 | S3 (food-app-assets-sj) | Lambda JAR 저장용 |

---

## 6. 배포 스크립트

```bash
# 백엔드 Lambda 배포
cd backend && ./scripts/build-lambda.sh

# AI Lambda 배포
cd functions/ai-recommend
npm ci --omit=dev
zip -r function.zip . -x "*.test.js" "*.md"
aws s3 cp function.zip s3://food-app-assets-sj/ai-recommend/function.zip
aws lambda update-function-code \
  --function-name food-recommend-api \
  --s3-bucket food-app-assets-sj \
  --s3-key ai-recommend/function.zip \
  --region ap-northeast-2
```
