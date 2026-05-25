# Logical Components — Unit 1: Core Backend
# 배달비 절약을 위한 음식 공동 구매 앱

**작성일**: 2026-05-24
**단계**: CONSTRUCTION — NFR Design

---

## 컴포넌트 구성도

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                   │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Security Filter Chain                    │  │
│  │  DeviceTokenAuthenticationFilter                      │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                   │
│  ┌──────────┬───────────┴──┬──────────┐                    │
│  │  Auth    │     Room     │  Order   │  ← Controllers      │
│  │Controller│  Controller  │Controller│                    │
│  └────┬─────┴──────┬───────┴────┬─────┘                    │
│       │            │            │                           │
│  ┌────▼─────┬──────▼───────┬────▼─────┐                    │
│  │  Auth    │  RoomService │  Order   │  ← Services         │
│  │ Service  │  RoomState   │ Service  │                    │
│  │          │  Validator   │ Delivery │                    │
│  │          │              │ FeeCalc  │                    │
│  └────┬─────┴──────┬───────┴────┬─────┘                    │
│       │            │            │                           │
│  ┌────▼─────┬──────▼───────┬────▼─────┐                    │
│  │  Member  │     Room     │  Order   │  ← Repositories     │
│  │  Repo    │     Repo     │  Repo    │                    │
│  └────┬─────┴──────┬───────┴────┬─────┘                    │
│       │            │            │                           │
│  ┌────▼─────────────▼───────────▼─────┐                    │
│  │         AesEncryptor               │  ← Infrastructure   │
│  │         GlobalExceptionHandler     │                    │
│  │         RoomAutoCloseScheduler     │                    │
│  └────────────────────────────────────┘                    │
└─────────────────────┬───────────────────────────────────────┘
                      │
          ┌───────────┼───────────┐
          ▼           ▼           ▼
      PostgreSQL    Redis      (Unit 2)
                              FCM / WebSocket
```

---

## 컴포넌트 상세

### DeviceTokenAuthenticationFilter
- **역할**: 모든 보호 API 인증 처리
- **의존**: Redis, MemberRepository
- **패턴**: OncePerRequestFilter, Cache-Aside

### AuthService
- **역할**: 회원 등록, 내 정보 조회
- **의존**: MemberRepository, Redis(캐시 무효화)
- **주요 메서드**: `register(nickname, deviceToken)`, `getMe(deviceToken)`

### RoomService
- **역할**: 방 CRUD, 상태 전이, 참여/탈퇴, 탐색 필터
- **의존**: RoomRepository, RoomParticipantRepository, RoomStateValidator, NotificationPort
- **패턴**: 낙관적 락 (`@Version`)
- **주요 메서드**: `createRoom(...)`, `joinRoom(roomId, memberId)`, `closeRoom(roomId, hostId)`, `cancelRoom(roomId, hostId)`, `searchRooms(lat, lng, category, meetingType)`
- **필터 규칙**: `DINE_OUT` 선택 시 거리 필터 자동 적용 (Haversine, 기본 1km)

### RoomStateValidator _(PBT-07)_
- **역할**: 방 상태 전이 유효성 검증
- **의존**: 없음 (순수 함수)
- **주요 메서드**: `isValidTransition(from, to): boolean`

### OrderService
- **역할**: 주문 항목 관리, 주문 확정, 정산 생성
- **의존**: OrderItemRepository, SettlementRepository, DeliveryFeeCalculator, NotificationPort
- **주요 메서드**: `addOrderItem(...)`, `confirmOrder(roomId, hostId)`, `getSettlement(roomId)`

### DeliveryFeeCalculator _(PBT-02, PBT-03)_
- **역할**: 배달비 올림 계산, 방장 잉여금 계산
- **의존**: 없음 (순수 함수)
- **주요 메서드**: `calculatePerPersonFee(totalFee, count)`, `calculateHostSurplus(totalFee, count)`

### AesEncryptor
- **역할**: 계좌번호 AES-256-GCM 암호화/복호화
- **의존**: 환경변수 `ENCRYPTION_KEY`
- **패턴**: JPA `AttributeConverter` 연동

### RoomAutoCloseScheduler
- **역할**: 만료된 방 자동 마감
- **의존**: RoomRepository, NotificationPort
- **패턴**: `@Scheduled(fixedDelay=60000)`

### GlobalExceptionHandler
- **역할**: 전역 예외 처리, 통일된 에러 응답
- **패턴**: `@RestControllerAdvice`

### NotificationPort _(인터페이스)_
- **역할**: FCM 알림 발송 추상화 (Unit 2 구현)
- **의존**: 없음 (인터페이스)
- **메서드**: `sendToRoom(roomId, title, body)`, `sendToMember(memberId, title, body)`

---

## 외부 인터페이스

| 인터페이스 | 방향 | 담당 유닛 |
|-----------|------|----------|
| `NotificationPort` | Unit 1 → Unit 2 | Unit 2가 FCM 구현체 제공 |
| WebSocket 이벤트 | Unit 1 → Unit 2 | 방 참여/탈퇴 이벤트 발행 |
| Lambda 호출 | Unit 1 → Unit 3 | `AiRecommendService` (별도 모듈) |
| Kakao REST API | Unit 1 → 외부 | `MapService` (별도 모듈) |

---

## 패키지 구조

```
com.foodgroup/
├── common/
│   ├── dto/ApiResponse, ErrorResponse, PageDto
│   ├── exception/BusinessException, ErrorCode
│   ├── security/DeviceTokenAuthenticationFilter, MemberPrincipal
│   ├── util/AesEncryptor
│   └── config/SecurityConfig, RedisConfig, SchedulerConfig
├── auth/
│   ├── controller/AuthController
│   ├── service/AuthService
│   ├── repository/MemberRepository
│   └── domain/Member
├── room/
│   ├── controller/RoomController
│   ├── service/RoomService, RoomStateValidator, RoomAutoCloseScheduler
│   ├── repository/RoomRepository, RoomParticipantRepository
│   └── domain/Room, RoomParticipant, RoomStatus
└── order/
    ├── controller/OrderController
    ├── service/OrderService, DeliveryFeeCalculator
    ├── repository/OrderItemRepository, SettlementRepository
    └── domain/OrderItem, Settlement, MemberSettlement
```
