# Business Rules — Unit 1: Core Backend
# 배달비 절약을 위한 음식 공동 구매 앱

**작성일**: 2026-05-24
**단계**: CONSTRUCTION — Functional Design

---

## BR-AUTH: 인증 규칙

> **MVP**: 닉네임 + 디바이스 UUID 방식. 이메일/소셜 로그인 없음.
> **Post-MVP**: 학교 이메일(도메인 제한) 로그인 추가 예정

### BR-AUTH-01: 최초 등록
- 앱 최초 실행 시 UUID(v4) 생성 → 로컬 저장 (AsyncStorage)
- 닉네임 입력 후 서버에 `{nickname, deviceToken}` 전송
- 서버는 닉네임 고유성 검증 후 Member 저장
- 이후 모든 요청 헤더에 `X-Device-Token: {uuid}` 포함

### BR-AUTH-02: 닉네임 고유성
- 닉네임은 서비스 전체에서 고유
- 2~12자, 특수문자 제외
- 중복 시 409 Conflict 반환 + 다른 닉네임 요청

### BR-AUTH-03: 디바이스 토큰 인증
- 모든 인증 필요 API는 `X-Device-Token` 헤더 검증
- DB에 존재하지 않는 토큰 → 401 Unauthorized
- JWT 없음 — 디바이스 토큰 자체가 인증 수단 (SECURITY-12 범위 축소)

---

## BR-ROOM: 방 규칙

### BR-ROOM-00: 방 탐색 필터 규칙
- 지원 필터: 카테고리(A), 모임유형(B), 거리(C)
- 거리 필터(C)는 모임유형이 `DINE_OUT`(직접외식)일 때 **자동 적용**
- `DELIVERY`, `DELIVERY_TOGETHER`는 거리 필터 미적용 (배달앱이 거리 커버)
- 기본 반경: 1,000m (클라이언트에서 조정 가능, 최대 5,000m)
- 거리 계산: Haversine 공식 (위경도 기반)

### BR-ROOM-01: 최대 인원 제약
- 방 개설 시 최대 인원은 최소 2명 이상이어야 한다 (서버 검증)
- 현재 참여 인원 > 변경하려는 최대 인원으로 변경 불가

### BR-ROOM-02: 방 마감 조건 (이중 마감)
- **수동 마감**: 방장이 명시적으로 마감 버튼을 누를 때 `OPEN → CLOSED`
- **자동 마감**: `closedAt` 시각이 도래하면 스케줄러가 `OPEN → CLOSED`
- **Race Condition 방지**: 상태 전환 쿼리에 `WHERE status = 'OPEN'` 조건 적용, 낙관적 락 사용

### BR-ROOM-03: 방장 탈퇴 정책
- 방장은 방을 탈퇴할 수 없다 (탈퇴 시도 시 에러 반환)
- 방장이 방을 없애려면 방 취소(`CANCELLED`) 처리해야 한다

### BR-ROOM-04: 참여자 탈퇴 조건
- `OPEN` 상태에서만 참여자 탈퇴 가능 (마감 전까지)
- `CLOSED` 이후에는 탈퇴 불가
- 탈퇴 시 해당 참여자의 OrderItem 일괄 삭제

### BR-ROOM-05: 방 취소 정책
- 방장만 취소 가능
- 모든 상태(`OPEN`, `CLOSED`, `CONFIRMED`)에서 취소 가능
- 취소 시 참여자 전원에게 FCM 푸시 알림 발송
- `COMPLETED` 상태에서는 취소 불가

### BR-ROOM-06: 방 상태 전이 매트릭스

| 현재 상태 | 이벤트 | 다음 상태 | 실행 주체 |
|-----------|--------|-----------|----------|
| OPEN | 방장 마감 / 시간 초과 | CLOSED | 방장 / 스케줄러 |
| OPEN | 방장 취소 / 인원 0명 | CANCELLED | 방장 / 시스템 |
| CLOSED | 방장 주문확정 | CONFIRMED | 방장 |
| CLOSED | 방장 취소 | CANCELLED | 방장 |
| CONFIRMED | 방장 완료 | COMPLETED | 방장 |
| CONFIRMED | 방장 취소 | CANCELLED | 방장 |

---

## BR-ORDER: 주문 규칙

### BR-ORDER-01: 주문 입력 조건
- `OPEN` 또는 `CLOSED` 상태의 방에만 주문 항목 추가 가능
- `CONFIRMED` 이후 주문 항목 추가/수정/삭제 불가 (방장 포함)
- 단, 방장의 방 취소(`CANCELLED`)는 허용

### BR-ORDER-02: 주문 확정 조건
- 방 상태가 `CLOSED`이고
- 참여자 수 >= 2명이고
- 모든 참여자의 주문 항목이 1개 이상 존재해야 확정 가능

### BR-ORDER-03: 본인 주문만 삭제 가능
- 참여자는 본인의 OrderItem만 삭제 가능
- 방장은 모든 참여자의 OrderItem 삭제 가능 (관리 목적)
- IDOR 방지: 서버에서 memberId 소유권 검증 필수

---

## BR-SETTLEMENT: 정산 규칙

### BR-SETTLEMENT-01: 배달비 계산 (PBT-02, PBT-03 적용 대상)
```
deliveryFeePerPerson = ceil(totalDeliveryFee / participantCount)
hostSurplus = (deliveryFeePerPerson × participantCount) - totalDeliveryFee
```

- 나머지가 발생하면 방장이 잉여금을 수령 (이득)
- 예시: 배달비 4,000원 / 3명 → 1인당 1,334원, 방장 잉여 2원

**PBT 속성**:
- `deliveryFeePerPerson × participantCount >= totalDeliveryFee` (항상 성립)
- `hostSurplus >= 0` (항상 성립)
- `hostSurplus < participantCount` (잉여금은 인원수 미만)

### BR-SETTLEMENT-02: 정산 생성 시점
- 방장이 주문확정(`CONFIRMED`) 처리 시 Settlement 자동 생성
- 취소(`CANCELLED`) 시 Settlement 생성하지 않음

### BR-SETTLEMENT-03: 계좌번호 보안
- 계좌번호는 AES-256 암호화하여 DB 저장
- 조회 시 복호화하여 반환
- 암호화 키는 환경변수로 관리 (코드 내 하드코딩 금지)
- SECURITY-01 준수

### BR-SETTLEMENT-04: 정산 표시 형식
```
[참여자 화면]
메뉴 합계:   12,000원
배달비:       1,334원
-----------------------
납부 금액:   13,334원

[방장 정보]
은행: 카카오뱅크
계좌: 3333-XX-XXXXXX (복호화 표시)
예금주: 홍길동
```

---

## BR-VALIDATION: 공통 입력 검증

| 필드 | 규칙 |
|------|------|
| 이메일 | RFC 5322 형식 |
| 비밀번호 | 8자 이상, 영문+숫자 혼합 |
| 닉네임 | 2~12자, 특수문자 제외 |
| 방 제목 | 2~30자 |
| 최대 인원 | 2~20 정수 |
| 배달비 | 0 이상 정수 |
| 메뉴명 | 1~50자 |
| 수량 | 1 이상 정수 |
| 단가 | 0 이상 정수 |
