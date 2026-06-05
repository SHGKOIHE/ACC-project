# FoodGroup — 공동 배달 주문 앱

> 같은 장소에 있는 사람들이 함께 배달 주문을 모아 배달비를 나누는 모바일 애플리케이션

---

## 주요 기능

| 기능 | 설명 |
|------|------|
| 방 생성 / 참여 | 배달 주문방 생성, QR 또는 목록에서 참여 |
| 주문 관리 | 참여자별 메뉴 추가 / 삭제 |
| 방 마감 → 주문 확정 | 방장이 모집 마감 후 전체 주문 확정 |
| 정산 | 배달비 분담, 개인별 정산 금액 계산 |
| 실시간 채팅 | WebSocket(STOMP) 기반 방 내 채팅 |
| AI 맛집 추천 | AWS Bedrock(Claude 3 Haiku) 기반 AI 추천 |
| 푸시 알림 | FCM을 통한 방 상태 변경 알림 |

---

## 기술 스택

### Mobile
- **React Native** + **Expo 54**
- **TanStack Query** — 서버 상태 관리 및 자동 폴링
- **@stomp/stompjs** — WebSocket 채팅
- **expo-secure-store** — Device Token 안전 저장

### Backend
- **Spring Boot 3.3** / **Java 17**
- **AWS Lambda** (Serverless, SnapStart 적용 가능)
- **AWS API Gateway** HTTP API v2
- **AWS DynamoDB** — 메인 데이터베이스
- **Redis** — Device Token 세션 관리 (24h TTL)

### AI / Infrastructure
- **AWS Bedrock** (Claude 3 Haiku) — AI 맛집 추천 Lambda
- **AWS Lightsail** — WebSocket 프록시 서버
- **AWS S3** — Lambda 배포 아티팩트 저장

### Load Testing / Monitoring
- **k6** — 부하 테스트 (baseline, spike, AI surge, cold start 시나리오)
- **InfluxDB + Grafana** — 성능 대시보드

---

## 아키텍처

```
[Mobile App (Expo)]
        │
        ├─── REST API ──► [API Gateway] ──► [Lambda: Spring Boot]
        │                                          │
        │                                    [DynamoDB]
        │                                    [Redis]
        │
        ├─── WebSocket ──► [Lightsail Proxy] ──► [Chat Server]
        │
        └─── AI 추천 ──► [API Gateway] ──► [Lambda: ai-recommend]
                                                   │
                                             [AWS Bedrock]
                                           (Claude 3 Haiku)
```

---

## 인증 방식

Device Token 기반 인증 (앱 최초 실행 시 UUID 생성):

```
앱 실행 → UUID 생성 → SecureStore 저장
                            │
                      POST /api/auth/register
                            │
                   모든 요청: X-Device-Token 헤더
                            │
                  Lambda → Redis 조회 → memberId
```

---

## 프로젝트 구조

```
ACC_1/
├── mobile/          # React Native (Expo) 앱
├── backend/         # Spring Boot Lambda 백엔드
├── functions/
│   └── ai-recommend/  # Node.js AI 추천 Lambda
├── k6/              # 부하 테스트 시나리오
│   ├── main.js
│   └── scenarios/
│       ├── auth.js
│       ├── baseline.js
│       ├── spike.js
│       ├── ai_surge.js
│       └── cold_start.js
└── docker-compose.monitoring.yml  # InfluxDB + Grafana
```

---

## 실행 방법

### 백엔드 (로컬)

```bash
cd backend
./gradlew bootRun
```

### 백엔드 (Lambda 배포)

```bash
cd backend
./scripts/build-lambda.sh
```

> JAR → S3(`food-app-assets-sj`) → Lambda 코드 업데이트 자동 수행

### 모바일

```bash
cd mobile
cp .env.example .env   # API URL 설정
npx expo start --clear
```

### 부하 테스트

```bash
# 기본 부하 (50명 × 5분)
k6 run -e BASE_URL=https://<api-gateway-url> k6/scenarios/baseline.js

# 스파이크 테스트
k6 run -e BASE_URL=https://<api-gateway-url> k6/scenarios/spike.js

# 모니터링 포함 전체 실행
docker-compose -f docker-compose.monitoring.yml up -d
k6 run -e BASE_URL=https://<api-gateway-url> k6/main.js \
  --out influxdb=http://localhost:8086/k6
```

---

## 환경변수

### Mobile (`.env`)

| 변수 | 설명 |
|------|------|
| `EXPO_PUBLIC_API_BASE_URL` | API Gateway 엔드포인트 |
| `EXPO_PUBLIC_WS_URL` | WebSocket 서버 URL |
| `EXPO_PUBLIC_KAKAO_JS_KEY` | 카카오 지도 API 키 |

### Lambda 환경변수

| 변수 | 설명 |
|------|------|
| `DYNAMODB_TABLE_PREFIX` | DynamoDB 테이블 접두사 |
| `REDIS_HOST` | Redis 엔드포인트 |
| `INTERNAL_SECRET_KEY` | AI Lambda 내부 인증 키 |

---

## 방 상태 흐름

```
OPEN → CLOSED → CONFIRMED → DELIVERING → COMPLETED
  └──────────────────────────────────────► CANCELLED
```

| 상태 | 설명 |
|------|------|
| OPEN | 모집 중, 참여 및 주문 추가 가능 |
| CLOSED | 방장이 마감, 주문 확정 대기 |
| CONFIRMED | 주문 확정, 정산 정보 생성 |
| DELIVERING | 배달 중 |
| COMPLETED | 완료 |
| CANCELLED | 취소 |

---

## 예상 월 비용 (소규모 기준)

| 서비스 | 월 비용 |
|--------|---------|
| AWS Lambda | ~$0 (프리 티어) |
| AWS DynamoDB | ~$1-2 |
| AWS Bedrock (Claude 3 Haiku) | ~$7-8 |
| AWS Lightsail | $3.50 |
| **합계** | **~$13/월** |
