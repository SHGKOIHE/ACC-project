# Unit of Work — Dependency Matrix
# 배달비 절약을 위한 음식 공동 구매 앱

**작성일**: 2026-05-24
**단계**: INCEPTION — Units Generation

---

## 의존성 매트릭스

| 소비자 → | Unit 1 (Core) | Unit 2 (Realtime+Mobile) | Unit 3 (AI+Infra) |
|----------|:-------------:|:------------------------:|:-----------------:|
| **Unit 1** | — | STOMP 메시지 스키마 수신 | Lambda 인터페이스 수신 |
| **Unit 2** | OpenAPI 스펙 수신 | — | — |
| **Unit 3** | — | — | — |

- **Unit 3**: 외부 의존성 없음 — 가장 먼저 독립 진행 가능
- **Unit 1**: Unit 3의 Lambda 인터페이스 확정 후 ai-client 구현
- **Unit 2**: Unit 1의 OpenAPI 발행 후 앱 실제 연동

---

## 개발 순서

```
Day 1~2
├── Unit 1: DB 스키마 설계, auth/room/order 핵심 API 개발
├── Unit 3: Lambda 인터페이스 정의, AWS IaC 초안, Docker Compose 작성
└── [인터페이스 확정 게이트]
    ├── Unit 1 → OpenAPI 초안 발행 (auth, room, order 엔드포인트)
    └── Unit 3 → Lambda 요청/응답 스키마 확정

Day 3~4
├── Unit 1: OpenAPI 기반 나머지 API 완성, ai-client 구현, PBT 작성
├── Unit 2: OpenAPI 참조하여 React Native 앱 개발 시작
└── Unit 3: Lambda RuleEngine + Gemini 연동, CI/CD 파이프라인

Day 5~6
├── Unit 1 & 2: 앱-백엔드 연동 테스트
├── Unit 3: 보안 스캐너 통합, 배포 검증
└── 통합 테스트

Day 7
└── 빌드·배포, 최종 검증
```

---

## 인터페이스 계약 (Interface Contract)

### IC-01: OpenAPI 스펙 (Unit 1 → Unit 2)
- **발행자**: Unit 1 (Stream 1)
- **소비자**: Unit 2 (Stream 2 — React Native 앱)
- **형식**: `docs/openapi.yaml` (springdoc-openapi 자동생성)
- **확정 시점**: Day 2 완료 시
- **포함 엔드포인트**:
  - `POST /api/auth/signup`, `POST /api/auth/login`
  - `GET/POST /api/rooms`, `POST /api/rooms/{id}/join`
  - `POST /api/rooms/{id}/orders`, `GET /api/rooms/{id}/settlement`

### IC-02: Lambda 인터페이스 (Unit 3 → Unit 1)
- **발행자**: Unit 3 (Stream 3)
- **소비자**: Unit 1 (Stream 1 — ai-client 모듈)
- **형식**: JSON 요청/응답 스키마 (`functions/ai-recommend/schema.json`)
- **확정 시점**: Day 2 완료 시
- **요청 예시**:
  ```json
  {
    "location": { "lat": 37.5, "lng": 127.0 },
    "filters": { "category": "한식", "maxPrice": 15000 },
    "memberCount": 4
  }
  ```
- **응답 예시**:
  ```json
  {
    "recommendations": [
      { "rank": 1, "restaurantId": "...", "explanation": "..." }
    ]
  }
  ```

### IC-03: STOMP 메시지 스키마 (Unit 2 → Unit 1)
- **발행자**: Unit 2 (Stream 2)
- **소비자**: Unit 1 (Stream 1 — STOMP 브로커 설정)
- **형식**: STOMP destination 패턴 문서 (`docs/stomp-schema.md`)
- **확정 시점**: Day 2 완료 시
- **패턴**:
  - Subscribe: `/topic/room/{roomId}/chat`
  - Publish: `/app/room/{roomId}/chat`

---

## 통신 패턴

| 연결 | 프로토콜 | 방향 |
|------|----------|------|
| React Native → Spring Boot | HTTPS REST | Unit 2 → Unit 1 |
| React Native → Spring Boot | WebSocket/STOMP | Unit 2 ↔ Unit 1 |
| Spring Boot → Lambda | HTTPS (API Gateway) | Unit 1 → Unit 3 |
| Spring Boot → Kakao REST API | HTTPS | Unit 1 (BC-06) → 외부 |
| Spring Boot → FCM | HTTPS | Unit 1 (BC-05) → 외부 |
| Spring Boot → Redis | TCP | Unit 1 내부 |
| Spring Boot → PostgreSQL | TCP | Unit 1 내부 |
| CloudFront → Spring Boot | HTTPS | 외부 → Unit 1 |

---

## 의존성 다이어그램

```
                    ┌─────────────────────────────────┐
                    │         Unit 3 (AI+Infra)        │
                    │  Lambda · API Gateway · S3       │
                    │  CloudFront · IaC · CI/CD        │
                    └──────────────┬──────────────────┘
                                   │ IC-02 Lambda 인터페이스
                                   ▼
┌──────────────────┐    ┌─────────────────────────────────┐
│  Unit 2          │    │         Unit 1 (Core Backend)    │
│  Realtime+Mobile │    │  auth · room · order · common   │
│  chat · notif    │◀───│  ai-client · map                │
│  React Native    │    │  PostgreSQL · Redis              │
└──────────────────┘    └─────────────────────────────────┘
        │  IC-01 OpenAPI 스펙
        └─────────────────────────────────────────────────▶ docs/openapi.yaml
        │  IC-03 STOMP 스키마
        └─────────────────────────────────────────────────▶ docs/stomp-schema.md
```

---

## 위험 요소 및 완화 전략

| 위험 | 영향 | 완화 |
|------|------|------|
| Unit 1 OpenAPI 발행 지연 | Unit 2 앱 개발 착수 지연 | Day 2까지 auth/room 엔드포인트 우선 발행 |
| Lambda 인터페이스 변경 | Unit 1 ai-client 재구현 | IC-02 스키마 버전 고정, 변경 시 Unit 1에 즉시 공지 |
| STOMP 스키마 변경 | 채팅 연동 재작업 | IC-03 확정 전 Unit 1과 합의 필수 |
| Docker Compose 환경 차이 | 개발/운영 불일치 | `.env.example` 제공, README에 실행 가이드 |
