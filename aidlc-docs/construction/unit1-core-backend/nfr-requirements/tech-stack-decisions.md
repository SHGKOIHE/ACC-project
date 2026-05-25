# Tech Stack Decisions — Unit 1: Core Backend
# 배달비 절약을 위한 음식 공동 구매 앱

**작성일**: 2026-05-24
**단계**: CONSTRUCTION — NFR Requirements

---

## 확정된 기술 스택

| 레이어 | 기술 | 버전 | 근거 |
|--------|------|------|------|
| 언어 | Java | 17 | LTS, Record/Pattern Matching 활용 |
| 프레임워크 | Spring Boot | 3.x | 표준, springdoc-openapi 연동 |
| 아키텍처 | 계층형 (Controller→Service→Repository) | — | 빠른 개발, 3인 팀 표준 |
| ORM | Spring Data JPA (Hibernate) | — | 빠른 개발, 쿼리 최적화 필요 시 JPQL |
| DB | PostgreSQL | 16 | 관계형, 트랜잭션 필수 |
| 캐시/세션 | Redis | 7 | 디바이스 토큰 캐싱, 향후 채팅 pub/sub |
| 빌드 | Gradle | 8.x | Kotlin DSL |
| 테스트 | JUnit 5 + jqwik | — | PBT 적용 (PBT-02,03,07) |
| API 문서 | springdoc-openapi | 2.x | OpenAPI 자동생성 |
| 보안 | Spring Security | 6.x | 디바이스 토큰 커스텀 필터 |
| 암호화 | Java AES-256-GCM | — | 계좌번호 암호화 |
| 로깅 | Logback + JSON encoder | — | 구조화 로깅 |
| 모니터링 | Spring Boot Actuator | — | 헬스체크, 메트릭 |
| 스케줄러 | Spring @Scheduled | — | 자동 마감 처리 |
| 컨테이너 | Docker Compose | — | 미니PC 운영 |

---

## 주요 의존성 (build.gradle)

```kotlin
dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // DB
    runtimeOnly("org.postgresql:postgresql")

    // API 문서
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.x")

    // 로깅
    implementation("net.logstash.logback:logstash-logback-encoder:7.x")

    // PBT
    testImplementation("net.jqwik:jqwik:1.x")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
```

---

## 인증 방식 결정

| 항목 | 결정 | 이유 |
|------|------|------|
| MVP 인증 | 디바이스 UUID (`X-Device-Token` 헤더) | 1주 개발, 로그인 없음 |
| Post-MVP | 학교 이메일 도메인 제한 | 도메인 검증만 추가하면 됨 |
| JWT | 미사용 | MVP 불필요, 추후 이메일 도입 시 추가 |
| 비밀번호 | 미사용 | MVP 불필요 |

**Spring Security 커스텀 필터**:
```
DeviceTokenAuthenticationFilter
  → X-Device-Token 헤더 추출
  → Redis 캐시 조회 (miss 시 DB 조회)
  → SecurityContext 설정
```

---

## DB 인덱스 전략

```sql
-- 방 목록 조회 (상태 + 생성일)
CREATE INDEX idx_rooms_status_created ON rooms(status, created_at DESC);

-- 방 탐색 필터
CREATE INDEX idx_rooms_filter ON rooms(status, restaurant_category, meeting_type);

-- 지도 범위 조회 (위경도)
CREATE INDEX idx_rooms_location ON rooms(latitude, longitude);

-- 참여자 중복 방지
CREATE UNIQUE INDEX idx_room_participant ON room_participants(room_id, member_id);

-- 닉네임 고유
CREATE UNIQUE INDEX idx_member_nickname ON members(nickname);

-- 디바이스 토큰 고유
CREATE UNIQUE INDEX idx_member_device_token ON members(device_token);
```

---

## Redis 사용 패턴

| 키 패턴 | 용도 | TTL |
|---------|------|-----|
| `auth:device:{token}` | 디바이스 토큰 → memberId 캐시 | 24시간 |
| `room:participants:{roomId}` | 방 참여자 수 캐시 | 1시간 |

---

## Docker Compose 설정 요점

```yaml
services:
  app:
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      retries: 3

  postgres:
    restart: unless-stopped
    volumes:
      - pgdata:/var/lib/postgresql/data

  redis:
    restart: unless-stopped
```
