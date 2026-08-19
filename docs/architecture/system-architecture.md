# System Architecture
# 배달비 절약 음식 공동구매 앱

**최종 수정**: 2026-08-14
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
  northeast-2.aws    (WebSocket wss://)
              |                |
              v                v
        [Lambda]         [chat-server]
    foodgroup-backend   (Spring Boot WebSocket)
    (java17, 1GB,                |
     SnapStart ON)          [Redis]
              |           Pub/Sub 브로커
              |                 |
              v                 v
        [Redis]          [DynamoDB]
    deviceToken 인증     ChatMessages
    (device-token:*)     (roomId HASH,
              |          createdAtId RANGE,
              v          TTL 30일)
        [DynamoDB]
   Members / Rooms /
   RoomParticipants /
   OrderItems /
   Settlements /
   MemberSettlements
              |
              v
        [Lambda]
    food-recommend-api
      (nodejs20.x)
         |        |
         v        v
[Kakao 로컬 API]  [Bedrock Claude 3 Haiku]
(실제 식당 후보,   (anthropic.claude-3-haiku,
 REST API 키)      ap-northeast-2)
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
    | wss://43.201.33.167/ws-native
    | X-Device-Token 헤더
    v
Lightsail chat-server (Spring Boot WebSocket)
    |
    | STOMP CONNECT: 선인증 세션 → X-Device-Token(Redis) → JWT(JWT_SECRET) 순으로 인증
    | STOMP SUBSCRIBE /topic/room/{roomId}: RoomParticipantChecker로 참가자 검증
    |
    +---> /topic/room/{roomId}/chat       채팅 메시지 브로드캐스트
    +---> Redis Pub/Sub (멀티 인스턴스 확장 대비)
    |
    v
DynamoDB ChatMessages (쓰기, TTL 30일) / RoomParticipants (참가자 확인, 읽기 전용)
```

### 2-3. AI 추천 요청

```
React Native
    |
    | POST /api/recommend          (단독 추천)
    | POST /api/rooms/{id}/recommend (방 기반 추천)
    | X-Device-Token 헤더
    v
API Gateway → Lambda (foodgroup-backend)
    |
    | HTTP invoke (X-Internal-Key 헤더 포함)
    v
Lambda (food-recommend-api, nodejs20.x)
    |
    | 카카오맵 로컬 API로 국제캠퍼스 인근 실제 식당 후보 조회 (카테고리별, KAKAO_REST_API_KEY 필요)
    v
Bedrock Claude 3 Haiku가 후보 목록을 근거로 추천 결과·설명 생성
    | Bedrock 실패 시 규칙 엔진 fallback (fallback: true, 카카오 후보는 동일하게 반영)
    | 카카오 미설정/실패 시 카테고리명 기반 결과로 자동 축소
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
- **REST API (Lambda)**: Redis에서 `device-token:{token}` → memberId 조회, 조회 시 TTL 슬라이딩 갱신 (활성 사용자는 만료되지 않음)
- **WebSocket (chat-server)**: Handshake 단계는 커스텀 헤더를 보낼 수 없는 클라이언트(RN WebSocket 등)를 고려해 더 이상 인증을 강제하지 않음. 실제 인증은 STOMP `CONNECT` 프레임에서 수행:
  1. Handshake에서 이미 선인증된 세션이면 그대로 사용
  2. `X-Device-Token` 네이티브 헤더가 있으면 Redis 조회로 memberId 확인
  3. 둘 다 없으면 `Authorization`/`access_token`/`token` 헤더의 JWT를 `JWT_SECRET`(HS256)으로 서명 검증
  - `SUBSCRIBE /topic/room/{roomId}` 시 `RoomParticipantChecker`로 DynamoDB `RoomParticipants` 조회, 참가자가 아니면 거부
- Redis 미등록 토큰 / 서명 불일치 / 미참가자 → 401 또는 STOMP 에러로 거부

---

## 5. 배포 환경

| 구분 | 서비스 | 비고 |
|------|--------|------|
| API 서버 | AWS Lambda (java17) | S3 JAR 배포, **SnapStart ON** (Version 3~) |
| CI/CD | GitHub Actions | push → build → S3 → Lambda update-function-code |
| DB | DynamoDB (ap-northeast-2) | PAY_PER_REQUEST |
| 인증 캐시 | Redis (Docker, Lightsail) | deviceToken → memberId, TTL 24h |
| WebSocket | Lightsail (43.201.33.167) | chat-server:8081, wss:// |
| AI 추천 | Lambda (nodejs20.x) | Bedrock Claude 3 Haiku, 카카오맵 로컬 API(실제 식당 후보), 규칙 엔진 fallback |
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
