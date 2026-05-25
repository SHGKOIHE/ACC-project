# NFR Requirements — Unit 1: Core Backend
# 배달비 절약을 위한 음식 공동 구매 앱

**작성일**: 2026-05-24
**단계**: CONSTRUCTION — NFR Requirements

---

## NFR-PERF: 성능 요구사항

### NFR-PERF-01: API 응답 시간
- 주요 API (방 목록, 주문 확정, 정산 조회): **1초 이내** (p95 기준)
- 단순 조회 API (방 상세, 내 정보): **500ms 이내** 목표
- 자동 마감 스케줄러: 1분 주기, 실행 시간 5초 이내

### NFR-PERF-02: 동시 사용자
- 목표 동시 접속자: **100~300명**
- 미니PC(Ryzen 7 PRO 6850H, 24GB RAM) 기준 충분히 처리 가능
- Spring Boot 기본 스레드풀(200) + HikariCP 커넥션풀(10~20)로 대응
- 병목 발생 시 커넥션풀 조정 우선 (코드 변경 없이 설정으로 해결)

### NFR-PERF-03: DB 쿼리 최적화
- 방 목록 조회: 복합 인덱스 `(status, created_at)` 적용
- 방 탐색 필터: `(status, restaurant_category, meeting_type)` 복합 인덱스
- N+1 쿼리 방지: fetch join 또는 별도 쿼리로 처리

---

## NFR-AVAIL: 가용성 요구사항

### NFR-AVAIL-01: 자동 재시작
- Docker Compose `restart: unless-stopped` 설정
- 컨테이너 비정상 종료 시 자동 재시작
- 미니PC 재부팅 시 Docker 서비스 자동 시작 (`systemctl enable docker`)

### NFR-AVAIL-02: 헬스체크
- Spring Boot Actuator `/actuator/health` 엔드포인트 활성화
- Docker Compose healthcheck 설정 (30초 간격, 3회 실패 시 재시작)

### NFR-AVAIL-03: 목표 가용성
- MVP 목표: **99% 이상** (월 다운타임 7시간 이하)
- 학교 주변 서비스 특성상 점심/저녁 피크타임 집중 대응

---

## NFR-SEC: 보안 요구사항 (Security Baseline SECURITY-01~15)

### NFR-SEC-01: 전송 보안 (SECURITY-01)
- CloudFront → 미니PC 구간: HTTPS (TLS 1.2+)
- PostgreSQL, Redis: 로컬 루프백 통신 (외부 노출 없음)

### NFR-SEC-02: 인증·인가 (SECURITY-08)
- 디바이스 토큰 (`X-Device-Token`) 헤더 검증
- 모든 보호 API에 `@AuthenticationPrincipal` 적용
- IDOR 방지: 본인 소유 리소스만 수정/삭제 가능

### NFR-SEC-03: 입력 검증 (SECURITY-05)
- 모든 Controller에 `@Valid` + Bean Validation 적용
- 서버사이드 검증 필수 (클라이언트 검증만 믿지 않음)

### NFR-SEC-04: 데이터 암호화 (SECURITY-01)
- 계좌번호: AES-256-GCM 암호화 저장
- 암호화 키: 환경변수 (`ENCRYPTION_KEY`) 관리, 코드 하드코딩 금지

### NFR-SEC-05: 로깅 보안 (SECURITY-03)
- 계좌번호, 디바이스 토큰 로그 마스킹
- 구조화 로깅 (JSON 포맷, Logback)

### NFR-SEC-06: 에러 처리 (SECURITY-15)
- 전역 `@ExceptionHandler`: 내부 스택트레이스 미노출
- 에러 응답: `{code, message}` 형식만 반환

### NFR-SEC-07: 보안 스캐너 (CI/CD)
- Semgrep, Grype, Gitleaks, Checkov 파이프라인 통합
- HIGH/CRITICAL 이슈 머지 차단

---

## NFR-DATA: 데이터 요구사항

### NFR-DATA-01: 보존 기간
- 모든 데이터 **영구 보관**
- 주문·정산 데이터: 분쟁 대응 근거로 삭제 금지
- 학교 주변 규모(300명 이하) — 수년치 데이터도 수백 MB 예상

### NFR-DATA-02: 백업
- PostgreSQL 일일 백업 → S3 (Unit 3 CI/CD에서 구현)
- 백업 보존: 30일

---

## NFR-OBS: 관찰가능성 요구사항

### NFR-OBS-01: 로깅
- 레벨: **INFO** (비즈니스 이벤트 + 에러)
- 포맷: JSON 구조화 로깅 (timestamp, level, traceId, message)
- 주요 이벤트 로깅: 방 생성, 방 참여, 주문 확정, 방 취소, 자동 마감
- 로그 파일: 일별 롤링, 30일 보관

### NFR-OBS-02: 헬스 모니터링
- Spring Boot Actuator: `/health`, `/metrics`, `/info`
- 외부 노출 제한: 미니PC 로컬만 접근 허용
