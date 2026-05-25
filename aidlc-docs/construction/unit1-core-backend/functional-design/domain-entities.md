# Domain Entities — Unit 1: Core Backend
# 배달비 절약을 위한 음식 공동 구매 앱

**작성일**: 2026-05-24
**단계**: CONSTRUCTION — Functional Design

---

## 엔티티 목록

### Member (회원)

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | Long | PK, auto | |
| nickname | String | UNIQUE, NOT NULL | 서비스 전체 고유, 2~12자 |
| deviceToken | String | UNIQUE, NOT NULL | 앱 최초 실행 시 생성된 UUID |
| createdAt | LocalDateTime | NOT NULL | |

> **Post-MVP**: 학교 이메일(도메인 제한) 로그인 추가 예정

**연관관계**
- Room (1:N) — 개설한 방 목록
- RoomParticipant (1:N) — 참여한 방 목록

---

### Room (방)

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | Long | PK, auto | |
| hostId | Long | FK(Member), NOT NULL | 방장 |
| title | String | NOT NULL | 방 제목 |
| meetingType | Enum | NOT NULL | DELIVERY / DELIVERY_TOGETHER / DINE_OUT |
| restaurantName | String | NOT NULL | 식당명 |
| restaurantAddress | String | NOT NULL | 식당 주소 |
| latitude | Double | NOT NULL | 지도 표시용 |
| longitude | Double | NOT NULL | 지도 표시용 |
| deliveryFee | Integer | NOT NULL | 총 배달비 (원) |
| maxParticipants | Integer | NOT NULL | 방장 설정, 최소 2 |
| status | Enum | NOT NULL | OPEN / CLOSED / CONFIRMED / COMPLETED / CANCELLED |
| closedAt | LocalDateTime | nullable | 자동 마감 시각 |
| meetingAddress | String | nullable | DELIVERY_TOGETHER / DINE_OUT 전용 |
| accountNumber | String | nullable | AES-256 암호화 저장 |
| accountHolder | String | nullable | 예금주 |
| bankName | String | nullable | 은행명 |
| createdAt | LocalDateTime | NOT NULL | |
| updatedAt | LocalDateTime | NOT NULL | |

**연관관계**
- RoomParticipant (1:N)
- OrderItem (1:N)

**상태 전이**
```
OPEN ──(방장 수동 or 시간 초과)──▶ CLOSED
OPEN ──(방장 취소 or 인원 0)───▶ CANCELLED
CLOSED ──(방장 주문확정)────────▶ CONFIRMED
CLOSED ──(방장 취소)────────────▶ CANCELLED
CONFIRMED ──(방장 완료처리)─────▶ COMPLETED
CONFIRMED ──(방장 취소)─────────▶ CANCELLED
```

---

### RoomParticipant (방 참여자)

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | Long | PK | |
| roomId | Long | FK(Room), NOT NULL | |
| memberId | Long | FK(Member), NOT NULL | |
| joinedAt | LocalDateTime | NOT NULL | |

**제약**: (roomId, memberId) UNIQUE

---

### OrderItem (주문 항목)

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | Long | PK | |
| roomId | Long | FK(Room), NOT NULL | |
| memberId | Long | FK(Member), NOT NULL | 주문자 |
| menuName | String | NOT NULL | 메뉴명 |
| quantity | Integer | NOT NULL, >= 1 | |
| price | Integer | NOT NULL, >= 0 | 메뉴 단가 (원) |
| createdAt | LocalDateTime | NOT NULL | |

---

### Settlement (정산 결과)

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | Long | PK | |
| roomId | Long | FK(Room), UNIQUE | |
| totalMenuAmount | Integer | NOT NULL | 전체 메뉴 합계 |
| totalDeliveryFee | Integer | NOT NULL | 총 배달비 |
| participantCount | Integer | NOT NULL | 정산 기준 인원 |
| deliveryFeePerPerson | Integer | NOT NULL | 1인당 배달비 (올림) |
| hostSurplus | Integer | NOT NULL | 방장 수령 잉여금 (올림 차액) |
| createdAt | LocalDateTime | NOT NULL | |
| memberSettlements | List\<MemberSettlement\> | | 참여자별 정산 |

---

### MemberSettlement (참여자별 정산)

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | Long | PK | |
| settlementId | Long | FK(Settlement) | |
| memberId | Long | FK(Member) | |
| menuAmount | Integer | NOT NULL | 본인 메뉴 합계 |
| deliveryFeeShare | Integer | NOT NULL | 본인 배달비 부담 (올림) |
| totalAmount | Integer | NOT NULL | menuAmount + deliveryFeeShare |
| isHost | Boolean | NOT NULL | 방장 여부 |

---

## ERD (텍스트)

```
Member ──< RoomParticipant >── Room
                                │
                                ├──< OrderItem
                                └──< Settlement >── MemberSettlement
```
