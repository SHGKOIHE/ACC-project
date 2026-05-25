# Data Model
# 배달비 절약 음식 공동구매 앱

**작성일**: 2026-05-24
**작성자**: arch
**DB**: PostgreSQL 16

---

## ERD

```
+----------------+          +---------------------+          +------------------+
|    members     |          |        rooms        |          |   order_items    |
+----------------+          +---------------------+          +------------------+
| PK id          |---+      | PK id               |---+      | PK id            |
|    nickname    |   |      | FK host_id           |   |      | FK room_id       |
|    device_token|   |      |    title             |   +----->| FK member_id     |
|    created_at  |   |      |    meeting_type      |   |      |    menu_name     |
+----------------+   |      |    restaurant_name   |   |      |    quantity      |
        |            |      |    restaurant_address|   |      |    price         |
        |            |      |    latitude          |   |      |    created_at    |
        |            |      |    longitude         |   |      +------------------+
        |            |      |    delivery_fee      |   |
        |            |      |    max_participants  |   |
        |            |      |    status            |   |      +------------------+
        |            |      |    closed_at         |   |      |   settlements    |
        |            |      |    meeting_address   |   |      +------------------+
        |            +----->|    account_number    |   +----->| PK id            |
        |                   |    account_holder    |          | FK room_id (UQ)  |
        |                   |    bank_name         |          |    total_menu_amt|
        |                   |    created_at        |          |    total_del_fee |
        |                   |    updated_at        |          |    participant_ct|
        |                   |    version           |          |    fee_per_person|
        |                   +---------------------+          |    host_surplus  |
        |                            |                       |    created_at    |
        |                            |                       +------------------+
        |                            |                              |
        |              +-------------+------------+                 |
        |              |                          |                 |
        |              v                          |                 v
        |   +---------------------+               |   +------------------------+
        |   | room_participants   |               |   |  member_settlements    |
        |   +---------------------+               |   +------------------------+
        +-->| PK id               |               |   | PK id                  |
            | FK room_id          |               |   | FK settlement_id       |
            | FK member_id        |               +-->| FK member_id           |
            |    joined_at        |                   |    menu_amount         |
            +---------------------+                   |    delivery_fee_share  |
            UQ(room_id, member_id)                    |    total_amount        |
                                                      |    is_host             |
                                                      +------------------------+
```

---

## 테이블 상세

### members

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGSERIAL | PK | |
| nickname | VARCHAR(12) | UNIQUE, NOT NULL | 서비스 전체 고유, 2~12자 |
| device_token | VARCHAR(255) | UNIQUE, NOT NULL | 앱 최초 실행 시 UUID |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

**인덱스**
- `idx_members_device_token` ON (device_token) -- 인증 조회용
- `idx_members_nickname` ON (nickname) -- 중복 체크용 (UNIQUE가 커버)

---

### rooms

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGSERIAL | PK | |
| host_id | BIGINT | FK(members), NOT NULL | 방장 |
| title | VARCHAR(50) | NOT NULL | |
| meeting_type | VARCHAR(20) | NOT NULL | DELIVERY / DELIVERY_TOGETHER / DINE_OUT |
| restaurant_name | VARCHAR(100) | NOT NULL | |
| restaurant_address | VARCHAR(200) | NOT NULL | |
| latitude | DOUBLE PRECISION | NOT NULL | |
| longitude | DOUBLE PRECISION | NOT NULL | |
| delivery_fee | INTEGER | NOT NULL | 총 배달비 (원) |
| max_participants | INTEGER | NOT NULL, CHECK(>=2) | |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'OPEN' | OPEN/CLOSED/CONFIRMED/COMPLETED/CANCELLED |
| closed_at | TIMESTAMP | NULLABLE | 자동 마감 시각 |
| meeting_address | VARCHAR(200) | NULLABLE | DELIVERY_TOGETHER/DINE_OUT 전용 |
| account_number | VARCHAR(255) | NULLABLE | AES-256-GCM 암호화 저장 |
| account_holder | VARCHAR(20) | NULLABLE | 예금주 |
| bank_name | VARCHAR(20) | NULLABLE | 은행명 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| version | INTEGER | NOT NULL, DEFAULT 0 | 낙관적 락 |

**인덱스**
- `idx_rooms_status` ON (status) -- 목록 필터링
- `idx_rooms_status_closed_at` ON (status, closed_at) -- 자동 마감 스케줄러
- `idx_rooms_location` ON (latitude, longitude) -- 거리 기반 조회
- `idx_rooms_host_id` ON (host_id) -- 방장의 방 목록

---

### room_participants

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGSERIAL | PK | |
| room_id | BIGINT | FK(rooms), NOT NULL | |
| member_id | BIGINT | FK(members), NOT NULL | |
| joined_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

**제약**
- UNIQUE (room_id, member_id)

**인덱스**
- `idx_room_participants_room_id` ON (room_id) -- 방 참여자 조회
- `idx_room_participants_member_id` ON (member_id) -- 내 참여 방 조회

---

### order_items

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGSERIAL | PK | |
| room_id | BIGINT | FK(rooms), NOT NULL | |
| member_id | BIGINT | FK(members), NOT NULL | |
| menu_name | VARCHAR(100) | NOT NULL | |
| quantity | INTEGER | NOT NULL, CHECK(>=1) | |
| price | INTEGER | NOT NULL, CHECK(>=0) | 메뉴 단가 (원) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

**인덱스**
- `idx_order_items_room_id` ON (room_id) -- 방 주문 목록
- `idx_order_items_member_room` ON (member_id, room_id) -- 내 주문 조회

---

### settlements

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGSERIAL | PK | |
| room_id | BIGINT | FK(rooms), UNIQUE, NOT NULL | 방당 1건 |
| total_menu_amount | INTEGER | NOT NULL | 전체 메뉴 합계 |
| total_delivery_fee | INTEGER | NOT NULL | 총 배달비 |
| participant_count | INTEGER | NOT NULL | 정산 기준 인원 |
| delivery_fee_per_person | INTEGER | NOT NULL | 1인당 배달비 (올림) |
| host_surplus | INTEGER | NOT NULL | 올림 차액 (방장 수령) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

---

### member_settlements

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGSERIAL | PK | |
| settlement_id | BIGINT | FK(settlements), NOT NULL | |
| member_id | BIGINT | FK(members), NOT NULL | |
| menu_amount | INTEGER | NOT NULL | 본인 메뉴 합계 |
| delivery_fee_share | INTEGER | NOT NULL | 본인 배달비 부담 (올림) |
| total_amount | INTEGER | NOT NULL | menu_amount + delivery_fee_share |
| is_host | BOOLEAN | NOT NULL | 방장 여부 |

**인덱스**
- `idx_member_settlements_settlement_id` ON (settlement_id)
- `idx_member_settlements_member_id` ON (member_id)

---

## 상태 전이 다이어그램 (rooms.status)

```
           +---------+
           |  OPEN   |
           +---------+
            /       \
   (방장수동/     (방장취소/
    시간초과)      인원0명)
          /           \
   +--------+    +-----------+
   | CLOSED |    | CANCELLED |
   +--------+    +-----------+
        |              ^
   (방장확정)      (방장취소)
        |         /
   +-----------+ /
   | CONFIRMED |/
   +-----------+
        |
   (방장완료)
        |
   +-----------+
   | COMPLETED |
   +-----------+
```

---

## Redis 키 설계

| 키 패턴 | 타입 | TTL | 용도 |
|---------|------|-----|------|
| `device:{deviceToken}` | String (memberId) | 24h | 인증 캐시 |
| `room:{roomId}:count` | String (integer) | 방 종료 시 삭제 | 참여자 수 (INCR/DECR) |
| `room:{roomId}:autoclose` | String (roomId) | closedAt까지 | 자동 마감 트리거 (Keyspace Notification) |

---

## 암호화 대상 필드

| 테이블 | 컬럼 | 알고리즘 | 비고 |
|--------|------|----------|------|
| rooms | account_number | AES-256-GCM | JPA AttributeConverter 적용 |

> 환경변수 `ENCRYPTION_KEY`로 키 주입. .env 파일에 보관, .gitignore 등록 필수.
