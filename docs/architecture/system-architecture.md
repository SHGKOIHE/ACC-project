# System Architecture
# 배달비 절약 음식 공동구매 앱

**작성일**: 2026-05-24
**작성자**: arch
**기술 스택**: Spring Boot 3.3 / Java 17, React Native (Expo), AWS Lambda (Node.js), PostgreSQL, Redis, Docker Compose, CloudFront

---

## 1. 전체 시스템 구성도

```
                          [React Native App]
                          (Expo / iOS+Android)
                                |
                 +--------------+-------+-------+
                 |              |       |       |
                 v              v       v       v
          [CloudFront]   [Cloudflare] [FCM]  [Kakao 지도 SDK]
          (S3 이미지CDN)  (API/WS)  (Google) (클라이언트 직접 호출)
                 |              |       ^       |
                 v              v       |       v
         +------[S3]    [미니PC (온프레미스)]   식당 검색 결과를
         |              |  Docker Compose  |   방 개설 시 서버 전송
         |              |                  |   (restaurantName,
         |   +----------+----------+-------+    restaurantCategory,
         |   |          |          |            latitude, longitude)
         |   v          v          v
         | [Spring   [Post-     [Redis]   [Scheduler]
         |  Boot     greSQL]    :6379     (자동마감)
         |  :8080    :5432]       |          |
         |   |          |         |          |
         |   +----------+---------+----------+
         |              |
         |              v
         |   [API Gateway] -----> [Lambda]
         |   (AWS)                (Node.js)
         |                           |
         |                           v
         |                      [Gemini API]
         |                      (설명 생성)
         |
         +--- [CloudWatch] (미니PC 헬스체크 + 알람)
```

---

## 2. 컴포넌트 간 통신 흐름

### 2-1. 일반 API 요청 (방 생성, 주문 등)

```
React Native
    |
    | HTTPS
    v
Cloudflare Tunnel (SSL 종료, 고정 도메인)
    |
    | HTTP (localhost 포워딩)
    v
Spring Boot :8080
    |
    +---> PostgreSQL (JDBC)     읽기/쓰기
    +---> Redis (Lettuce)       세션 캐시, 디바이스토큰 캐시
    |
    v
ApiResponse<T> JSON 응답
```

### 2-2. 이미지 로딩 (식당 사진, 메뉴 이미지)

```
React Native
    |
    | HTTPS
    v
CloudFront (캐시 HIT 시 즉시 응답)
    |
    | Cache MISS
    v
S3 Bucket (이미지 원본)
```

### 2-3. AI 추천 요청

```
React Native
    |
    | POST /api/ai/recommend
    v
Spring Boot
    |
    | 1) 규칙 엔진: 카테고리/거리/가격 필터링 + 랭킹
    | 2) Top 3~5 결과를 Lambda에 전달
    v
API Gateway (HTTPS)
    |
    v
Lambda (Node.js)
    |
    | Gemini Flash API 호출
    | "이 식당을 추천하는 이유" 설명 생성
    v
설명 텍스트 반환 --> Spring Boot --> React Native
```

---

## 3. Unit 2 — WebSocket/FCM 연동 구조

```
React Native (STOMP Client)
    |
    | ws:// (Cloudflare Tunnel)
    v
Spring Boot [SimpMessagingTemplate]
    |
    +---> /topic/room/{roomId}/chat       채팅 메시지 브로드캐스트
    +---> /topic/room/{roomId}/members    참여자 변경 알림
    +---> /user/queue/notification        개인 알림
    |
    +---> Redis Pub/Sub (단일 서버이지만 확장 대비)
    |
    v
[FCM 발송 흐름]

Spring Boot (NotificationPort 구현체)
    |
    | firebase-admin SDK
    v
FCM Server (Google)
    |
    | 푸시 알림
    v
React Native (백그라운드 수신)

[FCM 발송 트리거]
+------------------------------------------+
| 이벤트               | 수신 대상         |
|----------------------|-------------------|
| 새 참여자 입장        | 방 전체 참여자     |
| 참여자 탈퇴           | 방 전체 참여자     |
| 방 마감 (수동/자동)    | 방 전체 참여자     |
| 주문 확정             | 방 전체 참여자     |
| 방 취소               | 방 전체 참여자     |
| 채팅 메시지 (백그라운드)| 해당 방 참여자     |
+------------------------------------------+
```

---

## 4. Unit 3 — Lambda AI 추천 연동 구조

```
[Spring Boot]                          [AWS]
     |                                   |
     | 1. 규칙 엔진 필터링                 |
     |    - 카테고리 매칭                  |
     |    - 거리 필터 (Haversine)          |
     |    - 가격대 필터                    |
     |    - 모임유형 필터                  |
     |    - 참여율/마감시간 랭킹            |
     |                                   |
     | 2. Top 3~5 식당 + 사용자 조건       |
     |    POST /recommend                |
     +---------------------------------->|
     |                              [API Gateway]
     |                                   |
     |                              [Lambda]
     |                                   |
     |                  3. Gemini Flash 호출
     |                     프롬프트:
     |                     - 식당 정보 (이름,메뉴,가격)
     |                     - 사용자 조건 (카테고리,거리,가격대)
     |                     - "왜 이 식당을 추천하는지 2줄 설명"
     |                                   |
     |                  4. 설명 텍스트 생성
     |<----------------------------------+
     |
     | 5. 식당 정보 + 설명 텍스트 조합하여 응답
     v
[React Native]
  - 식당명, 거리, 예상 배달비
  - AI 설명: "이 식당은 현재 3명이 참여 중이라
    배달비가 1,200원으로 줄어요.
    인기 메뉴 김치찌개가 7,000원으로 가성비 좋아요."
```

### Lambda 요청/응답 형식

```json
// Request (Spring Boot -> Lambda)
{
  "restaurants": [
    {
      "name": "한솥도시락",
      "category": "한식",
      "distance_m": 350,
      "avg_price": 7000,
      "current_participants": 3,
      "delivery_fee_per_person": 1200,
      "popular_menus": ["김치찌개", "제육볶음"]
    }
  ],
  "user_preferences": {
    "categories": ["한식"],
    "max_price": 10000,
    "mood_keywords": []
  }
}

// Response (Lambda -> Spring Boot)
{
  "recommendations": [
    {
      "restaurant_name": "한솥도시락",
      "explanation": "현재 3명이 참여 중이라 배달비가 1,200원으로 줄어요. 인기 메뉴 김치찌개가 7,000원으로 가성비가 좋습니다.",
      "score": 0.92
    }
  ]
}
```

---

## 5. 배포 환경 요약

| 구분 | 위치 | 구성 |
|------|------|------|
| 앱 서버 | 미니PC | Spring Boot 3.3 (Docker) |
| DB | 미니PC | PostgreSQL 16 (Docker) |
| 캐시 | 미니PC | Redis 7 (Docker) |
| 터널링 | 미니PC | Cloudflare Tunnel (Docker) |
| 이미지 CDN | AWS | CloudFront + S3 |
| AI 추천 | AWS | API Gateway + Lambda |
| 모니터링 | AWS | CloudWatch (헬스체크) |
| 푸시 알림 | Google | FCM |
| 지도 (식당 검색) | 클라이언트 | Kakao 지도 SDK (React Native 직접 호출, API 키는 앱에만 존재) |
| AI 설명 | Google | Gemini Flash API |

---

## 6. Docker Compose 서비스 구성

```yaml
services:
  app:         # Spring Boot 3.3
  postgres:    # PostgreSQL 16
  redis:       # Redis 7
  tunnel:      # Cloudflare Tunnel
```

| 서비스 | 포트 | 리소스 제한 (권장) |
|--------|------|-------------------|
| app | 8080 | 4GB RAM, 4 CPU |
| postgres | 5432 | 4GB RAM, 2 CPU |
| redis | 6379 | 1GB RAM, 1 CPU |
| tunnel | - | 256MB RAM |
| OS + 여유 | - | ~15GB 잔여 |
| **합계** | | ~9GB / 24GB |
