/**
 * baseline.js — 기본 부하 테스트
 *
 * 합격 기준:
 *   - 동시 접속자 50명 / 5분 지속
 *   - 평균 응답시간 500ms 이하
 *   - 오류율 1% 미만
 *
 * 실행:
 *   k6 run -e BASE_URL=http://localhost:8080 k6/scenarios/baseline.js
 *   k6 run -e BASE_URL=https://40ocxlcwfl.execute-api.ap-northeast-2.amazonaws.com \
 *     --summary-export=reports/k6/baseline-summary.json \
 *     --out json=reports/k6/baseline-raw.json \
 *     k6/scenarios/baseline.js
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { generateNickname, uuidv4 } from './auth.js';

const BASE_URL = __ENV.BASE_URL || 'https://40ocxlcwfl.execute-api.ap-northeast-2.amazonaws.com';

// ── 커스텀 메트릭 ────────────────────────────────────────────
const errorRate      = new Rate('baseline_error_rate');
const registerCount  = new Counter('baseline_registers');
const roomViewCount  = new Counter('baseline_room_views');
const avgLatency     = new Trend('baseline_latency', true);

// ── 시나리오 옵션 ────────────────────────────────────────────
export const options = {
  scenarios: {
    baseline_load: {
      executor: 'constant-vus',
      vus:      50,
      duration: '5m',
      tags:     { scenario: 'baseline_load' },
    },
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  thresholds: {
    // 합격 기준
    http_req_duration:    ['avg<500'],                // 평균 응답시간 500ms 이하
    http_req_failed:      ['rate<0.01'],              // 오류율 1% 미만
    baseline_error_rate:  ['rate<0.01'],
    // 전체 플로우(5개 API + sleep) 합산 시간 — cold start 포함 20s 허용
    baseline_latency:     ['p(95)<20000'],
  },
};

// ── 유틸 ─────────────────────────────────────────────────────
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
  return {
    nickname,
    deviceToken,
  };
}

function authHeaders(token) {
  return { 'Content-Type': 'application/json', 'X-Device-Token': token };
}

// ── 메인 시나리오 ─────────────────────────────────────────────
// 실제 사용자 행동 모방:
//   1. 회원가입 (토큰 발급)
//   2. 방 목록 조회 (필터 2종)
//   3. 방 상세 조회 1건
//   4. 내 정보 조회
export default function () {
  const start = Date.now();

  // Step 1: 회원가입
  const member = register();
  if (!member) { sleep(1); return; }
  const hdrs = authHeaders(member.deviceToken);

  sleep(0.5);

  // Step 2: 방 목록 조회 (순차: Lambda 동시 실행 한도 10 초과 방지)
  group('방 목록 조회', () => {
    const resAll = http.get(
      `${BASE_URL}/api/rooms`,
      { headers: hdrs, tags: { name: 'room_list_all' } },
    );
    sleep(0.1);
    const resDelivery = http.get(
      `${BASE_URL}/api/rooms?meetingType=DELIVERY`,
      { headers: hdrs, tags: { name: 'room_list_delivery' } },
    );

    const ok1 = check(resAll,      { '전체 목록 200':    (r) => r.status === 200 });
    const ok2 = check(resDelivery, { '배달 목록 200':    (r) => r.status === 200 });
    errorRate.add(!ok1 ? 1 : 0);
    errorRate.add(!ok2 ? 1 : 0);

    // Step 3: 목록에서 방 1개 상세 조회
    let roomId = null;
    try {
      const rooms = JSON.parse(resAll.body).data;
      if (Array.isArray(rooms) && rooms.length > 0) {
        roomViewCount.add(rooms.length);
        roomId = rooms[Math.floor(Math.random() * rooms.length)].id;
      }
    } catch (_) {}

    if (roomId) {
      sleep(0.2);
      const detailRes = http.get(
        `${BASE_URL}/api/rooms/${roomId}`,
        { headers: hdrs, tags: { name: 'room_detail' } },
      );
      const ok3 = check(detailRes, { '방 상세 200': (r) => r.status === 200 });
      errorRate.add(!ok3 ? 1 : 0);
    }
  });

  sleep(0.5);

  // Step 4: 내 정보 조회
  group('내 정보', () => {
    const meRes = http.get(
      `${BASE_URL}/api/auth/me`,
      { headers: hdrs, tags: { name: 'auth_me' } },
    );
    const ok = check(meRes, { '내 정보 200': (r) => r.status === 200 });
    errorRate.add(!ok ? 1 : 0);
  });

  avgLatency.add(Date.now() - start);
  sleep(1);
}
