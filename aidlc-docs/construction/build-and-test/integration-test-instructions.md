# Integration Test Instructions
# 배달비 절약을 위한 음식 공동 구매 앱

**작성일**: 2026-05-24

---

## 통합 테스트 시나리오

Docker Compose로 전체 스택 기동 후 수동 또는 cURL로 검증합니다.

```bash
cd /home/sohegi/projects/ACC_1
docker compose up -d
```

---

## 시나리오 1: 회원 등록 → 방 생성 → 참여 → 주문 확정 → 정산

```bash
BASE=http://localhost:8080

# 1. 회원 A 등록
curl -X POST $BASE/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"nickname":"호스트"}' \
  -H "X-Device-Token: device-aaa"

# 2. 방 생성
curl -X POST $BASE/api/rooms \
  -H "Content-Type: application/json" \
  -H "X-Device-Token: device-aaa" \
  -d '{
    "title":"치킨 같이 시킬 사람",
    "meetingType":"DELIVERY",
    "restaurantName":"BBQ",
    "restaurantCategory":"치킨",
    "maxParticipants":3,
    "deliveryFee":3000
  }'
# → roomId 저장

# 3. 회원 B 등록 + 참여
curl -X POST $BASE/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"nickname":"참여자"}' \
  -H "X-Device-Token: device-bbb"

curl -X POST $BASE/api/rooms/{roomId}/join \
  -H "X-Device-Token: device-bbb"

# 4. 방 마감 (호스트)
curl -X POST $BASE/api/rooms/{roomId}/close \
  -H "X-Device-Token: device-aaa"

# 5. 메뉴 입력 (각 참여자)
curl -X POST $BASE/api/rooms/{roomId}/orders \
  -H "X-Device-Token: device-aaa" \
  -H "Content-Type: application/json" \
  -d '{"items":[{"name":"황금올리브","price":18000}]}'

curl -X POST $BASE/api/rooms/{roomId}/orders \
  -H "X-Device-Token: device-bbb" \
  -H "Content-Type: application/json" \
  -d '{"items":[{"name":"양념치킨","price":17000}]}'

# 6. 주문 확정 (호스트)
curl -X POST $BASE/api/rooms/{roomId}/confirm \
  -H "X-Device-Token: device-aaa"

# 7. 정산 조회
curl $BASE/api/rooms/{roomId}/settlement \
  -H "X-Device-Token: device-bbb"
# 기대: totalAmount = 17000 + ceil(3000/2) = 18500
```

---

## 시나리오 2: 실시간 채팅 (WebSocket)

```bash
# wscat 설치
npm install -g wscat

# STOMP 연결 (X-Device-Token 헤더)
wscat -c "ws://localhost:8080/ws" \
  -H "X-Device-Token: device-aaa"

# STOMP CONNECT 프레임 전송
CONNECT
accept-version:1.2
host:localhost

^@

# 구독
SUBSCRIBE
id:sub-0
destination:/topic/room/{roomId}

^@

# 메시지 발송
SEND
destination:/app/room/{roomId}/chat
content-type:application/json

{"type":"TALK","content":"치킨 시킬 사람?"}
^@
```

---

## 시나리오 3: AI 추천 (Lambda 연동)

```bash
# Lambda 로컬 mock 없이 실제 연동 테스트 (us-east-1 배포 후)
curl -X POST $BASE/api/rooms/{roomId}/recommend \
  -H "X-Device-Token: device-aaa"
# 기대: { recommendations: [...], explanation: "..." }
```

---

## 검증 체크리스트

- [ ] 방 상태 전이 (OPEN → CLOSED → CONFIRMED → COMPLETED)
- [ ] 배달비 분배 (ceil 연산, 호스트 잉여분)
- [ ] 비참여자 API 접근 차단 (403)
- [ ] 채팅 비참여자 WebSocket 연결 차단
- [ ] AI 추천 Lambda 호출 + fallback (Lambda 다운 시 빈 목록 반환)
- [ ] FCM 토큰 등록 → 알림 발송 (NoOp 로그 확인)
