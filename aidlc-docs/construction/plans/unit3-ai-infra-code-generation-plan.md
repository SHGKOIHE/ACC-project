# Code Generation Plan — Unit 3: AI + Infra

**작성일**: 2026-05-24
**단계**: CONSTRUCTION — Code Generation

## 확정 설계 결정
- Lambda 런타임: Node.js 20.x
- Lambda 호출: Function URL (auth=NONE) + X-Internal-Key 헤더
- AI 추천 방식: 규칙 엔진(점수) + Gemini API(자연어 설명)
- 사용자 트리거: "추천받기" 버튼 (자동 추천 없음)
- S3: 버킷 2개 (이미지 / 배포 아티팩트)
- 리전: us-east-1
- 배포: AWS CLI (IaC 없음)

---

## 생성 단계

### Step 1: Lambda — AI 추천 함수
- [x] `functions/ai-recommend/package.json` — Node.js 프로젝트, @google/generative-ai
- [x] `functions/ai-recommend/index.js` — Lambda 핸들러 (X-Internal-Key 검증 + orchestration)
- [x] `functions/ai-recommend/rule_engine.js` — 규칙 기반 추천 점수 계산
- [x] `functions/ai-recommend/gemini_client.js` — Gemini API 자연어 설명 생성
- [x] `functions/ai-recommend/index.test.js` — Jest 단위 테스트

### Step 2: 백엔드 — AI 클라이언트 모듈
- [x] `backend/src/main/java/com/foodgroup/ai/AiRecommendClient.java` — WebClient로 Lambda Function URL 호출
- [x] `backend/src/main/java/com/foodgroup/ai/AiRecommendRequest.java` — 요청 DTO
- [x] `backend/src/main/java/com/foodgroup/ai/AiRecommendResponse.java` — 응답 DTO
- [x] `backend/src/main/java/com/foodgroup/ai/AiRecommendController.java` — POST /api/rooms/{id}/recommend
- [x] `backend/src/main/resources/application.yml` 추가 설정 — ai.lambda.function-url, ai.lambda.internal-key
- [x] `backend/src/test/java/com/foodgroup/ai/AiRecommendClientTest.java` — MockWebServer 테스트

### Step 3: 인프라 README
- [x] `functions/ai-recommend/DEPLOY.md` — AWS CLI 배포 가이드
- [x] `infra/s3-setup.md` — S3 버킷 생성 명령어

---

## 총 단계: 3단계
## 예상 생성 파일: ~13개
## 상태: ✅ 완료
