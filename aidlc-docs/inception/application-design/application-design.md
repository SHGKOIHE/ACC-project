# Application Design
# 배달비 절약을 위한 음식 공동 구매 앱

**작성일**: 2026-05-24  
**단계**: INCEPTION — Application Design  
**상태**: 완료

---

## 1. 설계 결정 요약

| 항목 | 결정 | 근거 |
|------|------|------|
| 백엔드 아키텍처 | 계층형 (Controller → Service → Repository) | Spring Boot 표준, 빠른 개발 |
| 시스템 구성 | 미니PC 모놀리스 + AWS 보조 서비스 | MVP 3인팀, 운영 복잡도 최소화 |
| AI 파이프라인 | 규칙 엔진(순위) + Gemini(설명 텍스트) | PBT 적용 용이, 비용 절감, fallback 가능 |
| 배포 방식 | AWS + 온프레미스 하이브리드 | 미니PC 보유, AWS 최소 비용 |
| AWS 서비스 (4개) | CloudFront, API Gateway, Lambda, S3 | 각각 명확한 역할 분리 |
| 아키텍처 다이어그램 | Mermaid AWS 다이어그램 | 팀 공유 용이 |
| 프론트엔드 구조 | Feature-based (features/{feature}/) | 도메인별 응집성 |
| 데이터베이스 | PostgreSQL + Redis (미니PC 로컬) | 채팅 pub/sub, 세션, 캐시 |
| 지도 | 카카오맵 SDK (모바일) + Kakao REST API (서버) | 한국 서비스 최적화 |

---

## 2. 전체 아키텍처

```
                        ┌──────────────────────────────────────────┐
                        │              ☁️ AWS                       │
  📱 React Native       │  ┌──────────┐  ┌──────────┐  ┌────────┐ │
  (iOS / Android)  ────▶│  │CloudFront│  │    S3    │  │  API   │ │
                        │  │CDN/DDoS  │  │이미지/백업│  │Gateway │ │
                        │  └────┬─────┘  └──────────┘  └───┬────┘ │
                        └───────┼───────────────────────────┼──────┘
                                │ HTTPS                     │
                    ┌───────────▼──────────────┐    ┌───────▼───┐
                    │   🖥️ 미니PC (온프레미스)   │    │  Lambda   │
                    │                           │    │ RuleEngine│
                    │  Spring Boot Monolith     │    │ + Gemini  │
                    │  ┌────┬────┬────┬──────┐  │    └───────────┘
                    │  │auth│room│ord │chat  │  │
                    │  ├────┼────┼────┼──────┤  │    🤖 Gemini API
                    │  │map │ ai │ntf │admin │  │    🔔 FCM
                    │  └────┴────┴────┴──────┘  │    🗺️ Kakao REST API
                    │                           │
                    │  PostgreSQL    Redis       │
                    └───────────────────────────┘
```

---

## 3. 컴포넌트 목록

### 백엔드 (Spring Boot 모놀리스)

| ID | 모듈 | 핵심 기능 | 관련 FR |
|----|------|----------|---------|
| BC-01 | auth | 회원가입, 로그인, JWT | FR-01 |
| BC-02 | room | 방 CRUD, 상태 관리, 탐색 | FR-02, FR-03, FR-09 |
| BC-03 | order | 주문 항목, 배달비 계산, 정산 | FR-05, FR-06 |
| BC-04 | chat | WebSocket/STOMP, 채팅 이력 | FR-04 |
| BC-05 | notification | FCM 푸시 알림 | FR-07 |
| BC-06 | map | Kakao REST API (서버 사이드) | FR-03 |
| BC-07 | ai (클라이언트) | Lambda 호출, 추천 결과 반환 | FR-10 |
| BC-08 | admin | 운영 관리 | EP-10 |

### AWS

| ID | 서비스 | 역할 |
|----|--------|------|
| AC-01 | Lambda | AI 추천 (RuleEngine + Gemini 설명) |
| AC-02 | S3 | 이미지 저장, PG 백업 |
| AC-03 | API Gateway | Lambda 진입점, rate limiting |
| AC-04 | CloudFront | CDN, 미니PC IP 은닉, DDoS 방어 |

### 프론트엔드 (React Native)

| 피처 | 담당 화면/기능 |
|------|--------------|
| auth | 회원가입, 로그인 |
| room | 방 목록, 방 생성, 방 상세 |
| chat | 실시간 채팅 |
| order | 주문 입력, 정산 확인 |
| ai | AI 추천 요청·결과 |
| map | 지도 탐색 (Kakao Map SDK) |
| admin | 관리자 대시보드 |

---

## 4. PBT 적용 대상 (jqwik)

| 클래스 | 위치 | 적용 PBT 규칙 |
|--------|------|--------------|
| `DeliveryFeeCalculator` | BC-03 | PBT-02, PBT-03 |
| `RoomStateValidator` | BC-02 | PBT-07 |
| `RuleEngine` | AC-01 (Lambda) | PBT-08, PBT-09 |
| `MapService.calculateDistance` | BC-06 | PBT-09 후보 |

---

## 5. 보안 적용 포인트 (Security Baseline)

| 위치 | 적용 규칙 |
|------|----------|
| `SecurityConfig` (auth) | SECURITY-08: 인증·인가 전체 적용 |
| `AuthService` | SECURITY-12: JWT, bcrypt, Brute-force 방지 |
| 모든 Controller | SECURITY-05: 입력 검증 (`@Valid`) |
| `OrderService` | SECURITY-08: IDOR 방지 (본인 주문만 수정) |
| PostgreSQL, Redis | SECURITY-01: TLS in transit, at-rest 암호화 |
| 전역 `@ExceptionHandler` | SECURITY-15: Fail-closed, 민감정보 미노출 |
| 로깅 설정 | SECURITY-03: 구조화된 로깅, 민감정보 마스킹 |

---

## 6. 3인 개발 스트림 배분

| 스트림 | 담당 범위 |
|--------|----------|
| **Stream 1 — Core Backend** | BC-01(auth), BC-02(room), BC-03(order), BC-08(admin), PostgreSQL 스키마, Spring Security |
| **Stream 2 — Realtime + Mobile** | BC-04(chat), BC-05(notification), React Native 전체 피처, FCM, Kakao Map SDK |
| **Stream 3 — AI + Infra** | AC-01(Lambda), BC-07(ai 클라이언트), BC-06(map REST), AWS IaC, CI/CD + 보안 스캐너 |

**의존성 순서**:
1. Stream 1이 OpenAPI 스펙 발행 → Stream 2 모바일 연동 시작
2. Stream 3 Lambda 인터페이스 확정 → Stream 1 AI 클라이언트 구현
3. Stream 2 WebSocket 메시지 스키마 확정 → Stream 1 STOMP 브로커 구현

---

## 7. 산출물 목록

| 파일 | 내용 |
|------|------|
| `components.md` | 컴포넌트 정의, 책임, 클래스 구성 |
| `component-methods.md` | 메서드 시그니처, PBT 속성 후보 |
| `services.md` | 서비스 오케스트레이션, 에러 처리 |
| `component-dependency.md` | 의존성 매트릭스, 통신 패턴, Mermaid 다이어그램 |
| `application-design.md` | 본 문서 (통합 설계 요약) |
