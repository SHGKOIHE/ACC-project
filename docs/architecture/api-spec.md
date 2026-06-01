# API Specification
# 배달비 절약 음식 공동구매 앱

**최종 수정**: 2026-06-01
**Base URL**: `https://40ocxlcwfl.execute-api.ap-northeast-2.amazonaws.com/api`

---

## 공통

### 인증 헤더

```
X-Device-Token: {deviceToken}
```

### 성공 응답

```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

### 에러 응답

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ROOM_NOT_FOUND",
    "message": "방을 찾을 수 없습니다.",
    "timestamp": "2026-06-01T10:00:00Z",
    "path": "/api/rooms/xxx"
  }
}
```

---

## Auth

### POST /api/auth/register

**Request**
```json
{ "nickname": "맛집헌터", "deviceToken": "uuid" }
```

**Response** `201`
```json
{ "id": "uuid", "nickname": "맛집헌터", "createdAt": "..." }
```

| 상황 | 코드 | HTTP |
|------|------|------|
| 닉네임 중복 | NICKNAME_DUPLICATED | 409 |
| 형식 오류 | INVALID_NICKNAME | 400 |

---

### GET /api/auth/me

**Response** `200`
```json
{ "id": "uuid", "nickname": "맛집헌터", "gender": null, "address": null, "email": null }
```

---

### PUT /api/auth/me

**Request**
```json
{ "nickname": "새닉네임", "gender": "MALE", "address": "서울시 ...", "email": "..." }
```

---

### DELETE /api/auth/me

회원 탈퇴 (방장인 경우 불가)

---

### PUT /api/auth/fcm-token

**Request** `{ "fcmToken": "..." }`

---

### POST /api/auth/email/send-code

**Request** `{ "email": "user@example.com" }`

---

### POST /api/auth/email/verify

**Request** `{ "email": "user@example.com", "code": "123456" }`

---

## Room

### GET /api/rooms

**Response** `200`
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "hostId": "uuid",
      "title": "치킨 같이 시켜요",
      "meetingType": "DELIVERY",
      "restaurantName": "BBQ치킨",
      "deliveryFee": 3000,
      "maxParticipants": 4,
      "currentParticipantCount": 2,
      "status": "OPEN",
      "latitude": 37.123,
      "longitude": 127.456,
      "closedAt": "...",
      "createdAt": "..."
    }
  ]
}
```

---

### POST /api/rooms

**Request**
```json
{
  "title": "치킨 같이 시켜요",
  "meetingType": "DELIVERY",
  "restaurantName": "BBQ치킨",
  "restaurantAddress": "경기도 수원시 ...",
  "restaurantCategory": "치킨",
  "latitude": 37.123,
  "longitude": 127.456,
  "deliveryFee": 3000,
  "maxParticipants": 4,
  "closedAt": "2026-06-01T12:30:00",
  "accountHolder": "홍길동",
  "bankName": "신한은행",
  "accountNumber": "110-123-456789",
  "meetingAddress": null
}
```

**Response** `201`
```json
{ "id": "uuid", "title": "...", "status": "OPEN", "createdAt": "..." }
```

---

### GET /api/rooms/{id}

**Response** `200`
```json
{
  "id": "uuid",
  "hostId": "uuid",
  "title": "치킨 같이 시켜요",
  "meetingType": "DELIVERY",
  "restaurantName": "BBQ치킨",
  "restaurantAddress": "...",
  "restaurantCategory": "치킨",
  "latitude": 37.123,
  "longitude": 127.456,
  "deliveryFee": 3000,
  "maxParticipants": 4,
  "currentParticipantCount": 2,
  "status": "OPEN",
  "closedAt": "...",
  "meetingAddress": null,
  "accountHolder": "홍길동",
  "bankName": "신한은행",
  "createdAt": "...",
  "isParticipant": true,
  "isHost": false
}
```

> `isParticipant`, `isHost` — 호출자 기준 서버 측 계산값. 프론트에서 방장/참여자 UI 분기에 사용.

---

### POST /api/rooms/{id}/join

**Response** `200`

| 상황 | 코드 | HTTP |
|------|------|------|
| OPEN 아님 | ROOM_NOT_OPEN | 409 |
| 인원 초과 | ROOM_FULL | 409 |
| 이미 참여 | ALREADY_JOINED | 409 |

---

### POST /api/rooms/{id}/leave

참여자 퇴장 (OPEN 상태, 방장 불가 — 방장은 cancel 사용)

| 상황 | 코드 | HTTP |
|------|------|------|
| 방장 탈퇴 시도 | HOST_CANNOT_LEAVE | 403 |
| 마감 후 탈퇴 | ROOM_ALREADY_CLOSED | 409 |

---

### POST /api/rooms/{id}/close

방 마감 (방장 전용)

---

### POST /api/rooms/{id}/cancel

방 취소 (방장 전용, OPEN/CLOSED/CONFIRMED 상태)

---

### POST /api/rooms/{id}/deliver

배달 시작 처리 (방장 전용, CONFIRMED 상태)

---

### POST /api/rooms/{id}/complete

완료 처리 (방장 전용, DELIVERING 상태)

---

## 참여자

### GET /api/rooms/{id}/participants

**Response** `200`
```json
{
  "data": [
    { "memberId": "uuid", "nickname": "맛집헌터", "joinedAt": "..." }
  ]
}
```

---

## Order

### GET /api/rooms/{roomId}/orders

**Response** `200`
```json
{
  "data": [
    { "id": "uuid", "memberId": "uuid", "menuName": "양념치킨", "price": 18000 }
  ]
}
```

---

### POST /api/rooms/{roomId}/orders

**Request** `{ "menuName": "양념치킨", "price": 18000 }`

**Response** `201`

---

### DELETE /api/rooms/{roomId}/orders/{orderId}

**Response** `204`

---

### POST /api/rooms/{roomId}/orders/confirm

주문 확정 (방장 전용, CLOSED 상태)

**Response** `200`
```json
{
  "roomId": "uuid",
  "status": "CONFIRMED",
  "settlement": {
    "totalMenuAmount": 34000,
    "totalDeliveryFee": 3000,
    "participantCount": 3,
    "deliveryFeePerPerson": 1000,
    "hostSurplus": 0,
    "members": [
      { "nickname": "맛집헌터", "menuAmount": 18000, "deliveryFeeShare": 1000, "totalAmount": 19000, "isHost": true }
    ]
  }
}
```

---

### GET /api/rooms/{roomId}/orders/settlement

정산 결과 조회 (CONFIRMED/COMPLETED 상태)

---

## 채팅

### GET /api/rooms/{roomId}/chats

**Response** `200` — 최근 채팅 내역 (DynamoDB, TTL 30일)

### WebSocket

- 연결: `ws://43.201.33.167/ws` (STOMP)
- 구독: `/topic/room/{roomId}/chat`
- 발행: `/app/room/{roomId}/chat`

---

## AI 추천

### POST /api/ai/recommend

**Request**
```json
{
  "roomId": "uuid",
  "participants": [
    {
      "nickname": "짱구",
      "orderItems": [{ "name": "치킨", "price": 15000 }]
    }
  ],
  "filters": {
    "category": "치킨",
    "maxDeliveryFee": 3000,
    "userMessage": "매운 거 좋아해",
    "latitude": 37.123,
    "longitude": 127.456
  }
}
```

**Response** `200`
```json
{
  "recommendations": [
    { "rank": 1, "restaurantName": "BBQ치킨", "score": 80, "reason": "..." }
  ],
  "explanation": "치킨 계열 식당이 가장 적합합니다."
}
```

---

## 에러 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| MEMBER_NOT_FOUND | 404 | 회원 없음 |
| NICKNAME_DUPLICATED | 409 | 닉네임 중복 |
| INVALID_NICKNAME | 400 | 닉네임 형식 오류 |
| ROOM_NOT_FOUND | 404 | 방 없음 |
| ROOM_NOT_OPEN | 409 | OPEN 상태 아님 |
| ROOM_FULL | 409 | 인원 초과 |
| ALREADY_JOINED | 409 | 이미 참여 중 |
| HOST_CANNOT_LEAVE | 403 | 방장 탈퇴 불가 |
| ROOM_ALREADY_CLOSED | 409 | 이미 마감됨 |
| NOT_HOST | 403 | 방장 권한 필요 |
| INVALID_STATE_TRANSITION | 409 | 상태 전이 불가 |
| ORDER_NOT_FOUND | 404 | 주문 없음 |
| NOT_ORDER_OWNER | 403 | 주문 소유자 아님 |
| UNAUTHORIZED | 401 | 인증 실패 |
