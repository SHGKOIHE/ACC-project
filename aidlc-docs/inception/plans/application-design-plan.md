# Application Design Plan
# 배달비 절약을 위한 음식 공동 구매 앱

아래 질문에 각 [Answer]: 태그 뒤에 해당 알파벳을 입력해주세요.
모든 질문에 답변하신 후 "완료"라고 알려주세요.

---

## Part 1: 백엔드 아키텍처 스타일

## Question 1
Spring Boot 백엔드의 아키텍처 스타일은 어떻게 할까요?

A) 계층형 아키텍처 (Layered) — Controller → Service → Repository. 단순하고 빠른 개발, Spring Boot 표준 패턴

B) 클린 아키텍처 (Clean Architecture) — Domain / Application / Infrastructure / Presentation 레이어 분리. 테스트 용이, 의존성 방향 명확, 복잡도 높음

C) 헥사고날 아키텍처 (Hexagonal / Ports & Adapters) — 도메인 중심, 외부 어댑터 분리. 유연성 최대, 학습 곡선 있음

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Part 2: 프론트엔드 앱 구조

## Question 2
React Native 앱의 폴더/컴포넌트 구조는 어떻게 할까요?

A) Feature-based — 기능별 폴더 (`features/auth/`, `features/room/`, `features/chat/`)

B) Screen-based — 화면별 폴더 (`screens/HomeScreen/`, `screens/RoomScreen/`)

C) Domain-based — 도메인 엔티티 중심 (`domain/room/`, `domain/user/`, `domain/order/`)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Part 3: AI 음식 추천 서비스

## Question 3
AI 음식 추천 기능에 어떤 AI 서비스를 사용할까요?

A) Claude API (Anthropic) — 프롬프트 기반, 한국어 강점, 유료

B) OpenAI GPT API — 프롬프트 기반, 범용, 유료

C) 규칙 기반 추천 — AI 없이 카테고리/위치/평점 필터링으로 구현 (무료, 빠른 구현)

D) Gemini API (Google) — Google Maps와 연동 시너지, 유료

X) Other (please describe after [Answer]: tag below)

[Answer]: X
C로 필터링 후 D 적용

---

## Part 4: 지도 서비스 최종 결정

## Question 4
지도/위치 서비스를 최종 결정해주세요.

A) 카카오맵 SDK — 한국 서비스 최적화, 국내 POI 풍부, 한글 주소 지원

B) Google Maps SDK — 글로벌 표준, React Native 라이브러리 풍부

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Part 5: 데이터베이스 구성

## Question 5
데이터베이스 구성은 어떻게 할까요?

A) PostgreSQL 단독 — 관계형 DB로 모든 데이터 관리, 단순 구성

B) PostgreSQL + Redis — PostgreSQL 메인 DB + Redis 세션/캐싱/채팅 임시 저장

C) PostgreSQL + Redis + MongoDB — 채팅 이력을 MongoDB에 별도 저장 (대용량 고려)

X) Other (please describe after [Answer]: tag below)

[Answer]: 

---

## Part 6: 컴포넌트 경계

## Question 6
백엔드에서 AI 추천 서비스와 지도 서비스를 어떻게 통합할까요?

A) 별도 마이크로서비스 — AI 추천과 지도 연동을 독립 서비스로 분리 (확장성 높음, 복잡도 증가)

B) 단일 Spring Boot 앱 내 컴포넌트 — 동일 프로세스 내 서비스 클래스로 구현 (MVP에 적합)

C) API 게이트웨이 패턴 — 외부 API 호출을 게이트웨이 레이어로 추상화

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Application Design Generation Checklist

아래는 답변 확인 후 실행할 생성 단계입니다.

- [x] Step 1: 컴포넌트 목록 및 책임 정의 (`components.md`)
- [x] Step 2: 컴포넌트 메서드 시그니처 정의 (`component-methods.md`)
- [x] Step 3: 서비스 레이어 설계 (`services.md`)
- [x] Step 4: 컴포넌트 의존성 매핑 (`component-dependency.md`)
- [x] Step 5: 통합 설계 문서 생성 (`application-design.md`)
