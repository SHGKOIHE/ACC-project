# Domain Entities — Unit 2: Realtime + Mobile
# 배달비 절약을 위한 음식 공동 구매 앱

**작성일**: 2026-05-24
**단계**: CONSTRUCTION — Functional Design

---

## 백엔드 신규 엔티티

### ChatMessage
```
chat_messages
├── id            BIGINT PK (IDENTITY)
├── room_id       BIGINT NOT NULL (FK → rooms.id)
├── member_id     BIGINT NULLABLE (NULL = 시스템 메시지)
├── type          VARCHAR NOT NULL (TALK | ENTER | LEAVE | NOTICE)
├── content       TEXT NOT NULL
└── created_at    TIMESTAMP NOT NULL
```
- 인덱스: `(room_id, created_at DESC)` — 이력 조회 최적화
- 삭제 정책: 방 종료 30일 후 `@Scheduled` cron 일괄 삭제

### Member 테이블 변경
```
members 테이블에 컬럼 추가:
└── fcm_token     VARCHAR NULLABLE — FCM 디바이스 토큰
```
- Flyway V6 마이그레이션으로 추가
- null 허용 (토큰 미등록 사용자 알림 발송 스킵)

---

## React Native 주요 타입

### RoomSummary (방 목록 카드용)
```typescript
type RoomSummary = {
  id: number;
  title: string;
  meetingType: 'DELIVERY' | 'DELIVERY_TOGETHER' | 'DINE_OUT';
  restaurantName: string;
  restaurantCategory?: string;
  currentParticipantCount: number;
  maxParticipants: number;
  deliveryFee: number;
  status: RoomStatus;
  closedAt?: string;
  distanceMeters?: number; // DINE_OUT만
};
```

### ChatMessage (앱 채팅 메시지)
```typescript
type ChatMessage = {
  id: number;
  roomId: number;
  memberId?: number;
  nickname?: string;
  type: 'TALK' | 'ENTER' | 'LEAVE' | 'NOTICE';
  content: string;
  createdAt: string;
};
```

### SettlementDetail (정산 화면)
```typescript
type SettlementDetail = {
  totalMenuAmount: number;
  deliveryFeePerPerson: number;
  totalAmount: number;
  bankName: string;
  accountHolder: string;
  accountNumber: string;
  myOrderItems: OrderItem[];
};
```

---

## 인터페이스 계약 (Unit 1 ↔ Unit 2)

### NotificationPort 구현체 (Unit 2 제공)
```java
// Unit 1이 인터페이스 정의, Unit 2가 구현체 제공
@Service
@Primary
public class FcmNotificationAdapter implements NotificationPort {
    void sendToRoom(Long roomId, String title, String body);
    void sendToMember(Long memberId, String title, String body);
}
```

### WebSocket STOMP 메시지 포맷
```json
// 구독: /topic/room/{roomId}
// 발행: /app/room/{roomId}/chat
{
  "type": "TALK",
  "content": "치킨 시킬 사람?"
}

// 브로드캐스트 (서버 → 클라이언트)
{
  "id": 123,
  "type": "TALK",
  "memberId": 5,
  "nickname": "배고파",
  "content": "치킨 시킬 사람?",
  "createdAt": "2026-05-24T12:00:00"
}
```
