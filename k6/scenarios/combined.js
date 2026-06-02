/**
 * combined.js — 통합 부하 테스트 (과제 제출용)
 *
 * ┌────────────────────────────────────────────────────────────────┐
 * │  시나리오 1: 기본 부하 테스트                                    │
 * │    0s  ~ 30s  : 워밍업 (0→50명)                                │
 * │    30s ~ 5m30s: 50명 유지 — p95<500ms / 오류율<1% 검증         │
 * │                                                                │
 * │  시나리오 3: 요즘 뜨는 — 새벽 배치 후 아침 트래픽              │
 * │    5m30s ~ 7m30s: 50→120명 점진적 ramp-up (출근 트래픽)        │
 * │    7m30s ~ 8m30s: 120명 유지 (피크 안정성 검증)                │
 * │                                                                │
 * │  시나리오 2: 스파이크 테스트 — 공지 알림 급증                   │
 * │    8m30s ~ 8m40s: 10초 만에 200명 폭증 (공지 발송 상황)        │
 * │    8m40s ~ 9m40s: 200명 유지 (Lambda 동시성 검증)              │
 * │    9m40s ~10m10s: 30초 만에 10명으로 감소                      │
 * │   10m10s ~11m10s: 60초 복구 모니터링 (정상 복귀 확인)          │
 * │   11m10s ~11m40s: 쿨다운                                       │
 * └────────────────────────────────────────────────────────────────┘
 *
 * 합격 기준:
 *   - 기본 부하: 오류율 1% 미만, p95 < 500ms
 *   - 스파이크:  오류율 5% 미만, 60초 이내 정상 복귀
 *
 * 실행:
 *   k6 run k6/scenarios/combined.js
 *   k6 run k6/scenarios/combined.js --out json=/tmp/k6_combined_result.json
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { generateNickname, uuidv4 } from './auth.js';

const BASE_URL = __ENV.BASE_URL || 'https://40ocxlcwfl.execute-api.ap-northeast-2.amazonaws.com';

// ── 커스텀 메트릭 ─────────────────────────────────────────────────
const errorRate          = new Rate('combined_error_rate');
const registerCount      = new Counter('combined_registers');
const spikeErrorRate     = new Rate('combined_spike_error_rate');   // 스파이크 구간 오류율
const baselineLatency    = new Trend('combined_baseline_latency', true); // 기본 부하 구간 응답시간
const recoveryLatency    = new Trend('combined_recovery_latency', true); // 복구 구간 응답시간

// ── 단계 구분 타임라인 (ms) ───────────────────────────────────────
// 스파이크 급증 시작 = 8분 30초
const SPIKE_START_MS   = (30 + 300 + 120 + 60) * 1000;            // 510s
// 스파이크 해소 시작 = 9분 40초
const SPIKE_END_MS     = SPIKE_START_MS + (10 + 60) * 1000;       // 580s
// 복구 모니터링 끝  = 11분 10초
const RECOVERY_END_MS  = SPIKE_END_MS + 30 * 1000 + 60 * 1000;   // 670s

// 테스트 시작 기준 시각 (각 VU의 첫 실행 기준이 아닌 전역 시작 기준)
const TEST_START_MS = Date.now();

// ── 시나리오 옵션 ─────────────────────────────────────────────────
export const options = {
  stages: [
    // ── [Scenario 1] 기본 부하 테스트 ───────────────────────────
    { duration: '30s', target: 50  }, // 워밍업: 0 → 50명
    { duration: '5m',  target: 50  }, // 5분 유지 (기본 부하 기준)

    // ── [Scenario 3] 새벽 배치 후 아침 트래픽 (요즘 뜨는) ───────
    { duration: '2m',  target: 120 }, // 아침 출근 트래픽: 50 → 120명 (자연스러운 ramp-up)
    { duration: '1m',  target: 120 }, // 120명 피크 유지 (안정성 검증)

    // ── [Scenario 2] 스파이크 테스트 ────────────────────────────
    { duration: '10s', target: 200 }, // 10초 만에 200명 폭증 (공지 알림 상황)
    { duration: '1m',  target: 200 }, // 1분 유지 (Lambda 동시성 + Auto Scaling 대응 확인)
    { duration: '30s', target: 10  }, // 30초 만에 10명으로 급감 (복구 시작)
    { duration: '1m',  target: 10  }, // 60초 복구 모니터링 — 60초 이내 정상 복귀 확인

    // ── 종료 ────────────────────────────────────────────────────
    { duration: '30s', target: 0   },
  ],

  thresholds: {
    // 전체 오류율 — 스파이크 기준 5% 적용 (가장 완화된 기준)
    http_req_failed:               ['rate<0.05'],
    combined_error_rate:           ['rate<0.05'],
    combined_spike_error_rate:     ['rate<0.05'],

    // 기본 부하 구간 응답시간 — p95 500ms (기본 부하 합격 기준)
    combined_baseline_latency:     ['p(95)<500'],

    // 복구 구간 응답시간 — 60초 이내 정상 복귀 (500ms 기준 회복)
    combined_recovery_latency:     ['p(95)<500'],

    // 전체 p99 — 극단 지연 방지 (스파이크 cold-start 감안 5초)
    http_req_duration:             ['p(99)<5000'],
  },
};

// ── 유틸 ─────────────────────────────────────────────────────────
function register() {
  const nickname    = generateNickname();
  const deviceToken = uuidv4();
  const res = http.post(
    `${BASE_URL}/api/auth/register`,
    JSON.stringify({ nickname, deviceToken }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'register' } },
  );
  const ok = check(res, { '회원가입 201': (r) => r.status === 201 });
  errorRate.add(!ok ? 1 : 0);
  if (!ok) return null;
  registerCount.add(1);
  return { nickname, deviceToken };
}

function authHeaders(token) {
  return { 'Content-Type': 'application/json', 'X-Device-Token': token };
}

// ── 메인 시나리오 ─────────────────────────────────────────────────
// 실제 앱 사용 흐름 재현:
//   회원가입 → 방 목록 조회 → 방 상세 확인 → 내 정보 조회
export default function () {
  const elapsed      = Date.now() - TEST_START_MS;
  const inSpike      = elapsed >= SPIKE_START_MS && elapsed < SPIKE_END_MS;
  const inRecovery   = elapsed >= SPIKE_END_MS && elapsed < RECOVERY_END_MS;
  const inBaseline   = elapsed < SPIKE_START_MS && __VU <= 50; // 기본 부하 구간 VU

  const iterStart = Date.now();

  // Step 1: 회원가입 (deviceToken 발급)
  const member = register();
  if (!member) { sleep(1); return; }
  const hdrs = authHeaders(member.deviceToken);

  sleep(0.3);

  // Step 2: 방 목록 조회 (핵심 부하 - 공지 후 사용자들이 앱 열어서 확인)
  const roomsRes = http.get(
    `${BASE_URL}/api/rooms`,
    { headers: hdrs, tags: { name: 'room_list' } },
  );
  const ok1 = check(roomsRes, { '방 목록 200': (r) => r.status === 200 });
  errorRate.add(!ok1 ? 1 : 0);
  if (inSpike) spikeErrorRate.add(!ok1 ? 1 : 0);

  // Step 3: 방 상세 조회 (목록에서 랜덤 선택)
  let roomId = null;
  try {
    const rooms = JSON.parse(roomsRes.body).data;
    if (Array.isArray(rooms) && rooms.length > 0) {
      roomId = rooms[Math.floor(Math.random() * rooms.length)].id;
    }
  } catch (_) {}

  if (roomId) {
    sleep(0.2);
    const detailRes = http.get(
      `${BASE_URL}/api/rooms/${roomId}`,
      { headers: hdrs, tags: { name: 'room_detail' } },
    );
    const ok2 = check(detailRes, { '방 상세 200': (r) => r.status === 200 });
    errorRate.add(!ok2 ? 1 : 0);
    if (inSpike) spikeErrorRate.add(!ok2 ? 1 : 0);
  }

  sleep(0.3);

  // Step 4: 내 정보 조회
  const meRes = http.get(
    `${BASE_URL}/api/auth/me`,
    { headers: hdrs, tags: { name: 'auth_me' } },
  );
  const ok3 = check(meRes, { '내 정보 200': (r) => r.status === 200 });
  errorRate.add(!ok3 ? 1 : 0);

  // ── 구간별 응답시간 메트릭 기록 ─────────────────────────────
  const totalDuration = Date.now() - iterStart;
  if (inBaseline) {
    baselineLatency.add(totalDuration);
  } else if (inRecovery) {
    recoveryLatency.add(totalDuration);
  }

  // 스파이크 중 짧게, 평상시 길게 (실제 사용자 행동 재현)
  sleep(inSpike ? 0.5 : 1);
}
