# Story Planning Clarification Questions

답변 분석 중 아래 3가지 모호한 부분이 발견되었습니다. 해결 후 스토리 생성을 진행합니다.
각 [Answer]: 태그 뒤에 알파벳을 입력하신 후 "완료"라고 알려주세요.

---

## Ambiguity 1: 앱이 지원하는 모임 유형 (Q2 관련)

Q2 답변에서 아래 세 가지 유형을 언급하셨습니다:
1. 배달비 절약을 위한 **공동 배달 주문**
2. **같이 밥 먹기** (함께 모여서 식사)
3. **직접 가게에 같이 가서 함께 먹기** (외식 모임)

이 세 가지 모임 유형을 모두 하나의 앱에서 지원하려는 건가요?

### Clarification Question 1
MVP에서 지원할 모임 유형은 무엇인가요?

A) 공동 배달 주문만 — 같은 주소로 배달시키는 것에 집중

B) 배달 + 함께 모여 먹기 두 가지 — 방 개설 시 유형 선택 가능

C) 배달 + 함께 모여 먹기 + 직접 외식 가기 세 가지 모두

D) 유형 구분 없이 통합 — "같이 음식 관련 무언가를 하고 싶은 사람들을 모집"

X) Other (please describe after [Answer]: tag below)

[Answer]: C(함께 모여먹기 == 배달 시켜서 같이 먹기)

---

## Ambiguity 2: AI 음식 추천 기능 범위 (Q3 관련)

Q3 답변에서 "음식 추천 AI를 활용할 계획"과 "미리 음식 종류를 정한 사람 vs 아직 못 정한 사람"을 언급하셨습니다.

### Clarification Question 2
AI 음식 추천 기능은 언제 구현할 계획인가요?

A) MVP에 포함 — 미리 음식 종류를 정한/못 정한 사용자에게 AI 추천 제공

B) MVP 이후 — 1차 MVP 완성 후 다음 버전에서 추가

C) 기획 단계 — 아직 구체적인 계획 없음, 추후 논의 예정

X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Clarification Question 3
"미리 음식 종류를 정한 참여자 vs 아직 못 정한 참여자"는 스토리 분류에서 어떻게 활용하나요?

A) 두 가지 별도 페르소나로 분리 — "결정된 참여자"와 "미결정 참여자" 각각의 스토리

B) 하나의 참여자 페르소나 안에서 조건 분기로 처리 — AI 추천을 받을지 여부

C) 이번 User Stories에서는 AI 기능을 제외하고 기본 페르소나만 정의

X) Other (please describe after [Answer]: tag below)

[Answer]: B

---

## Ambiguity 3: 식당 메뉴/가격 정보 처리 방식 (Q6 관련)

Q6 답변에서 "식당에서 메뉴랑 가격이랑 추출 가능한가?"라고 질문하셨습니다. 이 부분을 어떻게 처리할지 결정이 필요합니다.

### Clarification Question 4
방 개설 시 식당 메뉴/가격 정보를 어떻게 제공할까요?

A) 방장이 직접 입력 — 메뉴명, 가격, 최소주문금액, 배달비를 수동으로 입력

B) 외부 음식 배달 앱 연동 — 배달의민족 / 쿠팡이츠 등 공개 API 활용 (가능 여부 별도 확인 필요)

C) 링크 붙여넣기 — 방장이 배달앱 링크를 공유하면 참여자가 개별적으로 확인

D) 하이브리드 — MVP는 직접 입력, 이후 버전에서 자동 추출 연동 고려

X) Other (please describe after [Answer]: tag below)

[Answer]: B 안되면 C
