# Unit Test Instructions
# 배달비 절약을 위한 음식 공동 구매 앱

**작성일**: 2026-05-24

---

## Unit 1 + 2: Spring Boot 단위 테스트

```bash
cd /home/sohegi/projects/ACC_1/backend

# 전체 테스트 실행
./gradlew test

# 테스트 결과 리포트
open build/reports/tests/test/index.html
```

### 테스트 파일 목록

| 파일 | 테스트 대상 | 특이사항 |
|------|------------|----------|
| `AuthServiceTest.java` | 회원가입, 디바이스 토큰 검증 | H2 인메모리 |
| `RoomServiceTest.java` | 방 생성/참여/상태전이 | Optimistic Lock 포함 |
| `OrderServiceTest.java` | 메뉴 입력, 주문 확정, 정산 | SettlementResult 반환 검증 |
| `ChatServiceTest.java` | 메시지 저장, 이력 조회, 참여자 검증 | 비참여자 차단 테스트 |
| `FcmNotificationAdapterTest.java` | FCM 발송 (Mock) | FirebaseMessaging mock |
| `AiRecommendClientTest.java` | Lambda URL 호출, fallback | MockWebServer |
| `DeliveryFeeCalculatorPbtTest.java` | 배달비 분배 PBT | jqwik — PBT-02,03 |
| `RoomStateValidatorPbtTest.java` | 방 상태 전이 PBT | jqwik — PBT-07 |

### PBT (Property-Based Testing) 별도 실행

```bash
./gradlew test --tests "*.PbtTest"
```

---

## Unit 3: Lambda 단위 테스트

```bash
cd /home/sohegi/projects/ACC_1/functions/ai-recommend

npm test

# 커버리지 포함
npm test -- --coverage
```

### 테스트 케이스

| 케이스 | 파일 |
|--------|------|
| X-Internal-Key 없음 → 403 | `index.test.js` |
| X-Internal-Key 불일치 → 403 | `index.test.js` |
| INTERNAL_SECRET_KEY 미설정 → 500 | `index.test.js` |
| 정상 요청 → recommendations 배열 포함 | `index.test.js` |
| Gemini 실패 → explanation 빈 문자열 fallback | `index.test.js` |
| rule_engine 점수 계산 | `index.test.js` |

---

## 전체 테스트 한 번에

```bash
# 백엔드
cd /home/sohegi/projects/ACC_1/backend && ./gradlew test

# Lambda
cd /home/sohegi/projects/ACC_1/functions/ai-recommend && npm test
```
