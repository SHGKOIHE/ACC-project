# Unit of Work — Story Map
# 배달비 절약을 위한 음식 공동 구매 앱

**작성일**: 2026-05-24
**단계**: INCEPTION — Units Generation

---

## 에픽 → 유닛 매핑

| 에픽 ID | 에픽명 | 유닛 | 스트림 | MVP |
|---------|--------|------|--------|-----|
| EP-01 | 회원 인증 | Unit 1 — Core Backend | Stream 1 | ✅ |
| EP-02 | 방 개설 (공동 배달) | Unit 1 — Core Backend | Stream 1 | ✅ |
| EP-03 | 방 개설 (모임/외식) | Unit 1 — Core Backend | Stream 1 | ✅ |
| EP-04 | 방 탐색 | Unit 1 + Unit 3 | Stream 1 + 3 | ✅ |
| EP-05 | 방 참여 및 메뉴 선택 | Unit 1 — Core Backend | Stream 1 | ✅ |
| EP-06 | AI 음식 추천 | Unit 3 — AI + Infra | Stream 3 | ✅ |
| EP-07 | 실시간 채팅 | Unit 2 — Realtime + Mobile | Stream 2 | ✅ |
| EP-08 | 주문 확정 및 배달비 정산 | Unit 1 — Core Backend | Stream 1 | ✅ |
| EP-09 | 식당 정보 연동 | Unit 3 — AI + Infra | Stream 3 | ✅ |
| EP-10 | 관리자 운영 | — | — | ❌ (post-MVP) |

---

## 유닛별 에픽 상세

### Unit 1 — Core Backend (EP-01, 02, 03, 05, 08)

| 에픽 | 백엔드 모듈 | React Native 피처 (Unit 2 담당) |
|------|-----------|--------------------------------|
| EP-01 회원 인증 | BC-01 auth | features/auth |
| EP-02 방 개설 (공동 배달) | BC-02 room | features/room |
| EP-03 방 개설 (모임/외식) | BC-02 room | features/room |
| EP-05 방 참여 및 메뉴 선택 | BC-02 room, BC-03 order | features/room, features/order |
| EP-08 주문 확정 및 배달비 정산 | BC-03 order | features/order |

**Unit 1 담당 API 엔드포인트**

```
POST   /api/auth/signup
POST   /api/auth/login
POST   /api/auth/logout
POST   /api/auth/refresh

GET    /api/rooms              # 방 목록 (지도용 좌표 포함)
POST   /api/rooms              # 방 생성
GET    /api/rooms/{id}         # 방 상세
POST   /api/rooms/{id}/join    # 방 참여
DELETE /api/rooms/{id}/leave   # 방 탈퇴
PATCH  /api/rooms/{id}/close   # 방 마감 (방장)

GET    /api/rooms/{id}/orders         # 주문 목록
POST   /api/rooms/{id}/orders         # 주문 항목 추가
DELETE /api/rooms/{id}/orders/{oid}   # 주문 항목 삭제
POST   /api/rooms/{id}/confirm        # 주문 확정 (방장)
GET    /api/rooms/{id}/settlement     # 정산 결과
```

---

### Unit 2 — Realtime + Mobile (EP-07)

| 에픽 | 백엔드 모듈 | React Native 피처 |
|------|-----------|------------------|
| EP-07 실시간 채팅 | BC-04 chat, BC-05 notification | features/chat |
| (앱 전체 UI) | — | features/auth, room, order, ai, map |

**Unit 2 담당 WebSocket 엔드포인트**

```
WS     /ws                            # STOMP 연결
SUB    /topic/room/{roomId}/chat      # 채팅 구독
PUB    /app/room/{roomId}/chat        # 메시지 전송
SUB    /topic/room/{roomId}/members   # 참여자 변경 알림
SUB    /user/queue/notification       # 개인 푸시 알림
```

**React Native 앱 화면 목록**

```
features/auth/
  LoginScreen, SignupScreen

features/room/
  RoomListScreen (지도), RoomCreateScreen, RoomDetailScreen

features/chat/
  ChatScreen

features/order/
  OrderInputScreen, SettlementScreen

features/ai/
  AiRecommendScreen

features/map/
  MapExploreScreen (카카오맵 SDK)
```

---

### Unit 3 — AI + Infrastructure (EP-04, 06, 09)

| 에픽 | 컴포넌트 | 담당 |
|------|---------|------|
| EP-04 방 탐색 (지도 서버사이드) | BC-06 map (Kakao REST API) | Stream 3 |
| EP-06 AI 음식 추천 | AC-01 Lambda (RuleEngine + Gemini) | Stream 3 |
| EP-09 식당 정보 연동 | BC-06 map, AC-01 Lambda | Stream 3 |

**Unit 3 담당 API 엔드포인트**

```
GET    /api/map/restaurants           # 주변 식당 검색 (Kakao REST)
GET    /api/map/geocode               # 주소 → 좌표 변환

POST   /api/ai/recommend              # AI 추천 요청 (Lambda 호출)
```

**Lambda 엔드포인트 (API Gateway)**

```
POST   /recommend                     # RuleEngine + Gemini 설명 생성
```

---

## MVP 개발 우선순위

```
Priority 1 (Day 1~2) — Unit 1 착수
  EP-01 회원 인증 (auth API + DB 스키마)
  EP-02/03 방 개설 (room API + DB 스키마)

Priority 2 (Day 2~3) — 인터페이스 확정 게이트
  Unit 1 → OpenAPI 초안 발행
  Unit 3 → Lambda 인터페이스 확정
  Unit 2 → STOMP 스키마 확정

Priority 3 (Day 3~5) — 병렬 진행
  Unit 1: EP-05 방 참여, EP-08 주문/정산, ai-client 구현
  Unit 2: React Native 앱 전체 (OpenAPI 참조)
  Unit 3: Lambda RuleEngine + Gemini, CI/CD

Priority 4 (Day 5~7) — 통합 및 검증
  EP-07 채팅 연동
  EP-06 AI 추천 연동
  EP-04/09 지도/식당 연동
  통합 테스트

Post-MVP
  EP-10 관리자 운영 (전체 제외)
```

---

## 공통 모듈 (common/) 책임

`common/` 패키지는 Unit 1(Stream 1)이 작성·소유하고 Unit 2, 3이 참조합니다.

| 패키지 | 내용 |
|--------|------|
| `common/dto` | `ApiResponse<T>`, `PageDto<T>`, `ErrorResponse` |
| `common/exception` | `BusinessException`, `GlobalExceptionHandler` |
| `common/security` | `JwtTokenProvider`, `SecurityConfig` |
| `common/util` | `DateUtils`, `StringUtils` |
| `common/constant` | `RoomStatus`, `OrderStatus`, `ErrorCode` |
