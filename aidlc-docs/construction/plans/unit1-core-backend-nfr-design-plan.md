# NFR Design Plan — Unit 1: Core Backend
# 배달비 절약을 위한 음식 공동 구매 앱

**작성일**: 2026-05-24
**단계**: CONSTRUCTION — NFR Design

## 생성 체크리스트
- [x] Step 1: nfr-design-patterns.md 생성
- [x] Step 2: logical-components.md 생성

---

## 명확화 질문

아래 [Answer]: 태그에 답변 후 **"완료"** 라고 알려주세요.

---

### Q1: 디바이스 토큰 인증 위치

Spring Security 커스텀 필터를 어디에 배치할까요?

A) **Filter 체인** — `OncePerRequestFilter` 구현, Spring Security 필터 체인에 등록
B) **Interceptor** — Spring MVC `HandlerInterceptor`로 구현
C) **AOP** — `@Authenticated` 커스텀 어노테이션 + AOP

**[Answer]:**

---

### Q2: Redis 캐싱 전략

디바이스 토큰 인증 시 매 요청마다 DB를 조회하면 부하가 생깁니다. Redis 캐싱을 어떻게 할까요?

A) **캐싱 적용** — 최초 인증 후 Redis에 캐시, TTL 24시간
B) **캐싱 없음** — 매 요청마다 DB 조회 (단순, 300명 규모에서 충분)

**[Answer]:**

---

### Q3: 동시성 제어 (방 참여)

방 참여 시 최대 인원 초과를 방지하는 방식은?

A) **낙관적 락** — `@Version` 컬럼 사용, 충돌 시 재시도
B) **비관적 락** — `SELECT FOR UPDATE`, 확실하지만 DB 부하
C) **Redis 원자 연산** — `INCR` 명령으로 카운터 관리

**[Answer]:**

---

### Q4: 전역 에러 응답 형식

API 에러 응답 형식을 어떻게 통일할까요?

A) **코드 + 메시지** — `{"code": "ROOM_NOT_FOUND", "message": "방을 찾을 수 없습니다"}`
B) **HTTP 상태만** — 상태 코드로만 구분, body 없음
C) **상세 형식** — `{"code": ..., "message": ..., "timestamp": ..., "path": ...}`

**[Answer]:**

---

### Q5: 자동 마감 스케줄러 방식

방 자동 마감을 어떻게 구현할까요?

A) **`@Scheduled` 폴링** — 1분마다 DB 조회해서 만료된 방 일괄 마감
B) **Redis TTL 이벤트** — 방 생성 시 Redis key TTL 설정, 만료 이벤트로 마감 트리거
C) **Spring TaskScheduler** — 방 생성 시 마감 시각에 작업 예약

**[Answer]:**
