# API Specification
# 배달비 절약 음식 공동구매 앱

**작성일**: 2026-05-24
**작성자**: arch
**Base URL**: `https://{tunnel-domain}/api`

---

## 공통 응답 형식

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
    "timestamp": "2026-05-24T14:30:00Z",
    "path": "/api/rooms/999"
  }
}
```

### 인증 헤더

```
X-Device-Token: {deviceToken}
```

> MVP는 디바이스 토큰 기반 인증. Post-MVP에서 JWT로 전환 예정.

---

## Auth 모듈

### POST /api/auth/register

회원 등록 (닉네임 + 디바이스토큰)

**Request**
```json
{
  "nickname": "맛집헌터",
  "deviceToken": "uuid-from-app"
}
```

**Response** `201 Created`
```json
{
  "id": 1,
  "nickname": "맛집헌터",
  "createdAt": "2026-05-24T10:00:00"
}
```

**에러**
| 상황 | 코드 | HTTP |
|------|------|------|
| 닉네임 중복 | NICKNAME_DUPLICATED | 409 |
| 닉네임 길이 위반 (2~12자) | INVALID_NICKNAME | 400 |

---

### GET /api/auth/me

내 정보 조회

**Response** `200 OK`
```json
{
  "id": 1,
  "nickname": "맛집헌터",
  "createdAt": "2026-05-24T10:00:00"
}
```

---

## Room 모듈

### GET /api/rooms

방 목록 조회 (필터)

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| lat | Double | Y | 현재 위도 |
| lng | Double | Y | 현재 경도 |
| radius | Integer | N | 반경 (m), 기본 3000 |
| category | String | N | 식당 카테고리 |
| meetingType | String | N | DELIVERY / DELIVERY_TOGETHER / DINE_OUT |
| status | String | N | OPEN (기본값) |
| page | Integer | N | 페이지 번호, 기본 0 |
| size | Integer | N | 페이지 크기, 기본 20 |

**Response** `200 OK`
```json
{
  "content": [
    {
      "id": 1,
      "title": "학교 앞 치킨 같이 시켜요",
      "restaurantName": "BBQ치킨",
      "meetingType": "DELIVERY",
      "deliveryFee": 3000,
      "currentParticipants": 2,
      "maxParticipants": 4,
      "deliveryFeePerPerson": 750,
      "latitude": 37.123,
      "longitude": 127.456,
      "status": "OPEN",
      "closedAt": "2026-05-24T12:30:00",
      "createdAt": "2026-05-24T11:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 5
}
```

---

### POST /api/rooms

방 생성

**Request**
```json
{
  "title": "학교 앞 치킨 같이 시켜요",
  "meetingType": "DELIVERY",
  "restaurantName": "BBQ치킨",
  "restaurantAddress": "경기도 수원시 ...",
  "restaurantCategory": "치킨",
  "latitude": 37.123,
  "longitude": 127.456,
  "deliveryFee": 3000,
  "maxParticipants": 4,
  "closedAt": "2026-05-24T12:30:00",
  "accountNumber": "110-123-456789",
  "accountHolder": "홍길동",
  "bankName": "신한은행",
  "meetingAddress": null
}
```

**Response** `201 Created`
```json
{
  "id": 1,
  "title": "학교 앞 치킨 같이 시켜요",
  "status": "OPEN",
  "hostNickname": "맛집헌터",
  "createdAt": "2026-05-24T11:00:00"
}
```

---

### GET /api/rooms/{id}

방 상세 조회

**Response** `200 OK`
```json
{
  "id": 1,
  "title": "학교 앞 치킨 같이 시켜요",
  "meetingType": "DELIVERY",
  "restaurantName": "BBQ치킨",
  "restaurantAddress": "경기도 수원시 ...",
  "restaurantCategory": "치킨",
  "latitude": 37.123,
  "longitude": 127.456,
  "deliveryFee": 3000,
  "maxParticipants": 4,
  "status": "OPEN",
  "closedAt": "2026-05-24T12:30:00",
  "host": {
    "id": 1,
    "nickname": "맛집헌터"
  },
  "participants": [
    { "id": 1, "nickname": "맛집헌터", "joinedAt": "2026-05-24T11:00:00" },
    { "id": 2, "nickname": "배달왕", "joinedAt": "2026-05-24T11:05:00" }
  ],
  "accountInfo": {
    "bankName": "신한은행",
    "accountHolder": "홍길동",
    "maskedNumber": "***-***-**6789"
  },
  "createdAt": "2026-05-24T11:00:00"
}
```

---

### POST /api/rooms/{id}/join

방 참여

**Response** `200 OK`
```json
{
  "roomId": 1,
  "currentParticipants": 3,
  "maxParticipants": 4
}
```

**에러**
| 상황 | 코드 | HTTP |
|------|------|------|
| 방이 OPEN 아님 | ROOM_NOT_OPEN | 409 |
| 인원 초과 | ROOM_FULL | 409 |
| 이미 참여 중 | ALREADY_JOINED | 409 |

---

### DELETE /api/rooms/{id}/leave

방 탈퇴 (OPEN 상태에서만)

**Response** `200 OK`

**에러**
| 상황 | 코드 | HTTP |
|------|------|------|
| 방장은 탈퇴 불가 | HOST_CANNOT_LEAVE | 403 |
| 마감 후 탈퇴 불가 | ROOM_ALREADY_CLOSED | 409 |

---

### PATCH /api/rooms/{id}/close

방 마감 (방장 전용)

**Response** `200 OK`

---

### PATCH /api/rooms/{id}/cancel

방 취소 (방장 전용, OPEN/CLOSED/CONFIRMED 상태)

**Response** `200 OK`

---

## Order 모듈

### GET /api/rooms/{id}/orders

방 주문 목록 조회

**Response** `200 OK`
```json
{
  "orders": [
    {
      "id": 1,
      "memberNickname": "맛집헌터",
      "menuName": "양념치킨",
      "quantity": 1,
      "price": 18000
    },
    {
      "id": 2,
      "memberNickname": "배달왕",
      "menuName": "후라이드치킨",
      "quantity": 1,
      "price": 16000
    }
  ],
  "totalMenuAmount": 34000
}
```

---

### POST /api/rooms/{id}/orders

주문 항목 추가 (OPEN 상태에서만)

**Request**
```json
{
  "menuName": "양념치킨",
  "quantity": 1,
  "price": 18000
}
```

**Response** `201 Created`

---

### DELETE /api/rooms/{id}/orders/{orderId}

주문 항목 삭제 (본인 주문, OPEN 상태에서만)

**Response** `204 No Content`

---

### POST /api/rooms/{id}/confirm

주문 확정 (방장 전용, CLOSED 상태에서만)

**Response** `200 OK`
```json
{
  "roomId": 1,
  "status": "CONFIRMED",
  "settlement": {
    "totalMenuAmount": 34000,
    "totalDeliveryFee": 3000,
    "participantCount": 3,
    "deliveryFeePerPerson": 1000,
    "hostSurplus": 0,
    "members": [
      {
        "nickname": "맛집헌터",
        "menuAmount": 18000,
        "deliveryFeeShare": 1000,
        "totalAmount": 19000,
        "isHost": true
      },
      {
        "nickname": "배달왕",
        "menuAmount": 16000,
        "deliveryFeeShare": 1000,
        "totalAmount": 17000,
        "isHost": false
      }
    ]
  }
}
```

---

### GET /api/rooms/{id}/settlement

정산 결과 조회 (CONFIRMED/COMPLETED 상태)

**Response**: confirm 응답의 settlement 동일

---

## Map 모듈 (클라이언트 처리)

> **식당 검색은 서버 API 없음.** React Native에서 카카오 지도 SDK를 직접 호출하여 식당을 검색하고, 사용자가 선택한 식당 정보를 방 개설 요청(POST /api/rooms)에 포함하여 서버로 전송. 서버는 수신한 식당 정보를 저장만 함.
>
> - 카카오 API 키는 모바일 앱에만 존재 (서버에 없음)
> - Post-MVP에서 서버 프록시(GET /api/map/restaurants)로 전환 가능 — 서버 인터페이스 변경 없음 (restaurantName, restaurantCategory 등 필드 동일)

---

## AI 모듈 (Unit 3)

### POST /api/ai/recommend

AI 식당 추천

**Request**
```json
{
  "latitude": 37.123,
  "longitude": 127.456,
  "categories": ["한식", "분식"],
  "maxPrice": 10000,
  "moodKeywords": ["따뜻한", "국물"]
}
```

**Response** `200 OK`
```json
{
  "recommendations": [
    {
      "restaurantName": "엄마손칼국수",
      "category": "한식",
      "distance": 350,
      "avgPrice": 8000,
      "explanation": "현재 2명이 참여 중인 방이 있어 배달비가 1,500원으로 줄어요. 칼국수와 수제비가 인기 메뉴입니다.",
      "activeRoomId": 5
    }
  ]
}
```

---

## 에러 코드 목록

| 코드 | HTTP | 설명 |
|------|------|------|
| MEMBER_NOT_FOUND | 404 | 회원 없음 |
| NICKNAME_DUPLICATED | 409 | 닉네임 중복 |
| INVALID_NICKNAME | 400 | 닉네임 형식 오류 |
| ROOM_NOT_FOUND | 404 | 방 없음 |
| ROOM_NOT_OPEN | 409 | 방이 열린 상태 아님 |
| ROOM_FULL | 409 | 인원 초과 |
| ALREADY_JOINED | 409 | 이미 참여 중 |
| HOST_CANNOT_LEAVE | 403 | 방장 탈퇴 불가 |
| ROOM_ALREADY_CLOSED | 409 | 이미 마감됨 |
| NOT_HOST | 403 | 방장 권한 필요 |
| INVALID_STATE_TRANSITION | 409 | 상태 전이 불가 |
| ORDER_NOT_FOUND | 404 | 주문 없음 |
| NOT_ORDER_OWNER | 403 | 주문 소유자 아님 |
| UNAUTHORIZED | 401 | 인증 실패 |
