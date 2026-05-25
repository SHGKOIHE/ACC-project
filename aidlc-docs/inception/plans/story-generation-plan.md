# Story Generation Plan
# 배달비 절약을 위한 음식 공동 구매 앱

아래 질문에 각 [Answer]: 태그 뒤에 해당 알파벳을 입력해주세요.
모든 질문에 답변하신 후 "완료"라고 알려주세요.

---

## Part 1: 페르소나 (Personas)

## Question 1
앱에 등장하는 사용자 유형은 무엇인가요?

A) 방장 + 참여자 두 가지만 (기본)

B) 방장 + 참여자 + 관리자(Admin) 세 가지

C) 방장 + 참여자 + 가게 사장님(식당 측) 세 가지

D) 방장 + 참여자 + 관리자 + 가게 사장님 네 가지

X) Other (please describe after [Answer]: tag below)

[Answer]: B

---

## Question 2
'방장'은 어떤 사용자인가요? 실생활 특성을 선택해주세요.

A) 자취생/직장인 — 혼자 배달시키기엔 배달비가 부담스러운 사람

B) 기숙사/오피스텔 거주자 — 같은 건물 주민과 자주 공동 구매하는 사람

C) 특정 특성 없음 — 단순히 방을 먼저 만드는 사람

X) Other (please describe after [Answer]: tag below)

[Answer]: X
가까운 공간(ex 제2 기숙사, 정문 건너편, 서천동)에서 배달비를 절약하고 싶은 사람 + 밥을 같이 먹고 싶은 사람 + 직접 가게에 같이 가서 함께 먹고 싶은 사람 

---

## Part 2: 스토리 분류 방식 (Breakdown Approach)

## Question 3
User Stories를 어떤 방식으로 구성할까요?

A) Feature-Based — 기능별 묶음 (회원관리, 방관리, 채팅, 지도 등)

B) User Journey-Based — 사용자 여정 흐름 (가입 → 방탐색 → 참여 → 주문 → 완료)

C) Persona-Based — 페르소나별 묶음 (방장의 스토리, 참여자의 스토리 분리)

D) Epic-Based — 대분류 에픽 아래 세부 스토리 계층 구조

X) Other (please describe after [Answer]: tag below)

[Answer]: X
음식 추천 AI를 활용할 계획이 있는데, 미리 음식 종류를 정한 (참여자, 방장)과 그렇지 않은 참여자, 방장

---

## Part 3: 스토리 세분화 수준 (Granularity)

## Question 4
스토리의 세분화 수준은 어느 정도로 할까요?

A) 에픽 수준 — 큰 기능 단위 (예: "사용자로서 공동 구매 방에 참여할 수 있다")

B) 스토리 수준 — 중간 단위, 스프린트 내 완료 가능 크기

C) 태스크 수준 — 매우 세분화 (예: "이메일 형식 유효성 검사 오류 메시지 표시")

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Part 4: 인수 기준 (Acceptance Criteria)

## Question 5
인수 기준(Acceptance Criteria) 형식은 어떻게 할까요?

A) Given-When-Then (BDD 스타일) — 구체적인 시나리오 형식

B) 체크리스트 — 간단한 불릿 포인트 목록

C) 둘 다 — 주요 스토리는 GWT, 단순 스토리는 체크리스트

X) Other (please describe after [Answer]: tag below)

[Answer]: C

---

## Part 5: MVP 범위 재확인

## Question 6
MVP에서 반드시 포함할 핵심 사용자 여정은 무엇인가요? (복수 선택 가능 — 해당 알파벳 모두 나열)

A) 회원가입 / 로그인

B) 방 개설 (식당, 배달비, 마감시간 설정)

C) 지도에서 근처 방 탐색 및 참여

D) 방 내 개인 메뉴 선택 및 주문

E) 실시간 채팅

F) 주문 완료 및 배달비 1/n 확인

X) Other (please describe after [Answer]: tag below)

[Answer]: A B C(지도는 근처 식당만 검색) D(주문은 외부 앱에서, 식당에서 메뉴랑 가격이랑 추출가능한가?) E  

---

## Part 6: 엣지 케이스 처리

## Question 7
아래 엣지 케이스 중 MVP에서 스토리로 다룰 항목을 선택해주세요. (복수 선택 가능)

A) 마감 시간 초과 후 참여 시도

B) 방 정원 초과 참여 시도

C) 방장이 방을 취소하는 경우

D) 참여자가 중간에 나가는 경우

E) 위 항목 모두 포함

F) 엣지 케이스는 MVP에서 다루지 않음

X) Other (please describe after [Answer]: tag below)

[Answer]: F

---

## Story Generation Plan Checklist

아래는 답변 확인 후 실행할 생성 단계입니다.

- [x] Step 1: 페르소나 문서 생성 (`personas.md`)
- [x] Step 2: 에픽 목록 정의
- [x] Step 3: 방장 페르소나 User Stories 작성 (EP-02, EP-03, EP-08, EP-09)
- [x] Step 4: 참여자 페르소나 User Stories 작성 (EP-04, EP-05, EP-06, EP-07)
- [x] Step 5: 관리자 페르소나 스토리 작성 (EP-10) — 가게사장 제외 (Q1=B)
- [x] Step 6: 각 스토리에 인수 기준 추가 (GWT + 체크리스트 혼합, Q5=C)
- [x] Step 7: 엣지 케이스 스토리 추가 — 스킵 (Q7=F)
- [x] Step 8: stories.md 최종 저장
- [x] Step 9: personas.md 최종 저장
