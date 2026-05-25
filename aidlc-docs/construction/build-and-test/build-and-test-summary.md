# Build and Test Summary
# 배달비 절약을 위한 음식 공동 구매 앱

**작성일**: 2026-05-24

---

## 유닛별 완료 현황

| 유닛 | 코드 생성 | QA 승인 | 비고 |
|------|-----------|---------|------|
| Unit 1: Core Backend | ✅ | ✅ | 40개 테스트 통과 |
| Unit 2: Realtime + Mobile | ✅ | ✅ | 채팅/FCM/모바일 6화면 |
| Unit 3: AI + Infra | ✅ | ✅ | Critical 수정 후 승인 |

---

## 잔존 Low 이슈 (비블로킹)

| 유닛 | 위치 | 내용 |
|------|------|------|
| Unit 1 | `deviceToken` DB 저장 | 평문 저장 — post-MVP 암호화 권고 |
| Unit 2 | `WebSocketConfig` | `allowedOriginPatterns("*")` — 운영 전 도메인 제한 필요 |
| Unit 2 | `ChatService.sendToRoom` | N+1 쿼리 가능성 — 부하 테스트 후 판단 |
| Unit 3 | `gemini_client.js` | `clearTimeout` 누락 |
| Unit 3 | `AiRecommendController` | 단위 테스트 없음 |

---

## 빌드 명령 요약

```bash
# 백엔드 빌드
cd backend && ./gradlew build -x test

# 백엔드 테스트
cd backend && ./gradlew test

# Lambda 테스트
cd functions/ai-recommend && npm test

# 모바일 빌드 (EAS)
cd mobile && eas build --profile preview --platform android
```

---

## 다음 단계 (post-MVP)

1. `deviceToken` AES-256 암호화 적용
2. WebSocket CORS 도메인 제한
3. AI 추천 결과 Redis 캐시 (Lambda 호출 비용 절감)
4. `AiRecommendController` 단위 테스트 추가
5. CloudFront + S3 이미지 CDN 연동
