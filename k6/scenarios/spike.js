/**
 * spike.js — 스파이크 테스트
 *
 * 시나리오: 공지 올라가서 사람들이 한꺼번에 몰리는 상황
 *   평상시 10명 → 갑자기 200명으로 급증 → 다시 10명으로 감소
 *
 * 합격 기준:
 *   - 스파이크 발생 후 60초 이내 정상 응답 복귀
 *   - 스파이크 중 오류율 5% 미만
 *   - Auto Scaling / Lambda 동시성으로 대응 확인
 *
 * 측정 포인트:
 *   - 스파이크 구간 p95 응답시간
 *   - 스파이크 해소 후 응답시간 복귀 속도
 *   - 오류율 변화
 *
 * 실행:
 *   k6 run -e BASE_URL=http://localhost:8080 k6/scenarios/spike.js
 *   k6 run -e BASE_URL=http://your-lambda-url k6/scenarios/spike.js --out json=spike_result.json
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import exec from 'k6/execution';
import { generateNickname, uuidv4 } from './auth.js';

const BASE_URL = __ENV.BASE_URL || 'https://40ocxlcwfl.execute-api.ap-northeast-2.amazonaws.com';

// ── 커스텀 메트릭 ────────────────────────────────────────────
const spikeErrorRate        = new Rate('spike_error_rate');          // 전체 스파이크 테스트 오류율
const spikePeakErrorRate    = new Rate('spike_peak_error_rate');     // 스파이크 정점 구간 오류율
const spikeRecoveryErrRate  = new Rate('spike_recovery_error_rate'); // 복구 구간 오류율
const recoveryLatency       = new Trend('spike_recovery_latency', true); // 복구 후 응답시간
const spikeLatency          = new Trend('spike_peak_latency', true); // 스파이크 정점 응답시간
const timeoutCount          = new Counter('spike_timeouts');

// ── 시나리오 타임라인 ──────────────────────────────────────
//   0s  ~ 60s  : 평상시 (10명)
//   60s ~ 80s  : 스파이크 진입 (10→200명)
//   80s ~ 140s : 스파이크 정점 (200명) — 합격 기준 검증 구간
//   140s~ 160s : 스파이크 해소 (200→10명)
//   160s~ 220s : 복구 확인 (10명) — 60초 이내 정상 응답 복귀 확인
//   220s~ 240s : 쿨다운 (10→0명)

export const options = {
  scenarios: {
    spike_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s',  target: 10  }, // 워밍업
        { duration: '30s',  target: 10  }, // 평상시 유지
        { duration: '20s',  target: 200 }, // 스파이크 급증 (공지 발행)
        { duration: '60s',  target: 200 }, // 스파이크 정점 유지
        { duration: '20s',  target: 10  }, // 스파이크 해소
        { duration: '60s',  target: 10  }, // 복구 구간 (60초 이내 복귀 확인)
        { duration: '20s',  target: 0   }, // 쿨다운
      ],
      gracefulRampDown: '30s',
    },
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],

  thresholds: {
    // 전체 오류율: 스파이크 포함 5% 미만
    http_req_failed:      ['rate<0.05'],
    spike_error_rate:     ['rate<0.05'],
    spike_peak_error_rate: ['rate<0.05'],

    // 스파이크 정점 구간 p95 허용치 (일시 완화)
    spike_peak_latency:   ['p(95)<2000'],

    // 복구 후 정상 응답시간 (500ms 이내)
    spike_recovery_latency: ['p(95)<500'],
    spike_recovery_error_rate: ['rate<0.01'],

    // 전체 요청 p99 3초 이내 (극단 지연 방지)
    http_req_duration:    ['p(99)<3000'],
  },
};

// ── VU 상태 추적 (스파이크 구간 구분) ────────────────────────
// k6 스크립트 시작 기준 시각 (ms)
const SPIKE_START_MS   = (30 + 30) * 1000;           // 60s
const SPIKE_END_MS     = (30 + 30 + 20 + 60) * 1000; // 140s
const RECOVERY_END_MS  = SPIKE_END_MS + 20 * 1000 + 60 * 1000; // 220s

function currentPhase() {
  const elapsed = Date.now() - exec.scenario.startTime;
  if (elapsed >= SPIKE_START_MS && elapsed < SPIKE_END_MS) return 'spike';
  if (elapsed >= SPIKE_END_MS && elapsed < RECOVERY_END_MS) return 'recovery';
  return 'normal';
}

function recordOutcome(phase, response, okStatus) {
  const failed = !okStatus || response.status === 0;
  spikeErrorRate.add(failed ? 1 : 0);
  if (phase === 'spike') {
    spikePeakErrorRate.add(failed ? 1 : 0);
  } else if (phase === 'recovery') {
    spikeRecoveryErrRate.add(failed ? 1 : 0);
  }
  if (response.status === 0) timeoutCount.add(1);
}

function recordLatency(phase, response) {
  if (phase === 'spike') {
    spikeLatency.add(response.timings.duration);
  } else if (phase === 'recovery') {
    recoveryLatency.add(response.timings.duration);
  }
}

// ── 유틸 ─────────────────────────────────────────────────────
function register() {
  const nickname    = generateNickname();
  const deviceToken = uuidv4();
  const res = http.post(
    `${BASE_URL}/api/auth/register`,
    JSON.stringify({ nickname, deviceToken }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags:    { name: 'register' },
      timeout: '5s',
    },
  );
  const ok = check(res, { '회원가입 201': (r) => r.status === 201 });
  recordOutcome(currentPhase(), res, ok);
  if (!ok) return null;
  return { nickname, deviceToken };
}

function authHeaders(token) {
  return { 'Content-Type': 'application/json', 'X-Device-Token': token };
}

// ── 메인 시나리오 ─────────────────────────────────────────────
// 공지 게시 상황 재현:
//   모든 사용자가 앱을 열고 → 방 목록 조회 → 방 상세 확인
export default function () {
  const phase = currentPhase();

  // Step 1: 회원가입 (토큰 발급)
  const member = register();
  if (!member) { sleep(0.5); return; }
  const hdrs = authHeaders(member.deviceToken);

  sleep(0.2);

  // Step 2: 방 목록 조회 (스파이크 핵심 부하)
  group('방 목록 조회 (스파이크)', () => {
    const listRes = http.get(
      `${BASE_URL}/api/rooms`,
      { headers: hdrs, tags: { name: 'room_list_spike' }, timeout: '10s' },
    );

    const ok = check(listRes, {
      '방 목록 200':       (r) => r.status === 200,
      '응답 500ms 이내':   (r) => r.timings.duration < 500,
    });

    // 오류율은 HTTP 성공 여부로만 집계한다. 500ms 초과는 응답시간 지표로 별도 판단.
    const okStatus = listRes.status === 200;
    recordOutcome(phase, listRes, okStatus);
    recordLatency(phase, listRes);

    // Step 3: 방 상세 조회 (목록에서 랜덤 선택)
    let roomId = null;
    try {
      const rooms = JSON.parse(listRes.body).data;
      if (Array.isArray(rooms) && rooms.length > 0) {
        roomId = rooms[Math.floor(Math.random() * rooms.length)].id;
      }
    } catch (_) {}

    if (roomId) {
      sleep(0.1);
      const detailRes = http.get(
        `${BASE_URL}/api/rooms/${roomId}`,
        { headers: hdrs, tags: { name: 'room_detail_spike' }, timeout: '10s' },
      );
      const detailOk = check(detailRes, { '방 상세 200': (r) => r.status === 200 });
      recordOutcome(phase, detailRes, detailOk);
      recordLatency(phase, detailRes);
    }
  });

  // 스파이크 중에는 실제 사용자보다 짧은 sleep (폭발적 접속 재현)
  const sleepTime = phase === 'spike' ? 0.5 : 1.5;
  sleep(sleepTime);
}
