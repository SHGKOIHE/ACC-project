# Business Rules — Unit 2: Realtime + Mobile
# 배달비 절약을 위한 음식 공동 구매 앱

**작성일**: 2026-05-24
**단계**: CONSTRUCTION — Functional Design

---

## BR-CHAT: 채팅 규칙

### BR-CHAT-01: 채팅방 접근 제한
- 방 참여자만 해당 방의 채팅방 입장 가능
- WebSocket 연결 시 서버 ChannelInterceptor에서 `room_participants` 조회로 검증
- 미참여자 연결 시 즉시 세션 종료 (403)

### BR-CHAT-02: 메시지 저장 정책
- 모든 채팅 메시지 DB 저장 (ChatMessage 테이블)
- 방이 `COMPLETED` 또는 `CANCELLED` 상태가 된 시점 기준 30일 후 자동 삭제
- 삭제는 `@Scheduled` cron으로 처리 (매일 새벽 3시)

### BR-CHAT-03: 메시지 타입
- `TALK`: 일반 텍스트 메시지
- `ENTER`: 참여자 입장 시스템 메시지 ("OOO님이 입장했습니다")
- `LEAVE`: 참여자 퇴장 시스템 메시지 ("OOO님이 나갔습니다")
- `NOTICE`: 방 상태 변경 알림 ("방장이 마감했습니다" 등)

### BR-CHAT-04: 채팅 이력 조회
- 방 입장 시 최근 메시지 50개 REST API로 선로드
- 이후 실시간 메시지는 WebSocket으로 수신

---

## BR-NOTIFICATION: 푸시 알림 규칙

### BR-NOTIFICATION-01: FCM 토큰 관리
- `members` 테이블에 `fcm_token` 컬럼 추가
- 앱 실행 시마다 FCM 토큰 갱신 요청 (POST /api/auth/fcm-token)
- 토큰 null인 경우 알림 발송 스킵 (오류 아님)

### BR-NOTIFICATION-02: 알림 발송 시점
| 이벤트 | 대상 | 내용 |
|--------|------|------|
| 새 참여자 입장 | 방장 | "OOO님이 참여했습니다" |
| 방 마감 | 전체 참여자 | "방이 마감됐습니다. 메뉴를 입력해주세요" |
| 주문 확정 | 전체 참여자 | "주문이 확정됐습니다. 정산 내역을 확인해주세요" |
| 방 취소 | 전체 참여자 | "방장이 방을 취소했습니다" |

### BR-NOTIFICATION-03: 알림 실패 처리
- FCM 발송 실패 시 로그만 기록, 예외 전파하지 않음 (non-blocking)
- 핵심 비즈니스 로직(방 상태 전이 등)이 알림 실패로 롤백되지 않음

---

## BR-MOBILE: React Native 앱 규칙

### BR-MOBILE-01: 초기 진입 흐름
- 앱 실행 시 AsyncStorage에서 deviceToken 조회
- 없으면 UUID v4 생성 → 닉네임 등록 화면으로 이동
- 있으면 서버 토큰 검증 후 메인(방 목록)으로 이동

### BR-MOBILE-02: MVP 6개 화면
1. **닉네임 등록** — 최초 실행 시 1회
2. **방 목록** — 카테고리/유형 필터, 방 카드 목록
3. **방 개설** — 카카오맵 식당 검색 + 방 정보 입력
4. **방 상세** — 참여/탈퇴, 메뉴 입력, 참여자 목록, 상태별 액션
5. **정산** — 1인당 금액, 계좌 정보, 납부 확인
6. **채팅** — 실시간 메시지, 시스템 메시지

### BR-MOBILE-03: 오프라인 처리
- 네트워크 없으면 토스트로 안내
- 캐시된 방 목록은 오프라인에서도 조회 가능 (읽기 전용)

### BR-MOBILE-04: 카카오맵 연동
- `react-native-kakao-maps` + `expo-dev-client` + config plugin 조합
- 방 개설 화면에서 식당명 검색 → 결과 선택 → 위경도/주소 자동 입력
- Kakao API 키는 앱 환경변수(.env)로 관리
