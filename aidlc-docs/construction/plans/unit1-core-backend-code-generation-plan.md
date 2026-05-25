# Code Generation Plan — Unit 1: Core Backend
# 배달비 절약을 위한 음식 공동 구매 앱

**작성일**: 2026-05-24
**단계**: CONSTRUCTION — Code Generation

## 유닛 컨텍스트
- **담당 에픽**: EP-01, EP-02, EP-03, EP-05, EP-08
- **코드 위치**: `backend/` (Workspace root)
- **언어/프레임워크**: Java 17, Spring Boot 3.x, Gradle
- **인증 방식**: 디바이스 UUID (X-Device-Token 헤더)
- **의존성**: Unit 2 (NotificationPort 인터페이스 제공), Unit 3 (Lambda 인터페이스 수신)

## 스토리 트레이서빌리티
| 에픽 | 구현 단계 |
|------|----------|
| EP-01 회원 인증 | Step 4, 5 (auth 모듈) |
| EP-02 방 개설 (공동배달) | Step 7, 8 (room 모듈) |
| EP-03 방 개설 (모임/외식) | Step 7, 8 (room 모듈) |
| EP-05 방 참여 및 메뉴 선택 | Step 7, 8, 10, 11 (room + order) |
| EP-08 주문 확정 및 배달비 정산 | Step 10, 11 (order 모듈) |

---

## 생성 단계

### Step 1: 프로젝트 구조 설정
- [x] `backend/build.gradle` — 의존성 정의
- [x] `backend/settings.gradle`
- [x] `backend/src/main/resources/application.yml` — 기본 설정
- [x] `backend/src/main/resources/application-local.yml` — 로컬 설정
- [x] `backend/src/main/resources/application-prod.yml` — 운영 설정
- [x] `backend/Dockerfile`
- [x] `infrastructure/docker-compose.yml`
- [x] `infrastructure/docker-compose.local.yml`
- [x] `.github/workflows/deploy.yml` — CI/CD 파이프라인
- [x] `.gitignore` (루트)

### Step 2: common 모듈 — DTO & 예외
- [x] `ApiResponse<T>` — 공통 응답 래퍼
- [x] `ErrorResponse` — 에러 응답 (code, message, timestamp, path)
- [x] `PageDto<T>` — 페이징 응답
- [x] `BusinessException` — 비즈니스 예외 base
- [x] `ErrorCode` enum — 전체 에러 코드
- [x] `GlobalExceptionHandler` — `@RestControllerAdvice`

### Step 3: common 모듈 — 보안 & 설정
- [x] `MemberPrincipal` — 인증 주체
- [x] `DeviceTokenAuthenticationFilter` — `OncePerRequestFilter`
- [x] `SecurityConfig` — Spring Security 설정
- [x] `RedisConfig` — Redis 연결 설정
- [x] `AesEncryptor` — AES-256-GCM 암호화
- [x] `AesAttributeConverter` — JPA AttributeConverter

### Step 4: auth 모듈 — 도메인 & 리포지토리
- [x] `Member` 엔티티 (id, nickname, deviceToken, createdAt)
- [x] `MemberRepository` JPA 인터페이스

### Step 5: auth 모듈 — 서비스 & 컨트롤러
- [x] `AuthService` (register, getMe)
- [x] `AuthController` (POST /api/auth/register, GET /api/auth/me)
- [x] `AuthService` 단위 테스트

### Step 6: DB 마이그레이션 스크립트
- [x] `V1__create_members.sql`
- [x] `V2__create_rooms.sql`
- [x] `V3__create_room_participants.sql`
- [x] `V4__create_order_items.sql`
- [x] `V5__create_settlements.sql`
- [x] Flyway 설정 (`application.yml`)

### Step 7: room 모듈 — 도메인 & 리포지토리
- [ ] `RoomStatus` enum (OPEN, CLOSED, CONFIRMED, COMPLETED, CANCELLED)
- [ ] `MeetingType` enum (DELIVERY, DELIVERY_TOGETHER, DINE_OUT)
- [ ] `Room` 엔티티 (`@Version` 낙관적 락 포함)
- [ ] `RoomParticipant` 엔티티
- [ ] `RoomRepository` JPA 인터페이스 (자동 마감 쿼리 포함)
- [ ] `RoomParticipantRepository`

### Step 8: room 모듈 — 서비스 & 컨트롤러
- [x] `RoomStateValidator` (PBT-07 대상)
- [x] `RoomAutoCloseScheduler` (`@Scheduled`)
- [x] `NotificationPort` 인터페이스 (Unit 2 구현체 수신용) + `NoOpNotificationAdapter`
- [x] `RoomService` (createRoom, joinRoom, leaveRoom, closeRoom, cancelRoom, searchRooms)
- [x] `RoomController` (GET/POST /api/rooms, POST /api/rooms/{id}/join 등)
- [x] `RoomStateValidator` PBT 테스트 (jqwik, PBT-07)
- [x] `RoomService` 단위 테스트

### Step 9: order 모듈 — 도메인 & 리포지토리
- [x] `OrderItem` 엔티티
- [x] `Settlement` 엔티티
- [x] `MemberSettlement` 엔티티
- [x] `OrderItemRepository`
- [x] `SettlementRepository`
- [x] `MemberSettlementRepository`

### Step 10: order 모듈 — 서비스 & 컨트롤러
- [x] `DeliveryFeeCalculator` (PBT-02, PBT-03 대상)
- [x] `OrderService` (addOrderItem, deleteOrderItem, confirmOrder, getSettlement)
- [x] `OrderController` (POST /api/rooms/{id}/orders 등)
- [x] `DeliveryFeeCalculator` PBT 테스트 (jqwik, PBT-02, PBT-03)
- [x] `OrderService` 단위 테스트

### Step 11: OpenAPI 문서 설정
- [x] `springdoc-openapi` 설정 (`OpenApiConfig`)
- [x] 주요 API 어노테이션 (`@Operation`, `@Tag`) — Auth/Room/Order 전체 적용
- [x] OpenAPI yaml 자동 생성 확인

### Step 12: 배포 스크립트
- [x] `infrastructure/scripts/backup.sh` — pg_dump → S3
- [x] `infrastructure/.env.example` — 환경변수 템플릿

---

## 총 단계: 12단계
## 예상 생성 파일: ~60개
