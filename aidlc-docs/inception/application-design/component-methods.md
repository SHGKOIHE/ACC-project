# Component Methods
# 배달비 절약을 위한 음식 공동 구매 앱

> **Note**: 상세 비즈니스 로직은 CONSTRUCTION 단계 Functional Design에서 정의됩니다.
> 여기서는 인터페이스(시그니처) 수준만 명세합니다.

---

## BC-01: AuthService

```java
// 회원가입
UserResponse register(RegisterRequest request);

// 로그인 (Access + Refresh 토큰 반환)
TokenResponse login(LoginRequest request);

// 토큰 갱신
TokenResponse refreshToken(String refreshToken);

// 로그아웃 (Refresh 토큰 무효화)
void logout(Long userId);

// 프로필 조회
UserProfileResponse getProfile(Long userId);

// 프로필 수정
UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);
```

**PBT 대상 없음** — 외부 상태 의존 (DB, JWT)

---

## BC-02: RoomService / RoomQueryService

```java
// 방 생성
RoomResponse createRoom(Long hostId, CreateRoomRequest request);

// 방 참여
void joinRoom(Long roomId, Long userId);

// 방 퇴장
void leaveRoom(Long roomId, Long userId);

// 방 상태 변경 (방장만)
RoomResponse updateRoomStatus(Long roomId, Long hostId, RoomStatus newStatus);

// 방 상세 조회
RoomDetailResponse getRoom(Long roomId);

// 내가 참여한 방 목록
List<RoomSummaryResponse> getMyRooms(Long userId);
```

```java
// 위치 기반 주변 방 검색
List<RoomSummaryResponse> searchNearbyRooms(
    double latitude, double longitude,
    double radiusKm,
    RoomSearchFilter filter   // 카테고리, 방 유형, 마감시간
);
```

**PBT 대상**: `RoomStateValidator`

```java
// 방 상태 전이 유효성 검사 — 순수 함수
boolean isValidTransition(RoomStatus current, RoomStatus next);
```

---

## BC-03: OrderService / DeliveryFeeCalculator

```java
// 주문 항목 추가
OrderItemResponse addOrderItem(Long roomId, Long userId, AddOrderItemRequest request);

// 주문 항목 수정 (본인만, 마감 전)
OrderItemResponse updateOrderItem(Long itemId, Long userId, UpdateOrderItemRequest request);

// 주문 항목 삭제 (본인만, 마감 전)
void removeOrderItem(Long itemId, Long userId);

// 방 전체 주문 목록 조회
List<ParticipantOrderResponse> getRoomOrders(Long roomId);

// 주문 확정 (방장만)
void confirmOrder(Long roomId, Long hostId);

// 정산 내역 조회
SettlementResponse getSettlement(Long roomId, Long userId);
```

**PBT-02, PBT-03 주요 대상**: `DeliveryFeeCalculator`

```java
// 1인당 배달비 계산 — 순수 함수 (소수점 올림)
// @Property: result >= ceil(totalFee / participantCount)
// @Property: result * participantCount >= totalFee (커버리지 보장)
// @Property: participantCount == 0 이면 IllegalArgumentException
int calculatePerPersonFee(int totalDeliveryFee, int participantCount);

// 정산 총액 계산 — 순수 함수
// @Property: result == orderTotal + perPersonDeliveryFee
int calculateTotal(int orderTotal, int perPersonDeliveryFee);
```

---

## BC-04: ChatService

```java
// 메시지 전송 (STOMP 핸들러에서 호출)
ChatMessageResponse sendMessage(Long roomId, Long senderId, String content);

// 채팅 이력 조회 (페이징)
Page<ChatMessageResponse> getChatHistory(Long roomId, Pageable pageable);

// 시스템 메시지 발행 (입장·퇴장·상태변경)
void publishSystemMessage(Long roomId, SystemMessageType type, String payload);
```

**PBT 대상 없음** — I/O 의존

---

## BC-05: NotificationService

```java
// 단건 푸시 발송
void sendPush(Long userId, PushNotification notification);

// 방 참여자 전체 푸시 발송
void sendPushToRoom(Long roomId, PushNotification notification);

// FCM 토큰 등록/갱신
void registerFcmToken(Long userId, String fcmToken);
```

---

## BC-06: MapService

```java
// 주소 → 좌표 변환
Coordinate geocode(String address);

// 두 좌표 간 거리 계산 (km) — 순수 함수
// @Property: distance(A,B) == distance(B,A)  ← PBT-09 후보
double calculateDistance(Coordinate from, Coordinate to);
```

---

## BC-07: AIRecommendService

```java
// AI 추천 요청 (Lambda 호출)
List<RestaurantRecommendation> recommend(RecommendRequest request);

// RecommendRequest 구성
// - preferredCategories: List<String>
// - priceRange: PriceRange (min, max)
// - location: Coordinate
```

---

## BC-08: AdminService

```java
// 사용자 목록 조회
Page<UserSummaryResponse> listUsers(Pageable pageable);

// 계정 정지/해제
void updateUserStatus(Long targetUserId, UserStatus status);

// 신고 목록 조회
Page<ReportResponse> listReports(Pageable pageable);

// 신고 처리
void processReport(Long reportId, ReportAction action);

// 운영 통계
AdminStatsResponse getStats(LocalDate date);
```

---

## AC-01: AI Lambda — RuleEngine

```java
// 후보 식당 필터링 + 랭킹 — 순수 함수
// @Property: 결과는 항상 비어 있지 않거나 빈 리스트 (null 반환 없음)
// @Property: 결과 식당은 모두 priceRange 내에 있음
// @Property: 결과 순서는 점수 내림차순
List<RankedRestaurant> filterAndRank(
    List<Restaurant> candidates,
    RecommendCriteria criteria
);
```

```java
// Gemini 설명 텍스트 생성 (Top-N 식당에 대해)
String generateExplanation(RankedRestaurant restaurant, RecommendCriteria criteria);
```

---

## 프론트엔드 주요 인터페이스

### API Client (src/api/)

```typescript
// Auth
authApi.register(data: RegisterDto): Promise<void>
authApi.login(data: LoginDto): Promise<TokenDto>
authApi.refresh(): Promise<TokenDto>

// Room
roomApi.createRoom(data: CreateRoomDto): Promise<RoomDto>
roomApi.joinRoom(roomId: number): Promise<void>
roomApi.getNearbyRooms(params: SearchParams): Promise<RoomSummaryDto[]>
roomApi.getRoomDetail(roomId: number): Promise<RoomDetailDto>

// Order
orderApi.addItem(roomId: number, data: OrderItemDto): Promise<OrderItemDto>
orderApi.updateItem(itemId: number, data: OrderItemDto): Promise<OrderItemDto>
orderApi.removeItem(itemId: number): Promise<void>
orderApi.getSettlement(roomId: number): Promise<SettlementDto>

// AI
aiApi.recommend(data: RecommendRequestDto): Promise<RecommendResultDto[]>

// WebSocket (STOMP)
chatSocket.connect(roomId: number): void
chatSocket.send(content: string): void
chatSocket.subscribe(callback: (msg: ChatMessageDto) => void): void
chatSocket.disconnect(): void
```
