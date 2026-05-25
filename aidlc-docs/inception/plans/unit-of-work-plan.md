# Unit of Work Plan
# 배달비 절약을 위한 음식 공동 구매 앱

## 현재 확정된 아키텍처 요약

- **배포**: 미니PC(Spring Boot 모놀리스 + PostgreSQL + Redis) + AWS(S3, CloudFront, API Gateway, Lambda)
- **백엔드 모듈**: auth, room, order, chat, notification, map, ai-client, admin
- **프론트엔드**: React Native (features/auth, room, chat, order, ai, map, admin)
- **AI**: Lambda (RuleEngine + Gemini 설명) — MVP 이후 Ollama 교체 검토
- **팀**: 3인 개발팀

아래 질문에 각 `[Answer]:` 태그 뒤에 답변 후 **"완료"** 라고 알려주세요.

---

## Part 1: 스토리 그룹핑

### Q1: 유닛 분리 기준
백엔드 모듈 8개와 React Native 앱을 어떻게 유닛으로 묶을까요?

A) **3유닛 — 팀 스트림 기준**
- Unit 1: 백엔드 Core (auth·room·order·admin) + PostgreSQL 스키마
- Unit 2: Realtime + Mobile (chat·notification 백엔드 + React Native 전체)
- Unit 3: AI Service (Lambda·map·ai-client) + AWS 인프라 + CI/CD

B) **2유닛 — 프론트/백 분리**
- Unit 1: 백엔드 전체 (모놀리스 8모듈 + Lambda)
- Unit 2: React Native 앱 전체

C) **4유닛 — 도메인 분리**
- Unit 1: 인증/회원 (auth)
- Unit 2: 공동구매 코어 (room·order·map)
- Unit 3: 소통 (chat·notification)
- Unit 4: AI + 인프라 + React Native

**[Answer]: A**

---

### Q2: Lambda AI 서비스 위치
AWS Lambda AI 추천 서비스는 어느 유닛에 속할까요?

A) 별도 독립 유닛 — Lambda 코드베이스는 Spring Boot와 완전 분리된 독립 유닛
B) 인프라 유닛에 포함 — AWS IaC, CI/CD와 같은 유닛으로 묶음
C) 백엔드 유닛에 포함 — ai-client 모듈과 함께 백엔드 유닛에서 관리

**[Answer]: A**

---

## Part 2: 의존성 및 개발 순서

### Q3: 유닛 간 개발 순서
어떤 순서로 개발할까요?

A) **백엔드 우선** — 백엔드 API 완성 후 프론트엔드 연동 (API 계약 먼저 확정)
B) **병렬 개발** — 3팀이 동시에 시작, OpenAPI 스펙으로 Mock 서버 사용
C) **기능 슬라이스** — 회원가입→방개설→채팅 순으로 기능 단위 수직 개발

**[Answer]: A**

---

### Q4: 공유 코드 전략
유닛 간 공통으로 쓰이는 DTO, 예외 클래스, 유틸은 어떻게 관리할까요?

A) **공통 모듈 분리** — `common/` 패키지를 별도로 두고 모든 유닛이 참조
B) **각 유닛에 복사** — 유닛별로 필요한 코드를 독립적으로 유지
C) **백엔드 유닛 소유** — 공통 코드는 백엔드 Core 유닛에 두고 다른 유닛이 참조

**[Answer]: A**

---

## Part 3: 팀 정렬

### Q5: 코드 저장소 구성
코드는 어떻게 관리할까요?

A) **모노레포** — 하나의 저장소에 backend/, mobile/, infrastructure/ 폴더로 구성
B) **멀티레포** — backend, mobile, infrastructure 각각 별도 저장소
C) **2개 저장소** — backend+infrastructure 통합 / mobile 분리

**[Answer]: A**

---

### Q6: 유닛별 브랜치 전략
각 유닛 개발자가 같은 저장소에서 작업할 때 브랜치를 어떻게 운영할까요?

A) **feature 브랜치** — `feature/unit1-auth`, `feature/unit2-chat` 형식으로 분리
B) **유닛 브랜치** — `unit/core-backend`, `unit/mobile` 장기 브랜치 운영
C) **트렁크 기반** — main 브랜치에 짧은 feature 브랜치로 자주 병합

**[Answer]: B**

---

## Part 4: 기술 고려사항

### Q7: API 계약 관리
백엔드와 프론트엔드 간 API 명세를 어떻게 공유할까요?

A) **OpenAPI(Swagger)** — Spring Boot에서 자동 생성, 프론트엔드가 참조
B) **별도 API 문서** — `docs/api-spec.md` 수동 관리
C) **MSW(Mock Service Worker)** — 프론트엔드에서 Mock 서버로 독립 개발 후 연동

**[Answer]: A**

---

### Q8: 배포 단위
각 유닛의 배포 방식은?

A) **미니PC 단일 배포** — 모든 백엔드 모듈을 하나의 JAR로 빌드·배포
B) **모듈별 독립 JAR** — 각 유닛을 별도 JAR로 빌드, 미니PC에서 별도 프로세스 실행
C) **Docker Compose** — 미니PC에서 Docker Compose로 서비스 컨테이너화

**[Answer]: C**

---

## Part 5: 비즈니스 도메인

### Q9: MVP 우선 유닛
어떤 유닛을 먼저 완성해야 MVP 데모가 가능한가요? (개발 우선순위 기준)

A) **Unit 1 우선** — 회원가입·방 개설·주문이 되어야 나머지 의미 있음
B) **모든 유닛 동시** — MVP는 통합 기능이므로 병렬로 진행 후 통합
C) **채팅 우선** — 실시간 채팅이 핵심 차별점이므로 먼저 검증

**[Answer]: A**

---

### Q10: 관리자 기능 포함 범위
admin 모듈은 MVP에 포함되나요?

A) MVP 포함 — 운영 관리가 필요하므로 첫 릴리즈에 포함
B) MVP 제외 — 초기에는 DB 직접 조회로 운영, 이후 추가
C) 최소 포함 — 사용자 정지 기능만 MVP에 포함

**[Answer]: B**

---

## Generation Checklist (답변 후 실행)

- [x] Step 1: unit-of-work.md 생성 (유닛 정의·책임·포함 모듈)
- [x] Step 2: unit-of-work-dependency.md 생성 (의존성 매트릭스·개발 순서)
- [x] Step 3: unit-of-work-story-map.md 생성 (EP→유닛 매핑)
