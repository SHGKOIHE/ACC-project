/**
 * cold_start.js — Lambda Cold Start 대응 테스트
 *
 * 시나리오: 서버리스 첫 요청 지연 문제 측정 및 Provisioned Concurrency 효과 비교
 *
 * 측정 포인트:
 *   1. 콜드 스타트 지연: Lambda 컨테이너가 없을 때 첫 요청 응답시간
 *   2. 웜 응답:         컨테이너 재사용 시 응답시간
 *   3. 비교 지표:       Provisioned Concurrency 적용 전후 차이
 *
 * 테스트 전략:
 *   Phase 1 — 콜드 스타트 유발:
 *     VU 1명씩 순차 투입 (컨테이너 초기화 상태 강제)
 *     각 VU 사이 충분한 간격으로 별도 컨테이너 사용 유도
 *
 *   Phase 2 — 웜 응답 측정:
 *     동일 VU가 연속 요청 (컨테이너 재사용)
 *
 *   Phase 3 — 동시 콜드 스타트:
 *     짧은 시간에 많은 VU 투입 (신규 컨테이너 동시 초기화)
 *
 * 실행:
 *   # 기본 (콜드 스타트 측정)
 *   k6 run -e BASE_URL=http://localhost:8080 k6/scenarios/cold_start.js
 *
 *   # Lambda URL 직접 테스트 (Provisioned Concurrency 없음)
 *   k6 run -e BASE_URL=https://xxx.lambda-url.ap-northeast-2.on.aws \
 *     k6/scenarios/cold_start.js --out json=cold_start_before.json
 *
 *   # Provisioned Concurrency 활성화 후 동일 테스트 (결과 비교)
 *   k6 run -e BASE_URL=https://xxx.lambda-url.ap-northeast-2.on.aws \
 *     k6/scenarios/cold_start.js --out json=cold_start_after.json
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { generateNickname, uuidv4 } from './auth.js';

const BASE_URL = __ENV.BASE_URL || 'https://40ocxlcwfl.execute-api.ap-northeast-2.amazonaws.com';

// ── 커스텀 메트릭 ────────────────────────────────────────────
const coldStartLatency   = new Trend('cold_start_p1_latency', true);   // Phase 1 첫 요청
const warmLatency        = new Trend('cold_start_p2_warm_latency', true); // Phase 2 웜 요청
const concurrentColdLat  = new Trend('cold_start_p3_concurrent', true); // Phase 3 동시 콜드
const coldStartCount     = new Counter('cold_start_detected');          // 콜드 스타트 감지 횟수
const errorRate          = new Rate('cold_start_error_rate');

// 콜드 스타트 판별 임계값 (ms) — 웜 응답 대비 2배 이상이면 콜드로 간주
const COLD_START_THRESHOLD_MS = 1000;

// ── 시나리오 옵션 ────────────────────────────────────────────
export const options = {
  scenarios: {
    // Phase 1: 콜드 스타트 유발 (1명씩 순차 투입)
    phase1_cold: {
      executor:    'per-vu-iterations',
      vus:         10,          // 10명 각각 1번씩 (각각 다른 Lambda 컨테이너)
      iterations:  1,
      maxDuration: '2m',
      exec:        'phase1Cold',
      tags:        { phase: 'cold_start' },
    },

    // Phase 2: 웜 응답 측정 (동일 VU 연속 요청)
    phase2_warm: {
      executor:    'constant-vus',
      vus:         5,
      duration:    '60s',
      startTime:   '90s',       // Phase 1 완료 후 시작
      exec:        'phase2Warm',
      tags:        { phase: 'warm' },
    },

    // Phase 3: 동시 콜드 스타트 (급격한 트래픽 증가)
    phase3_concurrent: {
      executor:    'ramping-vus',
      startVUs:    0,
      stages: [
        { duration: '5s',   target: 30 },  // 5초 만에 30명 투입
        { duration: '30s',  target: 30 },  // 30초 유지
        { duration: '5s',   target: 0  },  // 빠른 종료
      ],
      startTime:   '180s',      // Phase 2 완료 후
      exec:        'phase3Concurrent',
      tags:        { phase: 'concurrent_cold' },
    },
  },

  thresholds: {
    http_req_failed:         ['rate<0.05'],
    cold_start_error_rate:   ['rate<0.05'],
    // 웜 응답은 빨라야 함
    cold_start_p2_warm_latency: ['p(95)<500'],
    // 콜드 스타트 허용치 (Provisioned Concurrency 없을 때 10초 허용)
    cold_start_p1_latency:   ['p(95)<10000'],
  },
};

// ── 유틸 ─────────────────────────────────────────────────────
function register() {
  const nickname    = generateNickname();
  const deviceToken = uuidv4();
  const res = http.post(
    `${BASE_URL}/api/auth/register`,
    JSON.stringify({ nickname, deviceToken }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'register' }, timeout: '15s' },
  );
  if (res.status !== 201) return null;
  return { nickname, deviceToken };
}

function authHeaders(token) {
  return { 'Content-Type': 'application/json', 'X-Device-Token': token };
}

// 단순 API 호출 + 응답시간 반환
function callApi(url, headers, tagName) {
  const start = Date.now();
  const res = http.get(url, {
    headers,
    tags:    { name: tagName },
    timeout: '30s',
  });
  return { res, duration: Date.now() - start };
}

// ── Phase 1: 콜드 스타트 측정 ─────────────────────────────────
// 각 VU가 1번만 실행 → Lambda 신규 컨테이너 초기화 유도
export function phase1Cold() {
  const member = register();
  if (!member) { sleep(1); return; }
  const hdrs = authHeaders(member.deviceToken);

  // VU 간 간격 벌려서 각각 다른 컨테이너 사용 유도
  sleep(__VU * 0.5);

  group('[Phase1] 콜드 스타트 측정', () => {
    const { res, duration } = callApi(`${BASE_URL}/api/rooms`, hdrs, 'phase1_first_req');

    const ok = check(res, { 'Phase1 응답 200': (r) => r.status === 200 });
    errorRate.add(!ok ? 1 : 0);
    coldStartLatency.add(duration);

    // 콜드 스타트 감지 (임계값 초과)
    if (duration > COLD_START_THRESHOLD_MS) {
      coldStartCount.add(1);
      // console.log(`[Cold Start] VU=${__VU} | ${duration}ms (> ${COLD_START_THRESHOLD_MS}ms 임계값)`);
    }
  });
}

// ── Phase 2: 웜 응답 측정 ─────────────────────────────────────
// 동일 VU가 연속 요청 → 컨테이너 재사용 (웜 응답)
export function phase2Warm() {
  const member = register();
  if (!member) { sleep(1); return; }
  const hdrs = authHeaders(member.deviceToken);

  group('[Phase2] 웜 응답 측정', () => {
    // 첫 요청 (이 VU의 컨테이너도 처음일 수 있음 — 제외하지 않음)
    const first = callApi(`${BASE_URL}/api/rooms`, hdrs, 'phase2_first');
    check(first.res, { 'Phase2 첫 요청 200': (r) => r.status === 200 });
    errorRate.add(first.res.status !== 200 ? 1 : 0);

    sleep(0.2);

    // 후속 요청 (웜 컨테이너 재사용)
    const second = callApi(`${BASE_URL}/api/auth/me`, hdrs, 'phase2_warm');
    const ok2 = check(second.res, { 'Phase2 웜 200': (r) => r.status === 200 });
    errorRate.add(!ok2 ? 1 : 0);
    warmLatency.add(second.duration);
  });

  sleep(1);
}

// ── Phase 3: 동시 콜드 스타트 ────────────────────────────────
// 짧은 시간에 많은 VU 투입 → Lambda 동시 컨테이너 초기화
export function phase3Concurrent() {
  const member = register();
  if (!member) { sleep(0.5); return; }
  const hdrs = authHeaders(member.deviceToken);

  group('[Phase3] 동시 콜드 스타트', () => {
    // 방 목록 + 내 정보 동시 요청 (Lambda 동시 실행 유도)
    const batchStart = Date.now();
    const [roomsRes, meRes] = http.batch([
      {
        method: 'GET',
        url:    `${BASE_URL}/api/rooms`,
        params: { headers: hdrs, tags: { name: 'phase3_rooms' }, timeout: '30s' },
      },
      {
        method: 'GET',
        url:    `${BASE_URL}/api/auth/me`,
        params: { headers: hdrs, tags: { name: 'phase3_me' }, timeout: '30s' },
      },
    ]);
    const batchDuration = Date.now() - batchStart;

    const ok1 = check(roomsRes, { 'Phase3 방목록 200': (r) => r.status === 200 });
    const ok2 = check(meRes,    { 'Phase3 내정보 200': (r) => r.status === 200 });
    errorRate.add((!ok1 || !ok2) ? 1 : 0);
    concurrentColdLat.add(batchDuration);

    if (batchDuration > COLD_START_THRESHOLD_MS) {
      coldStartCount.add(1);
    }
  });

  sleep(1);
}

// 단독 실행 시 default — Phase 1 실행
export default phase1Cold;
