# Application Design Clarification Questions

아래 5개 항목에 대해 명확화가 필요합니다.
각 [Answer]: 태그 뒤에 답변 후 "완료"라고 알려주세요.

---

## Issue 1: Q5 미답변 — 데이터베이스 구성

## Clarification Question 1
데이터베이스 구성을 선택해주세요.

A) PostgreSQL 단독

B) PostgreSQL + Redis (세션/캐시)

C) PostgreSQL + Redis + MongoDB (채팅 이력 분리)

X) Other (please describe after [Answer]: tag below)

[Answer]: 

---

## Issue 2: Q1(계층형) vs Q6(마이크로서비스) 모순

Q1에서 **계층형 아키텍처(Layered)**, Q6에서 **별도 마이크로서비스**를 선택하셨습니다.
계층형은 단일 앱 내부 구조, 마이크로서비스는 전체 시스템 분리 방식으로 개념이 다릅니다.

### Clarification Question 2
전체 시스템 구성은 어떻게 하려는 의도인가요?

A) 마이크로서비스 전체 — 각 서비스(Auth, Room, Chat, AI, Map)를 독립 Spring Boot 앱으로 분리, 각 앱 내부는 계층형 구조

B) 모듈러 모놀리스 — 하나의 Spring Boot 앱에 패키지로 기능 분리, 내부는 계층형 구조 (MVP에 적합)

C) 핵심 서비스만 분리 — 메인 앱(Auth/Room/Order)은 모놀리스, AI 추천 서비스만 별도 분리

X) Other (please describe after [Answer]: tag below)

[Answer]: 

---

## Issue 3: Q3 AI 파이프라인 구조 확인

Q3에서 "C로 필터링 후 D 적용 (규칙 기반 → Gemini API)"을 선택하셨습니다.

### Clarification Question 3
AI 추천 파이프라인의 동작 방식을 확인해주세요.

A) 2단계 파이프라인 — ① 위치/카테고리/가격 규칙으로 식당 후보 필터링 → ② Gemini API로 최종 추천 문구/순위 생성

B) 1단계 Gemini 직접 호출 — 규칙 기반은 최소화하고 Gemini에게 전체 추천을 맡김 (위치·카테고리를 프롬프트에 포함)

C) 규칙 기반 우선, Gemini는 설명 생성용 — 추천 결과는 규칙으로, Gemini는 "이 음식을 추천하는 이유" 텍스트만 생성

X) Other (please describe after [Answer]: tag below)

[Answer]: 

---

## Issue 4: AWS 서비스 구성 (신규 요구사항)

AWS 서비스 3개 이상 또는 AWS + 온프레미스 하이브리드가 필요하다고 하셨습니다.

### Clarification Question 4
AWS 구성 방식은 무엇인가요?

A) AWS 전체 — 모든 컴포넌트를 AWS에 배포 (3개 이상 서비스 사용)

B) AWS + 온프레미스 하이브리드 — 일부 서비스는 AWS, 나머지는 로컬/사내 서버

X) Other (please describe after [Answer]: tag below)

[Answer]: 

### Clarification Question 5
사용할 AWS 서비스를 선택해주세요. (복수 선택 — 해당 알파벳 모두 나열)

A) ECS (Elastic Container Service) — Docker 컨테이너로 Spring Boot 앱 실행

B) RDS (PostgreSQL) — 관리형 관계형 데이터베이스

C) ElastiCache (Redis) — 관리형 Redis 캐시/세션

D) S3 — 정적 파일, 이미지 저장

E) API Gateway — REST API 엔드포인트 관리

F) Cognito — 사용자 인증/토큰 관리

G) Lambda — 서버리스 함수 (AI 추천, 알림 트리거 등)

H) CloudFront — CDN (React Native 웹뷰, 정적 자산)

X) Other (please describe after [Answer]: tag below)

[Answer]: 

---

## Issue 5: 아키텍처 다이어그램 형식

### Clarification Question 6
아키텍처 다이어그램 형식은 무엇으로 할까요?

A) AWS 공식 아이콘 기반 텍스트 다이어그램 (Mermaid flowchart로 AWS 서비스 표현)

B) C4 Model — System Context → Container → Component 계층 다이어그램

C) 시퀀스 다이어그램 — 주요 플로우(방 개설, 주문 확정 등) 별 요청/응답 흐름

D) 모두 포함 — 시스템 전체 구조도 + 주요 플로우 시퀀스

X) Other (please describe after [Answer]: tag below)

[Answer]: 
