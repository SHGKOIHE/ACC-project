# Business Logic Model — Unit 1: Core Backend
# 배달비 절약을 위한 음식 공동 구매 앱

**작성일**: 2026-05-24
**단계**: CONSTRUCTION — Functional Design

---

## 1. 회원 인증 플로우 (EP-01)

> **MVP**: 닉네임 + 디바이스 UUID. JWT/소셜 없음.

### 최초 등록
```
1. 앱에서 UUID(v4) 생성 → AsyncStorage 저장
2. 닉네임 입력
3. POST /api/auth/register {nickname, deviceToken}
4. 서버: 닉네임 중복 확인
   ├─ 중복 → 409 Conflict
   └─ 없음 → Member 저장, 200 OK
5. 이후 모든 요청: X-Device-Token 헤더 포함
```

### 재접속 (앱 재시작)
```
1. AsyncStorage에서 deviceToken 로드
2. GET /api/auth/me (X-Device-Token 헤더)
3. 서버: deviceToken으로 Member 조회
   ├─ 존재 → 사용자 정보 반환
   └─ 없음 → 401 → 닉네임 등록 화면으로
```

---

## 2. 방 생성 플로우 (EP-02, EP-03)

```
1. 방장 인증 확인 (JWT)
2. 입력 검증 (제목, 식당, 주소, 최대인원>=2, 배달비, 마감시각)
3. meetingType에 따른 추가 필드 검증
   ├─ DELIVERY: 기본 필드만
   ├─ DELIVERY_TOGETHER: meetingAddress 필수
   └─ DINE_OUT: meetingAddress 필수
4. 계좌번호 AES-256 암호화 (입력한 경우)
5. Room 저장 (status=OPEN)
6. 방장 RoomParticipant 자동 등록
7. 자동 마감 스케줄 등록 (closedAt 기준)
```

---

## 3. 방 탐색 플로우 (EP-04)

```
1. 현재 위치 (lat, lng) + 필터 파라미터 수신
   - category (선택): 한식/중식/일식 등
   - meetingType (선택): DELIVERY / DELIVERY_TOGETHER / DINE_OUT
   - radiusMeters (조건부): DINE_OUT 선택 시 자동 포함, 기본값 1000m
2. OPEN 상태 방 조회
3. 필터 적용
   ├─ category 있으면 → restaurantCategory 필터
   ├─ meetingType 있으면 → meetingType 필터
   └─ meetingType == DINE_OUT 이면 → 거리 필터 자동 적용
       (haversine 거리 <= radiusMeters)
       ※ 공동배달/배달+모여먹기는 거리 필터 미적용 (배달은 거리 무관)
4. 지도 마커용 응답 반환
   (id, title, lat, lng, meetingType, currentCount, maxParticipants, closedAt)
```

**거리 필터 자동 활성화 근거**:
- 직접외식(DINE_OUT)은 실제로 이동해야 하므로 거리가 핵심 조건
- 공동배달/배달+모여먹기는 배달앱이 거리를 커버하므로 거리 무관

---

## 4. 방 참여 플로우 (EP-05)

```
1. 방 상태 확인 (OPEN이어야 함)
2. 이미 참여 중인지 확인 (중복 참여 방지)
3. 최대 인원 초과 여부 확인 (currentCount < maxParticipants)
4. RoomParticipant 저장
5. 채팅방 입장 이벤트 발행 (Unit 2 연동)
6. 방장에게 FCM 알림: "{닉네임}님이 참여했습니다"
```

---

## 5. 주문 항목 관리 플로우 (EP-05)

### 주문 추가
```
1. 방 상태 확인 (OPEN 또는 CLOSED이어야 함)
2. 참여자 확인 (방 참여자이어야 함)
3. OrderItem 저장
4. 실시간 주문 목록 변경 이벤트 발행 (WebSocket)
```

### 주문 삭제
```
1. 방 상태 확인 (CONFIRMED 이전이어야 함)
2. 소유권 확인 (본인 주문 또는 방장)
3. OrderItem 삭제
```

---

## 6. 주문 확정 및 정산 플로우 (EP-08)

```
1. 방장 권한 확인
2. 방 상태 확인 (CLOSED이어야 함)
3. 참여자 수 >= 2 확인
4. 모든 참여자 주문 항목 >= 1개 확인
5. Settlement 생성
   ├─ participantCount = 현재 참여자 수
   ├─ totalDeliveryFee = room.deliveryFee
   ├─ deliveryFeePerPerson = ceil(totalDeliveryFee / participantCount)
   ├─ hostSurplus = (deliveryFeePerPerson × participantCount) - totalDeliveryFee
   └─ 참여자별 MemberSettlement 생성
       ├─ menuAmount = 본인 OrderItem.price × quantity 합산
       ├─ deliveryFeeShare = deliveryFeePerPerson
       └─ totalAmount = menuAmount + deliveryFeeShare
6. Room 상태 CONFIRMED 변경
7. 참여자 전원에게 FCM 알림: "주문이 확정되었습니다. 정산 확인하세요."
```

---

## 7. 방 취소 플로우

```
1. 방장 권한 확인
2. 방 상태 확인 (COMPLETED가 아니어야 함)
3. Room 상태 CANCELLED 변경
4. 참여자 전원에게 FCM 알림: "방이 취소되었습니다."
```

---

## 8. 자동 마감 스케줄러

```
실행 주기: 1분마다
동작:
  1. status=OPEN AND closedAt <= 현재시각 인 Room 조회
  2. UPDATE rooms SET status='CLOSED'
     WHERE status='OPEN' AND closed_at <= NOW()   ← Race Condition 방지
  3. 변경된 방의 참여자들에게 FCM 알림: "방이 마감되었습니다."
```

---

## 9. DeliveryFeeCalculator (PBT-02, PBT-03)

```java
public class DeliveryFeeCalculator {

    /**
     * @param totalDeliveryFee 총 배달비 (>= 0)
     * @param participantCount 참여자 수 (>= 2)
     * @return 1인당 배달비 (올림)
     */
    public int calculatePerPersonFee(int totalDeliveryFee, int participantCount) {
        return (int) Math.ceil((double) totalDeliveryFee / participantCount);
    }

    /**
     * @return 방장 수령 잉여금 (>= 0, < participantCount)
     */
    public int calculateHostSurplus(int totalDeliveryFee, int participantCount) {
        int perPerson = calculatePerPersonFee(totalDeliveryFee, participantCount);
        return (perPerson * participantCount) - totalDeliveryFee;
    }
}
```

**PBT 속성**:
- `perPerson * count >= totalFee` (항상)
- `surplus >= 0` (항상)
- `surplus < count` (항상)
- `perPerson * count - surplus == totalFee` (항상)

---

## 10. RoomStateValidator (PBT-07)

```java
public class RoomStateValidator {

    private static final Map<RoomStatus, Set<RoomStatus>> ALLOWED_TRANSITIONS = Map.of(
        OPEN,      Set.of(CLOSED, CANCELLED),
        CLOSED,    Set.of(CONFIRMED, CANCELLED),
        CONFIRMED, Set.of(COMPLETED, CANCELLED),
        COMPLETED, Set.of(),
        CANCELLED, Set.of()
    );

    public boolean isValidTransition(RoomStatus from, RoomStatus to) {
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
```

**PBT 속성**:
- COMPLETED, CANCELLED는 terminal state — 어떤 상태로도 전이 불가
- 역방향 전이 불가 (예: CONFIRMED → OPEN 불가)
