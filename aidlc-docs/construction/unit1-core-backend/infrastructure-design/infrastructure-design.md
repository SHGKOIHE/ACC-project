# Infrastructure Design — Unit 1: Core Backend
# 배달비 절약을 위한 음식 공동 구매 앱

**작성일**: 2026-05-24
**단계**: CONSTRUCTION — Infrastructure Design

---

## 인프라 구성 결정

| 항목 | 결정 | 비고 |
|------|------|------|
| 외부 접근 | CloudFront → 미니PC 공인 IP | 학교망 방화벽 확인 필요 |
| 환경 분리 | 로컬(개발) + 미니PC(운영) | 개발 중 운영 데이터 오염 방지 |
| CI/CD | GitHub Actions → SSH 배포 | push 시 자동 빌드·배포 |
| DB 백업 | pg_dump + cron → S3 | 일 1회, 30일 보관 |
| 환경변수 | .env 파일 (미니PC 로컬) | git 제외, 파일권한 600 |

---

## 컴포넌트 → 인프라 매핑

| 컴포넌트 | 인프라 | 스펙 |
|----------|--------|------|
| Spring Boot App | Docker Container (미니PC) | 메모리 4GB 할당 |
| PostgreSQL | Docker Container (미니PC) | 메모리 2GB, pgdata 볼륨 |
| Redis | Docker Container (미니PC) | 메모리 512MB, redisdata 볼륨 |
| 정적 리소스/이미지 | AWS S3 | us-east-1 (CloudFront 연동) |
| CDN / DDoS 방어 | AWS CloudFront | S3 + 미니PC Origin |
| DB 백업 저장소 | AWS S3 (별도 버킷) | Glacier 전환 30일 후 |
| Lambda (AI) | AWS Lambda | Unit 3 담당 |

---

## 네트워크 구성

```
Internet
    │
    ▼
AWS CloudFront
    ├─ /api/*  → Origin: 미니PC 공인 IP:8080
    └─ 이미지  → Origin: S3 버킷
    │
    ▼
미니PC (공인 IP)
    └─ Docker Compose
        ├─ app:8080   (Spring Boot)
        ├─ postgres:5432 (내부망만)
        └─ redis:6379    (내부망만)
```

**CloudFront 설정**:
- `Cache-Control` 헤더 기반 캐싱 (API는 캐싱 비활성화)
- HTTPS 강제 리다이렉트
- 미니PC Origin: HTTP (내부 TLS 불필요, CloudFront가 외부 TLS 처리)

**미니PC 방화벽 (UFW)**:
```bash
ufw allow 8080/tcp   # CloudFront Origin 연결
ufw deny 5432        # PostgreSQL 외부 차단
ufw deny 6379        # Redis 외부 차단
```

---

## Docker Compose (운영)

```yaml
# infrastructure/docker-compose.yml
version: '3.9'

services:
  app:
    image: foodgroup/backend:${APP_VERSION:-latest}
    ports:
      - "8080:8080"
    env_file: .env
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_started
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    deploy:
      resources:
        limits:
          memory: 4G

  postgres:
    image: postgres:16-alpine
    env_file: .env
    volumes:
      - pgdata:/var/lib/postgresql/data
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $POSTGRES_USER"]
      interval: 10s
      retries: 5
    deploy:
      resources:
        limits:
          memory: 2G

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redisdata:/data
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 512M

volumes:
  pgdata:
  redisdata:
```

---

## 환경변수 (.env)

```bash
# .env (미니PC /app/.env, 권한 600)

# App
SPRING_PROFILES_ACTIVE=prod
APP_VERSION=latest

# PostgreSQL
POSTGRES_DB=foodgroup
POSTGRES_USER=foodgroup
POSTGRES_PASSWORD=<strong-password>
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/foodgroup

# Redis
REDIS_PASSWORD=<strong-password>
SPRING_REDIS_PASSWORD=<strong-password>

# Security
ENCRYPTION_KEY=<base64-32bytes>   # AES-256 계좌번호 암호화

# External APIs (Unit 3 담당이지만 app 컨테이너에서 참조)
KAKAO_REST_API_KEY=<key>
GEMINI_API_KEY=<key>
FCM_SERVER_KEY=<key>

# AWS
AWS_ACCESS_KEY_ID=<key>
AWS_SECRET_ACCESS_KEY=<key>
AWS_S3_BUCKET=foodgroup-images
AWS_S3_BACKUP_BUCKET=foodgroup-backup
AWS_REGION=us-east-1
```

---

## S3 DB 백업

```bash
# /app/scripts/backup.sh
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
FILENAME="backup_${DATE}.sql.gz"

docker exec postgres pg_dump -U $POSTGRES_USER $POSTGRES_DB \
  | gzip > /tmp/${FILENAME}

aws s3 cp /tmp/${FILENAME} s3://${AWS_S3_BACKUP_BUCKET}/postgres/${FILENAME}
rm /tmp/${FILENAME}
```

```bash
# crontab (미니PC)
0 3 * * * /app/scripts/backup.sh >> /var/log/backup.log 2>&1
```

---

## 환경 분리 전략

| 항목 | 로컬 (개발) | 미니PC (운영) |
|------|------------|--------------|
| 실행 방법 | `docker compose up` | GitHub Actions 자동 배포 |
| DB | H2 인메모리 또는 로컬 PG | 운영 PostgreSQL |
| 외부 API | Mock / Test Key | 실제 API Key |
| 프로파일 | `local` | `prod` |
| 로그 레벨 | DEBUG | INFO |
