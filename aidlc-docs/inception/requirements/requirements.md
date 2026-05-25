# Requirements Document
# 배달비 절약을 위한 음식 공동 구매 앱

---

## Intent Analysis

| 항목 | 내용 |
|------|------|
| **사용자 요청** | 배달비 절약을 위한 음식 공동 구매 앱 |
| **요청 유형** | New Project (Greenfield) |
| **범위 추정** | System-wide — 모바일 앱 + 백엔드 서버 + 실시간 통신 + 위치 서비스 |
| **복잡도 추정** | Complex — 실시간 채팅, 위치 기반 탐색, 다중 사용자 상태 동기화 포함 |
| **MVP 범위** | 회원 관리, 방 개설/참여, 주문 확인 (결제 제외) |

---

## 기술 스택 결정

| 레이어 | 기술 |
|--------|------|
| **모바일 앱** | React Native (iOS / Android 크로스플랫폼) |
| **백엔드 API** | Java 17+ / Spring Boot 3.x (REST API) |
| **실시간 채팅** | WebSocket (Spring WebSocket / STOMP 프로토콜) |
| **푸시 알림** | Firebase Cloud Messaging (FCM) |
| **지도/위치** | 카카오맵 SDK 또는 Google Maps SDK |
| **데이터베이스** | PostgreSQL (TBD — 추후 Functional Design에서 확정) |
| **PBT 프레임워크** | jqwik (JUnit 5 통합, Java) |

---

## 기능 요구사항 (Functional Requirements)

### FR-01: 회원 관리
- 이메일/비밀번호 회원가입 및 로그인
- JWT 기반 인증 (Access Token + Refresh Token)
- 프로필 관리 (닉네임, 배달 주소 기본값)

### FR-02: 공동 구매 방 개설
- 방 생성 시 입력: 식당 이름, 음식 카테고리, 배달 주소, 최대 참여 인원, 마감 시간
- 방 상태 관리: 모집중 → 주문완료 → 배달중 → 완료 / 취소
- 배달비 자동 계산 및 참여자 수 기반 1/n 분배 금액 표시

### FR-03: 방 탐색 및 참여
- 지도 기반 주변 방 탐색 (카카오맵 / Google Maps)
- 카테고리/거리/마감시간 필터링
- 방 상세 조회 및 참여 신청

### FR-04: 실시간 채팅
- 방 내 참여자 간 실시간 채팅 (WebSocket / STOMP)
- 시스템 메시지: 참여자 입/퇴장, 주문 상태 변경 알림

### FR-05: 개인 주문 항목 관리
- 방 참여 후 개인 메뉴 및 수량 입력
- 참여자 전체 주문 목록 조회
- 내 주문 수정/삭제 (마감 전까지)

### FR-06: 주문 진행 및 상태 추적
- 방장이 전체 주문 확정 후 주문 완료 처리
- 배달 상태 업데이트 (배달중, 완료)
- 참여자별 정산 금액 최종 확인

### FR-07: 푸시 알림
- 방 마감 임박 알림
- 주문 상태 변경 알림 (주문완료, 배달중, 완료)
- FCM 기반 모바일 푸시

### FR-09: 방 유형 선택
- 방 개설 시 유형 선택: `공동 배달` / `배달+모여먹기` / `직접 외식`
- 유형별 추가 입력 필드 표시 (모일 장소, 출발 시간 등)
- 지도 탐색 화면에서 유형별 아이콘 구분

### FR-10: AI 음식 추천 (MVP 포함)
- 메뉴 미결정 사용자 대상: 선호 카테고리, 가격대, 위치 입력 → AI 추천 결과 제공
- 추천 결과에서 관련 방 탐색 또는 방 개설로 연결
- 참여자 페르소나 내 조건 분기로 구현 (별도 페르소나 아님)

### FR-08: 결제 (범위 외)
- 이번 MVP에서 결제 기능은 포함하지 않음
- 정산은 앱 외부에서 처리 (현금, 계좌이체 등)

---

## 비기능 요구사항 (Non-Functional Requirements)

### NFR-01: 성능
- API 응답 시간: 일반 요청 200ms 이내 (P95 기준)
- 실시간 채팅 메시지 전달 지연: 500ms 이내
- 동시 접속 사용자: MVP 기준 최소 100명 지원

### NFR-02: 보안 (Security Baseline 전체 적용 — blocking)
- SECURITY-01: DB 암호화 (at rest + TLS in transit)
- SECURITY-03: 구조화된 로깅 (민감정보 제외)
- SECURITY-05: 모든 API 파라미터 입력 검증
- SECURITY-08: 인증/인가 미들웨어 전체 적용, IDOR 방지
- SECURITY-12: JWT 인증, bcrypt 비밀번호 해싱, Brute-force 방지
- SECURITY-15: Fail-closed 예외 처리, 전역 에러 핸들러
- (전체 SECURITY-01 ~ SECURITY-15 규칙 적용)

### NFR-03: 테스트 (PBT Partial 적용)
- 적용 규칙: PBT-02, PBT-03, PBT-07, PBT-08, PBT-09
- 대상: 배달비 분배 계산 로직, 금액 합산, 방 상태 전이 순수 함수
- 프레임워크: jqwik (Java, JUnit 5)

### NFR-04: 유지보수성
- 계층화된 아키텍처 (Controller → Service → Repository)
- Conventional Commits 형식 준수
- markdownlint 규칙 준수 (AGENT.md 기준)

### NFR-05: 의존성 관리 (AGENT.md 기준)
- Grype: 의존성 CVE 스캔
- Gitleaks: 시크릿 git 히스토리 스캔
- Semgrep: 멀티언어 SAST
- HIGH/CRITICAL 발견 시 머지 전 해결 필수

---

## 핵심 비즈니스 규칙

1. 방 최대 인원 초과 시 참여 불가
2. 마감 시간 이후 참여 및 주문 수정 불가
3. 방장만 주문 확정 및 상태 변경 가능
4. 배달비 분배 = 총 배달비 ÷ 실제 참여자 수 (소수점 올림 처리)
5. 본인 방에서 본인 주문 항목만 수정 가능 (IDOR 방지)

---

## 확장 설정 (Extension Configuration)

| Extension | Enabled | Mode | Decided At |
|-----------|---------|------|------------|
| Security Baseline | Yes | Full (blocking) | Requirements Analysis |
| Property-Based Testing | Yes | Partial (PBT-02,03,07,08,09 only) | Requirements Analysis |
