# Code Generation Plan — Unit 2: Realtime + Mobile

**작성일**: 2026-05-24
**단계**: CONSTRUCTION — Code Generation

## 확정 설계 결정
- STOMP 브로커: In-memory
- WebSocket 인증: ChannelInterceptor 연결 시 1회
- FCM: Firebase Admin SDK + @Async 비동기 발송
- 채팅 삭제: @Scheduled cron 매일 새벽 3시
- 앱 상태관리: React Query + Context
- API 클라이언트: axios + React Query
- 빌드: EAS Build (Expo Managed + expo-dev-client)
- WebSocket 노출: CloudFront 동일 경로 /ws

---

## 생성 단계

### Step 1: 백엔드 — 채팅 모듈
- [x] `V6__add_fcm_token_and_chat.sql` — members.fcm_token 컬럼, chat_messages 테이블
- [x] `ChatMessage` 엔티티
- [x] `ChatMessageRepository`
- [x] `WebSocketConfig` — STOMP In-memory 브로커 설정
- [x] `ChatChannelInterceptor` — ChannelInterceptor 인증
- [x] `ChatService` — 메시지 저장, 이력 조회, 30일 삭제 스케줄러
- [x] `ChatController` — WebSocket @MessageMapping
- [x] `ChatHistoryController` — REST GET /api/rooms/{id}/chats (이력 50개)
- [x] `ChatServiceTest`

### Step 2: 백엔드 — FCM 알림 모듈
- [x] `build.gradle`에 `firebase-admin` 의존성 추가
- [x] `AsyncConfig` — `@EnableAsync` + `@EnableScheduling`
- [x] `Member.java` — `fcmToken` 필드 + `updateFcmToken()` 메서드 추가
- [x] `AuthService.java` — `updateFcmToken(Long memberId, String fcmToken)` 추가
- [x] `FcmConfig` — Firebase Admin SDK 초기화
- [x] `FcmNotificationAdapter` — NotificationPort 구현체 (@Primary)
- [x] `FcmTokenController` — PUT /api/auth/fcm-token
- [x] `FcmNotificationAdapterTest`

### Step 3: 모바일 — 프로젝트 초기화
- [x] `mobile/package.json` — Expo Managed + expo-dev-client, React Query, axios, stompjs
- [x] `mobile/app.config.js` — Expo 설정, 카카오맵 config plugin
- [x] `mobile/tsconfig.json`
- [x] `mobile/App.tsx`
- [x] `mobile/src/api/client.ts` — axios 인스턴스 (X-Device-Token 인터셉터)
- [x] `mobile/src/api/queryClient.ts` — React Query QueryClient 설정
- [x] `mobile/src/context/AuthContext.tsx` — deviceToken, memberId 전역 상태
- [x] `mobile/src/navigation/AppNavigator.tsx` — React Navigation 스택

### Step 4: 모바일 — 화면 구현
- [x] `screens/NicknameScreen.tsx` — 최초 닉네임 등록
- [x] `screens/RoomListScreen.tsx` — 방 목록 + 필터 (React Query)
- [x] `screens/CreateRoomScreen.tsx` — 방 개설 + 카카오맵 식당 검색
- [x] `screens/RoomDetailScreen.tsx` — 방 상세, 참여/탈퇴, 메뉴 입력, 상태별 액션
- [x] `screens/SettlementScreen.tsx` — 정산 내역
- [x] `screens/ChatScreen.tsx` — 실시간 채팅 (@stomp/stompjs)

### Step 5: 모바일 — 공통 컴포넌트
- [x] `components/RoomCard.tsx`
- [x] `components/OrderItemRow.tsx`
- [x] `components/SettlementSummary.tsx`
- [x] `components/ChatBubble.tsx`

### Step 6: EAS 빌드 설정
- [x] `mobile/eas.json` — EAS Build 프로파일 (development / preview / production)
- [x] `mobile/.env.example` — KAKAO_APP_KEY 등 환경변수

---

## 총 단계: 6단계
## 예상 생성 파일: ~40개
## 상태: ✅ 완료
