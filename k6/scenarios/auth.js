/**
 * auth.js — 회원가입 및 인증 시나리오
 *
 * 테스트 대상:
 *   - POST /api/auth/register  : 닉네임으로 회원가입 → deviceToken 발급
 *   - GET  /api/auth/me        : 내 정보 조회
 *
 * 실행 예시:
 *   k6 run scenarios/auth.js
 *   k6 run -e BASE_URL=http://192.168.0.10:8080 scenarios/auth.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

// ── 환경변수 ──────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'https://40ocxlcwfl.execute-api.ap-northeast-2.amazonaws.com';

// ── 커스텀 메트릭 ─────────────────────────────────────────
const registerSuccess = new Counter('auth_register_success');
const registerFail    = new Counter('auth_register_fail');
const meLatency       = new Trend('auth_me_latency', true);

// ── 유틸: 클라이언트 UUID 생성 (모바일 앱과 동일한 방식) ──
export function uuidv4() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = Math.random() * 16 | 0;
    return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
  });
}

// ── 시나리오 옵션 ─────────────────────────────────────────
export const options = {
  scenarios: {
    auth_smoke: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 5  }, // 워밍업
        { duration: '20s', target: 20 }, // 부하 증가
        { duration: '20s', target: 20 }, // 유지
        { duration: '10s', target: 0  }, // 쿨다운
      ],
    },
  },
  thresholds: {
    http_req_duration:           ['p(95)<500'],
    http_req_failed:             ['rate<0.01'],
    auth_me_latency:             ['p(95)<400'],
  },
};

// ── 실행마다 고유한 3자리 stamp (테스트 간 닉네임 충돌 방지) ──
// Date.now()를 base-36으로 변환 → 마지막 3자리만 사용 (영숫자 조합)
const RUN_STAMP = Date.now().toString(36).slice(-3).toUpperCase();

// ── 유틸: 랜덤 닉네임 생성 ───────────────────────────────
export function generateNickname() {
  // 형식: R + stamp(3) + VU(1-3) + x + ITER(1-3) = 최대 11자
  // 예시: RK9R1x0 / RK9R50x99 / RK9R200x288
  // 테스트 실행마다 RUN_STAMP가 달라져 이전 실행과 충돌 없음
  return `R${RUN_STAMP}${__VU}x${__ITER}`;
}

// ── 메인 시나리오 함수 ────────────────────────────────────
export default function () {
  const nickname    = generateNickname();
  const deviceToken = uuidv4(); // 모바일 앱이 Crypto.randomUUID()로 생성하는 것과 동일
  const headers     = { 'Content-Type': 'application/json' };

  // ── Step 1: 회원가입 (nickname + clientDeviceToken 전송) ─
  const registerRes = http.post(
    `${BASE_URL}/api/auth/register`,
    JSON.stringify({ nickname, deviceToken }),
    { headers, tags: { name: 'auth_register' } },
  );

  const registered = check(registerRes, {
    '회원가입 201 응답':   (r) => r.status === 201,
    '응답에 memberId 존재': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.data && typeof body.data.id === 'string' && body.data.id.length > 0;
      } catch (_) {
        return false;
      }
    },
  });

  if (registered) {
    registerSuccess.add(1);
  } else {
    registerFail.add(1);
    sleep(1);
    return;
  }

  // deviceToken은 클라이언트가 생성한 값을 그대로 사용 (응답에 포함되지 않음)
  const authHeaders = {
    'Content-Type':  'application/json',
    'X-Device-Token': deviceToken,
  };

  sleep(0.3); // 짧은 대기 (실제 사용자 행동 모방)

  // ── Step 2: 내 정보 조회 ──────────────────────────────
  const meStart = Date.now();
  const meRes = http.get(
    `${BASE_URL}/api/auth/me`,
    { headers: authHeaders, tags: { name: 'auth_me' } },
  );
  meLatency.add(Date.now() - meStart);

  check(meRes, {
    '내 정보 조회 200 응답':       (r) => r.status === 200,
    '응답 닉네임이 가입 닉네임과 일치': (r) => {
      try {
        return JSON.parse(r.body).data.nickname === nickname;
      } catch (_) {
        return false;
      }
    },
  });

  sleep(1);
}
