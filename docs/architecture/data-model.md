# Data Model
# 배달비 절약 음식 공동구매 앱

**최종 수정**: 2026-06-01
**DB**: DynamoDB (ap-northeast-2, PAY_PER_REQUEST)

---

## 테이블 목록

| 테이블 | PK | SK | GSI |
|--------|----|----|-----|
| Members | id (S) | - | deviceToken-index |
| Rooms | id (S) | - | - |
| RoomParticipants | id (S) `roomId#memberId` | - | roomId-index |
| OrderItems | id (S) | - | roomId-index |
| Settlements | roomId (S) | - | - |
| MemberSettlements | settlementId (S) | memberId (S) | - |
| ChatMessages | roomId (S) | createdAtId (S) | TTL: expiresAt |

---

## Members

| 속성 | 타입 | 설명 |
|------|------|------|
| id | S (HASH) | UUID |
| nickname | S | 2~12자, 서비스 전체 고유 |
| deviceToken | S | 앱 최초 실행 UUID |
| gender | S | MALE / FEMALE / OTHER (nullable) |
| address | S | 주소 (nullable) |
| email | S | 이메일 (nullable, SES 인증용) |
| fcmToken | S | FCM 푸시 토큰 (nullable) |
| createdAt | S | ISO-8601 |

**GSI**: `deviceToken-index` — deviceToken HASH (인증 조회)

---

## Rooms

| 속성 | 타입 | 설명 |
|------|------|------|
| id | S (HASH) | UUID |
| hostId | S | Members.id |
| title | S | 방 제목 |
| meetingType | S | DELIVERY / DELIVERY_TOGETHER / DINE_OUT |
| restaurantName | S | |
| restaurantAddress | S | |
| restaurantCategory | S | |
| latitude | N | |
| longitude | N | |
| deliveryFee | N | 총 배달비 (원) |
| maxParticipants | N | |
| currentParticipantCount | N | |
| status | S | OPEN / CLOSED / CONFIRMED / DELIVERING / COMPLETED / CANCELLED |
| closedAt | S | 자동 마감 시각 (nullable) |
| meetingAddress | S | DELIVERY_TOGETHER/DINE_OUT 전용 (nullable) |
| accountHolder | S | 예금주 (nullable) |
| bankName | S | 은행명 (nullable) |
| encryptedAccountNumber | S | AES-256-GCM 암호화 (nullable) |
| createdAt | S | ISO-8601 |

---

## RoomParticipants

PK = `id` = `{roomId}#{memberId}` (복합 문자열)

| 속성 | 타입 | 설명 |
|------|------|------|
| id | S (HASH) | `roomId#memberId` |
| roomId | S | Rooms.id |
| memberId | S | Members.id |
| joinedAt | S | ISO-8601 |

**GSI**: `roomId-index` — roomId HASH (방 참여자 목록 조회)

---

## OrderItems

| 속성 | 타입 | 설명 |
|------|------|------|
| id | S (HASH) | UUID |
| roomId | S | Rooms.id |
| memberId | S | Members.id |
| menuName | S | |
| price | N | 단가 (원) |
| createdAt | S | ISO-8601 |

**GSI**: `roomId-index` — roomId HASH (방 주문 목록 조회)

---

## Settlements

| 속성 | 타입 | 설명 |
|------|------|------|
| roomId | S (HASH) | Rooms.id (방당 1건) |
| id | S | UUID |
| totalMenuAmount | N | 전체 메뉴 합계 |
| totalDeliveryFee | N | 총 배달비 |
| participantCount | N | 정산 기준 인원 |
| deliveryFeePerPerson | N | 1인당 배달비 (올림) |
| hostSurplus | N | 올림 차액 (방장 수령) |
| createdAt | S | ISO-8601 |

---

## MemberSettlements

| 속성 | 타입 | 설명 |
|------|------|------|
| settlementId | S (HASH) | Settlements.id |
| memberId | S (RANGE) | Members.id |
| menuAmount | N | 본인 메뉴 합계 |
| deliveryFeeShare | N | 본인 배달비 부담 (올림) |
| totalAmount | N | menuAmount + deliveryFeeShare |
| isHost | BOOL | 방장 여부 |

---

## ChatMessages

| 속성 | 타입 | 설명 |
|------|------|------|
| roomId | S (HASH) | Rooms.id |
| createdAtId | S (RANGE) | `{ISO-8601}#{UUID}` (정렬용) |
| senderId | S | Members.id |
| senderNickname | S | |
| content | S | |
| type | S | CHAT / SYSTEM |
| expiresAt | N | Unix timestamp (TTL, 30일) |

**TTL**: `expiresAt` 속성으로 30일 후 자동 삭제

---

## 상태 전이 (Rooms.status)

```
           +---------+
           |  OPEN   |
           +---------+
            /       \
   (방장수동/     (방장취소)
    시간초과)          \
          /        +-----------+
   +--------+      | CANCELLED |
   | CLOSED |      +-----------+
   +--------+
        |
   (방장확정)
        |
   +-----------+
   | CONFIRMED |
   +-----------+
        |
   (배달시작)
        |
   +------------+
   | DELIVERING |
   +------------+
        |
   (완료처리)
        |
   +-----------+
   | COMPLETED |
   +-----------+
```

---

## 암호화

| 테이블 | 속성 | 알고리즘 | 비고 |
|--------|------|----------|------|
| Rooms | encryptedAccountNumber | AES-256-GCM | `ENCRYPTION_KEY` 환경변수로 키 주입 |
