# Services
# 배달비 절약을 위한 음식 공동 구매 앱

서비스 레이어의 오케스트레이션 패턴과 책임을 정의합니다.

---

## 서비스 레이어 원칙

- 각 모듈의 Service 클래스가 비즈니스 로직의 진입점
- Controller는 HTTP 변환만 담당 (비즈니스 로직 없음)
- Service 간 직접 의존 최소화 — 이벤트 발행 또는 도메인 이벤트 사용
- 트랜잭션 경계: Service 메서드 단위

---

## SVC-01: 방 개설 오케스트레이션

**트리거**: `POST /api/rooms`

```
RoomController
  └─ RoomService.createRoom()
       ├─ MapService.geocode()          // 배달 주소 → 좌표 변환
       ├─ RoomRepository.save()         // 방 DB 저장
       └─ [이벤트 발행] RoomCreatedEvent
            └─ NotificationService      // (선택) 관심 지역 사용자 알림
```

**트랜잭션**: `RoomService.createRoom()` 단위

---

## SVC-02: 방 참여 오케스트레이션

**트리거**: `POST /api/rooms/{roomId}/join`

```
RoomController
  └─ RoomService.joinRoom()
       ├─ 인원 초과 검사 (비즈니스 규칙 #1)
       ├─ 마감 시간 검사 (비즈니스 규칙 #2)
       ├─ RoomRepository.addParticipant()
       └─ ChatService.publishSystemMessage()  // "XXX님이 참여했습니다"
            └─ RedisPubSubService.publish()
```

**트랜잭션**: `RoomService.joinRoom()` 단위

---

## SVC-03: 실시간 채팅 오케스트레이션

**트리거**: STOMP `/app/chat/{roomId}`

```
ChatController (@MessageMapping)
  └─ ChatService.sendMessage()
       ├─ 방 참여자 권한 검사
       ├─ ChatRepository.save()          // PostgreSQL 영구 저장
       └─ RedisPubSubService.publish()   // Redis pub/sub → 모든 인스턴스 브로드캐스트
            └─ STOMP /topic/chat/{roomId}  // 구독 클라이언트에 전달
```

**트랜잭션**: 없음 (메시지 저장 + 발행은 별개, 손실 허용)

---

## SVC-04: 주문 확정 오케스트레이션

**트리거**: `POST /api/rooms/{roomId}/confirm`

```
OrderController
  └─ OrderService.confirmOrder()
       ├─ 방장 권한 검사 (비즈니스 규칙 #3)
       ├─ OrderRepository.lockOrders()
       ├─ RoomService.updateRoomStatus(RECRUITING → ORDER_COMPLETE)
       └─ NotificationService.sendPushToRoom()   // FCM 전체 참여자 알림
            └─ FCMClient.sendMulticast()
```

**트랜잭션**: `OrderService.confirmOrder()` 단위 (Room 상태 변경 포함)

---

## SVC-05: AI 추천 오케스트레이션

**트리거**: `POST /api/ai/recommend` (미니PC Spring Boot)

```
AIRecommendController
  └─ AIRecommendService.recommend()
       └─ LambdaGatewayClient.post(API Gateway)
             └─ [AWS] API Gateway → Lambda
                    └─ RecommendationHandler
                         ├─ RuleEngine.filterAndRank()   // 규칙 기반 순위 결정
                         └─ GeminiClient.generateExplanation()  // Top-3 설명 생성
```

**트랜잭션**: 없음 (외부 서비스 호출)  
**장애 처리**: Lambda 타임아웃 시 빈 리스트 반환 (fallback), 설명 생성 실패 시 기본 문구 사용

---

## SVC-06: 배달비 정산 오케스트레이션

**트리거**: `GET /api/rooms/{roomId}/settlement`

```
OrderController
  └─ SettlementService.getSettlement()
       ├─ OrderRepository.findByRoomAndUser()  // 본인 주문 항목
       ├─ RoomRepository.findById()            // 방 정보 (총 배달비, 참여자 수)
       └─ DeliveryFeeCalculator.calculatePerPersonFee()  // 순수 함수
            └─ DeliveryFeeCalculator.calculateTotal()    // 순수 함수
```

**트랜잭션**: 읽기 전용

---

## SVC-07: 이미지 업로드 오케스트레이션

**트리거**: `POST /api/rooms/{roomId}/images`

```
RoomController
  └─ RoomService.uploadImage()
       └─ S3Client.putObject()   // AWS SDK v2 → S3 업로드
            └─ CloudFront URL 반환 → DB 저장
```

---

## 서비스 간 의존 관계

```
RoomService
  ├── depends on: MapService (지오코딩)
  ├── depends on: ChatService (시스템 메시지)
  └── depends on: NotificationService (이벤트 기반)

OrderService
  ├── depends on: RoomService (방 상태 확인)
  └── depends on: NotificationService (주문 확정 알림)

ChatService
  └── depends on: RedisPubSubService (브로드캐스트)

AIRecommendService
  └── depends on: LambdaGatewayClient (AWS API Gateway)

SettlementService
  ├── depends on: OrderRepository
  ├── depends on: RoomRepository
  └── depends on: DeliveryFeeCalculator (순수 함수)
```

---

## Redis 사용 패턴

| 용도 | Key 패턴 | TTL |
|------|----------|-----|
| 세션 캐시 | `session:{userId}` | 30분 |
| 방 실시간 참여자 수 카운터 | `room:count:{roomId}` | 방 종료 시 삭제 |
| 채팅 pub/sub 채널 | `chat:{roomId}` | - |
| AI 추천 결과 캐시 | `ai:rec:{hash(criteria)}` | 10분 |

---

## 에러 처리 전략

| 계층 | 처리 방식 |
|------|----------|
| Controller | `@ExceptionHandler` → 표준 에러 응답 (`ErrorResponse`) |
| Service | 비즈니스 예외 (`RoomFullException`, `DeadlinePassedException` 등) throw |
| External API | Retry 2회 → 실패 시 fallback 응답 반환 |
| Lambda | CloudWatch 로그, 오류 시 빈 추천 결과 반환 |
