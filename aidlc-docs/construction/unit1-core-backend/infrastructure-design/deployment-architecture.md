# Deployment Architecture — Unit 1: Core Backend
# 배달비 절약을 위한 음식 공동 구매 앱

**작성일**: 2026-05-24
**단계**: CONSTRUCTION — Infrastructure Design

---

## CI/CD 파이프라인 (GitHub Actions → SSH)

```
개발자 push (unit/core-backend)
    │
    ▼
GitHub Actions
    ├─ 1. Checkout
    ├─ 2. Java 17 Setup
    ├─ 3. Gradle Build + Test
    ├─ 4. Semgrep / Grype / Gitleaks 보안 스캔
    │       └─ HIGH/CRITICAL → 파이프라인 중단
    ├─ 5. Docker 이미지 빌드
    └─ 6. SSH → 미니PC 배포
            ├─ docker compose pull
            ├─ docker compose up -d --no-deps app
            └─ 헬스체크 확인 (/actuator/health)
```

### GitHub Actions 워크플로우 (`deploy.yml`)

```yaml
name: Deploy Core Backend

on:
  push:
    branches: [unit/core-backend, main]
    paths: [backend/**]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build
        run: cd backend && ./gradlew build

      - name: Security Scan
        run: |
          # Semgrep
          # Grype
          # Gitleaks

      - name: Build Docker Image
        run: docker build -t foodgroup/backend:${{ github.sha }} ./backend

      - name: Deploy to MiniPC
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.MINIPC_HOST }}
          username: ${{ secrets.MINIPC_USER }}
          key: ${{ secrets.MINIPC_SSH_KEY }}
          script: |
            cd /app
            APP_VERSION=${{ github.sha }} docker compose up -d --no-deps app
            sleep 10
            curl -f http://localhost:8080/actuator/health
```

---

## 전체 배포 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                         GitHub                                   │
│  unit/core-backend ──push──▶ GitHub Actions                     │
│                              │ build + scan + deploy             │
└──────────────────────────────┼──────────────────────────────────┘
                               │ SSH
                               ▼
┌──────────────────────────────────────────────────────────────────┐
│                    미니PC (온프레미스)                            │
│                                                                  │
│  /app/                                                           │
│  ├─ docker-compose.yml                                           │
│  ├─ .env (권한 600)                                              │
│  └─ scripts/backup.sh                                            │
│                                                                  │
│  Docker Compose                                                  │
│  ├─ app:8080      ◀── GitHub Actions SSH 배포                   │
│  ├─ postgres:5432 (내부망)                                       │
│  └─ redis:6379    (내부망)                                       │
│                                                                  │
│  UFW: 8080 허용 / 5432,6379 차단                                 │
└──────────────────┬───────────────────────────────────────────────┘
                   │ Origin (HTTP:8080)
                   ▼
┌──────────────────────────────────────────────────────────────────┐
│                        AWS                                       │
│                                                                  │
│  CloudFront                                                      │
│  ├─ /api/*     → 미니PC Origin                                   │
│  └─ /images/*  → S3 Origin                                      │
│                                                                  │
│  S3 (이미지)       S3 (DB 백업, cron 03:00)                     │
└──────────────────────────────────────────────────────────────────┘
                   ▲
                   │ HTTPS
              📱 React Native App
```

---

## 로컬 개발 환경

```bash
# 로컬 실행 (개발자 PC)
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'

# 또는 Docker로 DB만 띄우고 앱은 IDE에서 실행
docker compose -f docker-compose.local.yml up -d  # postgres + redis만
```

```yaml
# docker-compose.local.yml
services:
  postgres:
    image: postgres:16-alpine
    ports: ["5432:5432"]
    environment:
      POSTGRES_DB: foodgroup_dev
      POSTGRES_USER: dev
      POSTGRES_PASSWORD: dev

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
```

---

## 배포 체크리스트

```
배포 전
  [ ] .env 파일 미니PC에 존재 확인
  [ ] Docker 서비스 실행 중 확인
  [ ] S3 버킷 권한 확인

배포 후
  [ ] /actuator/health 200 확인
  [ ] CloudFront 경유 API 응답 확인
  [ ] 로그 에러 없음 확인
```
