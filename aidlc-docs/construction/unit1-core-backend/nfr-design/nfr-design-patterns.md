# NFR Design Patterns — Unit 1: Core Backend
# 배달비 절약을 위한 음식 공동 구매 앱

**작성일**: 2026-05-24
**단계**: CONSTRUCTION — NFR Design

---

## 1. 인증 패턴 — Spring Security Filter 체인

### DeviceTokenAuthenticationFilter

```
HTTP Request
    │
    ▼
DeviceTokenAuthenticationFilter (OncePerRequestFilter)
    ├─ X-Device-Token 헤더 추출
    ├─ Redis 캐시 조회 (auth:device:{token})
    │   ├─ Hit  → memberId 획득
    │   └─ Miss → DB 조회 → Redis 캐시 저장 (TTL 24h)
    ├─ Member 없음 → 401 Unauthorized
    └─ SecurityContext에 MemberPrincipal 설정
    │
    ▼
Controller (@AuthenticationPrincipal MemberPrincipal)
```

**구현 포인트**:
- `UsernamePasswordAuthenticationFilter` 앞에 배치
- 공개 API (`/api/auth/register`, `/actuator/health`) 는 필터 스킵
- Redis 캐시 키: `auth:device:{deviceToken}` → value: `memberId`
- 토큰 등록 시 캐시 PUT, 닉네임 변경 시 캐시 무효화

---

## 2. 동시성 패턴 — 낙관적 락 (방 참여)

### @Version 기반 낙관적 락

```java
@Entity
public class Room {
    @Version
    private Long version;  // 충돌 감지용

    private int currentParticipantCount;
}
```

**방 참여 플로우**:
```
1. Room 조회 (version 포함)
2. currentCount < maxParticipants 검증
3. RoomParticipant INSERT
4. Room.currentCount++ UPDATE
   └─ 동시 요청 시 OptimisticLockException 발생
      → 409 Conflict 반환 (재시도 없음, 클라이언트에서 처리)
```

**선택 근거**: 300명 이하 저경합 환경에서 충돌 빈도 낮음. 구현 단순, 추가 인프라 불필요.

---

## 3. 스케줄링 패턴 — @Scheduled 폴링 (자동 마감)

```java
@Scheduled(fixedDelay = 60_000)  // 1분마다
public void autoCloseExpiredRooms() {
    int updated = roomRepository.closeExpiredRooms(LocalDateTime.now());
    // UPDATE rooms SET status='CLOSED'
    // WHERE status='OPEN' AND closed_at <= :now
}
```

**Race Condition 방지**:
```sql
UPDATE rooms
SET status = 'CLOSED', updated_at = NOW()
WHERE status = 'OPEN'
  AND closed_at <= :now
```
- `WHERE status = 'OPEN'` 조건이 중복 전환 차단
- 미니PC 재시작 후에도 DB 기준으로 재처리 가능 (이벤트 유실 없음)

---

## 4. 에러 처리 패턴 — GlobalExceptionHandler

### 에러 응답 형식

```json
{
  "code": "ROOM_NOT_FOUND",
  "message": "방을 찾을 수 없습니다",
  "timestamp": "2026-05-24T12:00:00Z",
  "path": "/api/rooms/999"
}
```

### 에러 코드 체계

| HTTP | 코드 | 상황 |
|------|------|------|
| 400 | `INVALID_INPUT` | Bean Validation 실패 |
| 401 | `UNAUTHORIZED` | 디바이스 토큰 없음/불일치 |
| 403 | `FORBIDDEN` | 권한 없음 (타인 리소스) |
| 404 | `MEMBER_NOT_FOUND` | 회원 없음 |
| 404 | `ROOM_NOT_FOUND` | 방 없음 |
| 409 | `NICKNAME_DUPLICATE` | 닉네임 중복 |
| 409 | `ROOM_FULL` | 방 인원 초과 |
| 409 | `ALREADY_JOINED` | 이미 참여한 방 |
| 409 | `ROOM_STATUS_INVALID` | 잘못된 상태 전이 |
| 409 | `OPTIMISTIC_LOCK` | 동시 접근 충돌 |
| 500 | `INTERNAL_ERROR` | 서버 오류 (스택트레이스 미노출) |

### GlobalExceptionHandler 구조

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e, HttpServletRequest req) {
        // code, message, timestamp, path 포함
        // 스택트레이스 미포함 (SECURITY-15)
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(...) { ... }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(...) {
        // INTERNAL_ERROR, 상세 메시지 미노출
    }
}
```

---

## 5. 캐싱 패턴 — Redis

### 키 설계

| 키 | 타입 | TTL | 용도 |
|----|------|-----|------|
| `auth:device:{token}` | String (memberId) | 24h | 인증 캐시 |
| `room:count:{roomId}` | String (int) | 1h | 현재 참여자 수 캐시 |

### 캐시 무효화 시점

| 이벤트 | 무효화 키 |
|--------|----------|
| 방 참여 / 탈퇴 | `room:count:{roomId}` |
| 닉네임 변경 (Post-MVP) | `auth:device:{token}` |

---

## 6. 보안 패턴

### AES-256-GCM 계좌번호 암호화

```java
@Component
public class AesEncryptor {
    // 키: 환경변수 ENCRYPTION_KEY (Base64 32바이트)

    public String encrypt(String plaintext) { ... }
    public String decrypt(String ciphertext) { ... }
}
```

- `Room.accountNumber` 저장 시 `encrypt()`, 조회 시 `decrypt()`
- JPA `@Convert(converter = AesAttributeConverter.class)` 적용으로 자동 처리

### 로그 마스킹

```java
// Logback PatternLayout 커스텀
// 계좌번호 패턴: [0-9]{10,14} → "****"
// 디바이스 토큰: UUID 패턴 → 앞 8자리만 표시
```
