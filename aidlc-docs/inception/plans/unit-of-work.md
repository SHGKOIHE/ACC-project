# Unit of Work
# 배달비 절약을 위한 음식 공동 구매 앱

**작성일**: 2026-05-24
**단계**: INCEPTION — Units Generation
**상태**: 완료

---

## 유닛 구성 결정

| 결정 항목 | 선택 |
|-----------|------|
| 유닛 분리 기준 | A — 3유닛 (팀 스트림) |
| Lambda 위치 | A — Unit 3 독립 |
| 개발 순서 | A — 백엔드 API 우선 |
| 공유 코드 전략 | A — common/ 패키지 분리 |
| 저장소 구성 | A — 모노레포 |
| 브랜치 전략 | B — 유닛 브랜치 (`unit/core-backend` 등) |
| API 계약 | A — OpenAPI (springdoc-openapi 자동생성) |
| 배포 단위 | C — Docker Compose |
| MVP 우선순위 | A — Unit 1 우선 (회원·방·주문) |
| 관리자 기능 | B — MVP 제외 (post-MVP 추가) |

---

## Unit 1 — Core Backend

### 개요
- **스트림**: Stream 1
- **책임**: 사용자 인증, 방 관리, 주문/정산 핵심 도메인
- **배포**: 미니PC (Spring Boot JAR in Docker Compose)

### 포함 모듈

| 모듈 | 역할 | 컴포넌트 ID |
|------|------|------------|
| auth | 회원가입, 로그인, JWT 발급/검증 | BC-01 |
| room | 방 CRUD, 상태 전이, 탐색/검색 | BC-02 |
| order | 주문 항목 관리, 배달비 계산, 정산 | BC-03 |
| common | 공유 DTO, 예외, 유틸, 상수 | — |

### 주요 클래스

```
backend/src/main/java/com/foodgroup/
├── common/
│   ├── dto/          # 공유 DTO (ApiResponse, PageDto 등)
│   ├── exception/    # GlobalExceptionHandler, BusinessException
│   └── util/         # DateUtils, SecurityUtils
├── auth/
│   ├── controller/AuthController
│   ├── service/AuthService
│   ├── repository/MemberRepository
│   └── domain/Member, RefreshToken
├── room/
│   ├── controller/RoomController
│   ├── service/RoomService, RoomStateValidator (PBT-07)
│   ├── repository/RoomRepository
│   └── domain/Room, RoomStatus
└── order/
    ├── controller/OrderController
    ├── service/OrderService, DeliveryFeeCalculator (PBT-02,03)
    ├── repository/OrderItemRepository
    └── domain/OrderItem, Settlement
```

### 데이터베이스 스키마 책임
- `members` 테이블
- `rooms` 테이블
- `order_items` 테이블
- `settlements` 테이블
- `refresh_tokens` 테이블

### OpenAPI 발행 책임
- Unit 1이 `/docs/openapi.yaml` 초안 작성
- Unit 2, 3가 참조하여 연동

### PBT 적용
- `DeliveryFeeCalculator.calculatePerPersonFee()` — PBT-02, PBT-03
- `RoomStateValidator.isValidTransition()` — PBT-07

### Security Baseline 적용
- SECURITY-08: JWT 인증·인가 전체 적용
- SECURITY-12: bcrypt, Brute-force 방지
- SECURITY-05: `@Valid` 입력 검증 (모든 Controller)
- SECURITY-08: IDOR 방지 (본인 주문만 수정)

---

## Unit 2 — Realtime + Mobile

### 개요
- **스트림**: Stream 2
- **책임**: 실시간 채팅, 푸시 알림, React Native 앱 전체
- **배포**: 채팅 백엔드 — 미니PC Docker Compose / 앱 — App Store/Play Store

### 포함 모듈

| 모듈/피처 | 역할 | 컴포넌트 ID |
|-----------|------|------------|
| chat (백엔드) | WebSocket/STOMP 브로커, 채팅 이력 | BC-04 |
| notification (백엔드) | FCM 푸시 알림 발송 | BC-05 |
| React Native 앱 전체 | features/auth, room, chat, order, ai, map | — |

### 주요 클래스 (백엔드)

```
backend/src/main/java/com/foodgroup/
├── chat/
│   ├── controller/ChatController (WebSocket)
│   ├── service/ChatService
│   ├── repository/ChatMessageRepository
│   └── domain/ChatMessage
└── notification/
    ├── service/NotificationService
    └── config/FcmConfig
```

### React Native 앱 구조

```
mobile/
├── src/
│   ├── features/
│   │   ├── auth/       # 로그인, 회원가입
│   │   ├── room/       # 방 목록, 생성, 상세
│   │   ├── chat/       # 실시간 채팅
│   │   ├── order/      # 주문 입력, 정산
│   │   ├── ai/         # AI 추천 요청·결과
│   │   └── map/        # 카카오맵 SDK
│   ├── shared/         # 공통 컴포넌트, hooks
│   └── navigation/     # React Navigation 설정
```

### WebSocket 메시지 스키마 책임
- Unit 2가 STOMP 메시지 포맷 정의
- Unit 1이 참조하여 STOMP 브로커 구현

### 의존성
- Unit 1의 OpenAPI 스펙 참조하여 앱 API 연동
- FCM 토큰은 auth 모듈과 연계

---

## Unit 3 — AI + Infrastructure

### 개요
- **스트림**: Stream 3
- **책임**: AI 추천 Lambda, 지도 서버사이드, AWS IaC, CI/CD
- **배포**: Lambda — AWS / IaC — Terraform or CDK

### 포함 모듈

| 모듈 | 역할 | 컴포넌트 ID |
|------|------|------------|
| Lambda (AI) | RuleEngine + Gemini 설명 텍스트 생성 | AC-01 |
| API Gateway | Lambda 진입점, rate limiting | AC-03 |
| S3 | 이미지 저장, DB 백업 | AC-02 |
| CloudFront | CDN, 미니PC IP 은닉, DDoS 방어 | AC-04 |
| map (백엔드) | Kakao REST API 서버사이드 호출 | BC-06 |
| ai-client (백엔드) | Lambda 호출 클라이언트 | BC-07 |
| CI/CD | GitHub Actions, 보안 스캐너 | — |

### Lambda 구조

```
functions/
├── ai-recommend/
│   ├── handler.py (또는 handler.js)
│   ├── rule_engine.py    # RuleEngine (PBT-08, PBT-09)
│   ├── gemini_client.py  # Gemini API 호출
│   └── requirements.txt
```

### 백엔드 모듈

```
backend/src/main/java/com/foodgroup/
├── map/
│   ├── service/MapService (PBT-09 후보)
│   └── client/KakaoMapClient
└── ai/
    ├── service/AiRecommendService
    └── client/LambdaClient
```

### Lambda 인터페이스 책임
- Unit 3이 Lambda 요청/응답 스키마 정의
- Unit 1(ai-client)이 참조하여 구현

### PBT 적용
- `RuleEngine.filterAndRank()` — PBT-08, PBT-09

### Security Baseline 적용
- SECURITY-01: TLS 전송, S3 암호화
- SECURITY-03: 구조화 로깅, 민감정보 마스킹
- SECURITY-15: Fail-closed, 에러 시 민감정보 미노출

### CI/CD 파이프라인
- Semgrep, Grype, Gitleaks, Checkov, ClamAV 스캐너 포함
- HIGH/CRITICAL 이슈 머지 차단

---

## 모노레포 디렉토리 구조

```
ACC_1/                          # 모노레포 루트
├── backend/                    # Spring Boot 모놀리스 (Unit 1, 2, 3 공유)
│   ├── src/main/java/com/foodgroup/
│   │   ├── common/
│   │   ├── auth/
│   │   ├── room/
│   │   ├── order/
│   │   ├── chat/
│   │   ├── notification/
│   │   ├── map/
│   │   └── ai/
│   └── build.gradle
├── mobile/                     # React Native 앱 (Unit 2)
│   ├── src/
│   └── package.json
├── functions/                  # AWS Lambda (Unit 3)
│   └── ai-recommend/
├── infrastructure/             # IaC, Docker Compose (Unit 3)
│   ├── docker-compose.yml
│   └── terraform/ (또는 cdk/)
├── docs/
│   └── openapi.yaml            # Unit 1 발행, 전체 참조
└── aidlc-docs/
```

---

## Docker Compose 구성 (미니PC)

```yaml
# infrastructure/docker-compose.yml
services:
  app:
    build: ../backend
    ports: ["8080:8080"]
    depends_on: [postgres, redis]

  postgres:
    image: postgres:16
    volumes: [pgdata:/var/lib/postgresql/data]

  redis:
    image: redis:7-alpine
    volumes: [redisdata:/data]

volumes:
  pgdata:
  redisdata:
```

---

## 브랜치 전략 (유닛 브랜치)

```
main
├── unit/core-backend      # Unit 1 — Stream 1 작업
├── unit/realtime-mobile   # Unit 2 — Stream 2 작업
└── unit/ai-infra          # Unit 3 — Stream 3 작업
```

- 각 유닛 브랜치에서 개발 완료 후 main에 PR
- 1주 개발이므로 장기 브랜치 머지 충돌 위험 낮음
