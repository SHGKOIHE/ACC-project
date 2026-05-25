# Components
# 배달비 절약을 위한 음식 공동 구매 앱

## 배포 구성 요약

| 위치 | 구성요소 |
|------|----------|
| **미니PC (온프레미스)** | Spring Boot 모놀리스, PostgreSQL, Redis, WebSocket Broker |
| **AWS** | CloudFront, API Gateway, Lambda (AI 추천), S3 |
| **모바일** | React Native 앱 (iOS / Android) |

---

## 백엔드 컴포넌트 (Spring Boot 모놀리스 — 미니PC)

패키지 구조: `com.foodshare.{module}`  
아키텍처: Controller → Service → Repository (계층형)

---

### BC-01: auth 모듈

| 항목 | 내용 |
|------|------|
| **패키지** | `com.foodshare.auth` |
| **목적** | 회원 가입/로그인/로그아웃, JWT 토큰 발급·검증 |
| **주요 책임** | 이메일 회원가입, bcrypt 비밀번호 해싱, Access/Refresh Token 관리, Brute-force 방지, 프로필 관리 |
| **관련 FR** | FR-01 |
| **관련 EP** | EP-01 |

**클래스 구성**

| 클래스 | 레이어 | 역할 |
|--------|--------|------|
| `AuthController` | Controller | 회원가입·로그인·로그아웃·토큰 갱신 REST 엔드포인트 |
| `AuthService` | Service | 회원가입 로직, 로그인 검증, 토큰 생성·갱신 |
| `UserRepository` | Repository | 사용자 DB CRUD |
| `JwtProvider` | Component | JWT 생성·파싱·검증 유틸 |
| `SecurityConfig` | Config | Spring Security 필터 체인, CORS, Endpoint 보안 설정 |

---

### BC-02: room 모듈

| 항목 | 내용 |
|------|------|
| **패키지** | `com.foodshare.room` |
| **목적** | 공동구매 방 생성·조회·참여·상태 관리 |
| **주요 책임** | 방 CRUD, 방 유형(공동배달/배달+모여먹기/직접외식) 처리, 참여 인원 관리, 방 상태 전이(모집중→주문완료→배달중→완료/취소), 지도 기반 주변 방 조회 |
| **관련 FR** | FR-02, FR-03, FR-09 |
| **관련 EP** | EP-02, EP-03, EP-04 |

**클래스 구성**

| 클래스 | 레이어 | 역할 |
|--------|--------|------|
| `RoomController` | Controller | 방 생성·조회·참여·상태변경 REST 엔드포인트 |
| `RoomService` | Service | 방 비즈니스 로직, 인원 초과 검사, 마감시간 검사 |
| `RoomQueryService` | Service | 위치 기반 주변 방 검색, 필터링(카테고리·거리·마감) |
| `RoomRepository` | Repository | 방 DB CRUD, 지오쿼리 |
| `RoomStateValidator` | Component | 방 상태 전이 유효성 검사 (순수 함수 — PBT 대상) |

---

### BC-03: order 모듈

| 항목 | 내용 |
|------|------|
| **패키지** | `com.foodshare.order` |
| **목적** | 참여자별 주문 항목 관리, 배달비 계산·정산 |
| **주요 책임** | 주문 항목 추가·수정·삭제(마감 전), 전체 주문 목록 조회, 1인당 배달비 계산(소수점 올림), 주문 확정, 최종 정산 내역 제공 |
| **관련 FR** | FR-05, FR-06 |
| **관련 EP** | EP-05, EP-08 |

**클래스 구성**

| 클래스 | 레이어 | 역할 |
|--------|--------|------|
| `OrderController` | Controller | 주문 항목 CRUD, 주문 확정, 정산 조회 REST 엔드포인트 |
| `OrderService` | Service | 주문 항목 관리, 권한 검사(본인 주문만 수정), 주문 확정 처리 |
| `OrderRepository` | Repository | 주문 항목 DB CRUD |
| `DeliveryFeeCalculator` | Component | 배달비 1/n 계산 순수 함수 — **PBT-02,03 주요 대상** |
| `SettlementService` | Service | 최종 정산 내역 조합 (주문합계 + 1인당 배달비) |

---

### BC-04: chat 모듈

| 항목 | 내용 |
|------|------|
| **패키지** | `com.foodshare.chat` |
| **목적** | 방 내 실시간 채팅, 시스템 메시지 처리 |
| **주요 책임** | WebSocket/STOMP 메시지 수신·브로드캐스트, 채팅 이력 PostgreSQL 저장, Redis pub/sub으로 다중 인스턴스 간 메시지 전달, 시스템 메시지(입장·퇴장·상태변경) 생성 |
| **관련 FR** | FR-04 |
| **관련 EP** | EP-07 |

**클래스 구성**

| 클래스 | 레이어 | 역할 |
|--------|--------|------|
| `ChatController` | Controller | STOMP 메시지 구독·발행 엔드포인트 (`@MessageMapping`) |
| `ChatService` | Service | 메시지 저장, 시스템 메시지 생성, 채팅 이력 조회 |
| `ChatRepository` | Repository | 채팅 메시지 DB CRUD |
| `RedisPubSubService` | Component | Redis pub/sub 발행·구독 (다중 인스턴스 브로드캐스트) |
| `WebSocketConfig` | Config | STOMP 브로커 설정, Redis 릴레이 연결 |

---

### BC-05: notification 모듈

| 항목 | 내용 |
|------|------|
| **패키지** | `com.foodshare.notification` |
| **목적** | FCM 기반 모바일 푸시 알림 발송 |
| **주요 책임** | 주문 확정·배달중·완료 시 참여자 전체 푸시, 방 마감 임박 알림, FCM 토큰 관리 |
| **관련 FR** | FR-07 |
| **관련 EP** | EP-08 |

**클래스 구성**

| 클래스 | 레이어 | 역할 |
|--------|--------|------|
| `NotificationService` | Service | 알림 발송 로직, 대상 사용자 조회 |
| `FCMClient` | Component | Firebase Admin SDK 래퍼, 메시지 빌드·전송 |
| `FcmTokenRepository` | Repository | 사용자별 FCM 토큰 저장·조회 |

---

### BC-06: map 모듈

| 항목 | 내용 |
|------|------|
| **패키지** | `com.foodshare.map` |
| **목적** | 서버 사이드 위치 처리 (Kakao REST API) |
| **주요 책임** | 주소 → 좌표 변환(지오코딩), 좌표 기반 주변 검색, 거리 계산 |
| **관련 FR** | FR-03 |
| **관련 EP** | EP-04 |

**클래스 구성**

| 클래스 | 레이어 | 역할 |
|--------|--------|------|
| `MapService` | Service | 지오코딩, 주변 방 좌표 계산 |
| `KakaoMapClient` | Component | Kakao Local REST API HTTP 클라이언트 |

---

### BC-07: ai 모듈 (클라이언트)

| 항목 | 내용 |
|------|------|
| **패키지** | `com.foodshare.ai` |
| **목적** | AWS Lambda AI 추천 서비스 호출 클라이언트 |
| **주요 책임** | 사용자 선호(카테고리·가격대·위치) 전달, 추천 결과(식당 + 설명 텍스트) 수신·반환 |
| **관련 FR** | FR-10 |
| **관련 EP** | EP-06 |

**클래스 구성**

| 클래스 | 레이어 | 역할 |
|--------|--------|------|
| `AIRecommendController` | Controller | AI 추천 요청 REST 엔드포인트 |
| `AIRecommendService` | Service | 요청 조립, Lambda 응답 파싱, 결과 반환 |
| `LambdaGatewayClient` | Component | API Gateway → Lambda HTTP 호출 클라이언트 |

---

### BC-08: admin 모듈

| 항목 | 내용 |
|------|------|
| **패키지** | `com.foodshare.admin` |
| **목적** | 관리자 전용 운영 기능 |
| **주요 책임** | 사용자 목록·계정 정지/해제, 신고 처리, 운영 통계 조회 |
| **관련 FR** | (EP-10) |
| **관련 EP** | EP-10 |

**클래스 구성**

| 클래스 | 레이어 | 역할 |
|--------|--------|------|
| `AdminController` | Controller | 관리자 전용 REST 엔드포인트 (ROLE_ADMIN 필요) |
| `AdminService` | Service | 사용자 관리, 신고 처리, 통계 집계 |
| `AdminRepository` | Repository | 운영 데이터 조회 |

---

## AWS 컴포넌트

### AC-01: AI 추천 Lambda

| 항목 | 내용 |
|------|------|
| **배포** | AWS Lambda (Java 17 런타임) |
| **트리거** | API Gateway POST `/recommend` |
| **목적** | 규칙 기반 식당 추천 + Gemini API 설명 텍스트 생성 |
| **주요 책임** | 선호 조건으로 후보 식당 필터·랭킹(규칙 엔진), 상위 결과에 대해 Gemini API 호출로 설명 생성 |

**클래스 구성**

| 클래스 | 역할 |
|--------|------|
| `RecommendationHandler` | Lambda 진입점 (`RequestHandler` 구현) |
| `RuleEngine` | 순수 함수: 조건 필터링 + 랭킹 — **jqwik PBT-07,08,09 주요 대상** |
| `GeminiClient` | Gemini API HTTP 클라이언트, 설명 텍스트 생성 |
| `RestaurantDataProvider` | 식당 후보 데이터 로드 (S3 또는 외부 API) |

---

### AC-02: S3 버킷

| 항목 | 내용 |
|------|------|
| **용도** | 식당·메뉴 이미지 저장, PostgreSQL 백업 파일 저장 |
| **접근** | Spring Boot → AWS SDK v2, Lambda → AWS SDK v2 |

---

### AC-03: API Gateway

| 항목 | 내용 |
|------|------|
| **용도** | Lambda 진입점, 요청 인증·rate limiting |
| **엔드포인트** | `POST /recommend` → Lambda AI 추천 |

---

### AC-04: CloudFront

| 항목 | 내용 |
|------|------|
| **용도** | 미니PC 앞단 CDN·방패, S3 이미지 CDN |
| **기능** | 미니PC IP 은닉, AWS Shield Basic DDoS 보호, S3 이미지 캐싱 |

---

## 프론트엔드 컴포넌트 (React Native)

Feature-based 폴더 구조: `src/features/{feature}/`

| 피처 | 폴더 | 담당 EP |
|------|------|---------|
| **auth** | `features/auth/` | EP-01 |
| **room** | `features/room/` | EP-02, EP-03, EP-04 |
| **chat** | `features/chat/` | EP-07 |
| **order** | `features/order/` | EP-05, EP-08 |
| **ai** | `features/ai/` | EP-06 |
| **map** | `features/map/` | EP-04 (Kakao Map SDK) |
| **admin** | `features/admin/` | EP-10 |

**공통 레이어**

| 폴더 | 내용 |
|------|------|
| `src/api/` | Axios 인스턴스, 인터셉터, API 클라이언트 |
| `src/store/` | 전역 상태 관리 |
| `src/components/` | 공통 UI 컴포넌트 |
| `src/utils/` | 포맷터, 계산 유틸 |
