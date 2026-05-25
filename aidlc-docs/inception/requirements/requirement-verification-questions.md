# Requirements Clarification Questions
# 배달비 절약을 위한 음식 공동 구매 앱

아래 질문에 각 [Answer]: 태그 뒤에 해당 알파벳을 입력해주세요.
모든 질문에 답변하신 후 "완료"라고 알려주세요.

---

## Question 1
프로젝트 목적은 무엇인가요?

[Answer]: 배달비 절약을 위한 음식 공동 구매 앱 (사용자 직접 입력)

---

## Question 2
어떤 플랫폼을 타겟으로 하나요?

A) 모바일 앱 (iOS / Android — React Native 또는 Flutter)

B) 웹 앱 (브라우저 기반 — React / Next.js)

C) 모바일 + 웹 둘 다

D) 웹 앱 (백오피스/관리자 포함)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Question 3
백엔드(서버) 기술 스택은 어떻게 할까요?

A) Java / Spring Boot (REST API)

B) Python / FastAPI 또는 Django

C) Node.js / NestJS (TypeScript)

D) 백엔드 없이 Firebase / Supabase 등 BaaS 사용

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Question 4
공동 구매 방에서 사용자 간 실시간 소통이 필요한가요?

A) Yes — 실시간 채팅 및 알림 필요 (WebSocket / FCM)

B) Partial — 주문 상태 알림만 필요 (Push Notification)

C) No — 비동기 방식으로 충분 (새로고침 기반)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Question 5
결제 기능이 필요한가요?

A) Yes — 실제 결제 연동 필요 (카카오페이, 토스, 신용카드 등)

B) Partial — 1차 버전은 결제 없이 주문만, 이후 연동 예정

C) No — 결제는 앱 밖에서 처리 (현금/계좌이체 등 오프라인)

X) Other (please describe after [Answer]: tag below)

[Answer]: C

---

## Question 6
위치 기반 기능이 필요한가요? (같은 배달 주소 / 근처 사용자 모집 등)

A) Yes — 지도 및 위치 기반 방 탐색 필요 (카카오맵 / Google Maps)

B) Partial — 주소 입력 기반으로 충분 (지도 없이)

C) No — 위치 기능 불필요

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Question 7
이번에 만들 범위(MVP)는 어느 정도인가요?

A) PoC / 데모 — 핵심 기능만, 빠르게 검증용

B) MVP — 실제 사용 가능한 최소 기능 (회원, 방 개설/참여, 주문 확인)

C) Full — MVP + 결제, 알림, 리뷰, 관리자 대시보드 포함

X) Other (please describe after [Answer]: tag below)

[Answer]: B

---

## Question 8 — Security Extension Opt-In
**Security Baseline** 확장 규칙을 적용할까요?
(인증/인가, SQL Injection 방지, 시크릿 관리 등 보안 요건을 코드 생성 단계에서 blocking constraint로 강제 적용)

A) Yes — 보안 규칙 전체 적용 (프로덕션 수준 권장)

B) No — 보안 규칙 스킵 (PoC / 프로토타입에 적합)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Question 9 — Property-Based Testing Extension Opt-In
**Property-Based Testing (PBT)** 확장 규칙을 적용할까요?
(공동 구매 분배 로직, 금액 계산, 상태 전이 등에 대한 속성 기반 테스트 강제 적용)

A) Yes — PBT 규칙 전체 적용 (비즈니스 로직이 복잡한 경우 권장)

B) Partial — 순수 함수(금액 계산, 분배 로직)에만 적용

C) No — PBT 규칙 스킵 (일반 단위 테스트로 충분)

X) Other (please describe after [Answer]: tag below)

[Answer]: B
