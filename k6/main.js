/**
 * main.js — 통합 부하 테스트 메인 스크립트
 *
 * ┌──────────────────────────────────────────────────────────────┐
 * │  시나리오 목록                                                 │
 * │                                                              │
 * │  [기본]  baseline     : 50명 × 5분, p95<500ms, 오류<1%       │
 * │  [기본]  auth         : 회원가입/인증 워밍업                    │
 * │  [기본]  room_list    : 방 목록 읽기 집중                       │
 * │  [기본]  room_flow    : 방 생성→참여→주문→정산 E2E              │
 * │                                                              │
 * │  [스파이크] spike     : 10→200→10명, 복구 60초 이내            │
 * │                                                              │
 * │  [트렌드] ai_surge    : Lambda+Bedrock AI 추론 급증            │
 * │  [트렌드] cold_start  : Lambda 콜드 스타트 Provisioned 비교    │
 * └──────────────────────────────────────────────────────────────┘
 *
 * 실행 예시:
 *   # 기본 부하 (50명 × 5분)
 *   k6 run -e BASE_URL=http://localhost:8080 k6/scenarios/baseline.js
 *
 *   # 스파이크 테스트
 *   k6 run -e BASE_URL=http://localhost:8080 k6/scenarios/spike.js --out json=spike.json
 *
 *   # AI 급증 시나리오
 *   k6 run -e BASE_URL=http://localhost:8080 k6/scenarios/ai_surge.js --out json=ai.json
 *
 *   # Cold Start 비교 (Provisioned Concurrency 전후)
 *   k6 run -e BASE_URL=https://your-lambda-url k6/scenarios/cold_start.js --out json=cold_before.json
 *
 *   # 전체 통합 실행 (이 파일)
 *   k6 run -e BASE_URL=http://localhost:8080 k6/main.js --out json=result.json
 *   k6 run -e BASE_URL=http://localhost:8080 k6/main.js --out influxdb=http://localhost:8086/k6
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Trend, Rate } from 'k6/metrics';
import { uuidv4 } from './scenarios/auth.js';

// ── 환경변수 ──────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'https://40ocxlcwfl.execute-api.ap-northeast-2.amazonaws.com';

// ── 커스텀 메트릭 (시나리오 공통) ────────────────────────
const totalRegistered  = new Counter('total_registered');
const totalRoomsViewed = new Counter('total_rooms_viewed');
const totalOrdersAdded = new Counter('total_orders_added');
const apiErrorRate     = new Rate('api_error_rate');

// ── 통합 임계값 (thresholds) ─────────────────────────────
export const options = {
  // 시나리오별 executor 설정
  scenarios: {
    // ── 1. 인증 시나리오: 회원가입 + 내 정보 조회 ────────
    auth_scenario: {
      executor:   'ramping-vus',
      startVUs:   0,
      stages: [
        { duration: '10s', target: 5  },
        { duration: '30s', target: 15 },
        { duration: '20s', target: 15 },
        { duration: '10s', target: 0  },
      ],
      // 이 시나리오에서 실행할 함수
      exec: 'authScenario',
      tags: { scenario: 'auth' },
    },

    // ── 2. 방 목록 읽기 부하: 일정 도착률로 요청 ─────────
    room_list_scenario: {
      executor:          'constant-arrival-rate',
      rate:              20,          // 초당 20회 요청
      timeUnit:          '1s',
      duration:          '60s',
      preAllocatedVUs:   10,          // 사전 할당 VU
      maxVUs:            50,          // 최대 VU
      startTime:         '15s',       // auth 시나리오 워밍업 후 시작
      exec: 'roomListScenario',
      tags: { scenario: 'room_list' },
    },

    // ── 3. 전체 플로우: 방 생성→참여→주문→정산 ───────────
    room_flow_scenario: {
      executor:   'ramping-vus',
      startVUs:   0,
      stages: [
        { duration: '10s', target: 6  }, // 워밍업 (호스트 3 + 참여자 3)
        { duration: '40s', target: 10 }, // 부하 증가 (호스트 5 + 참여자 5)
        { duration: '20s', target: 10 }, // 유지
        { duration: '10s', target: 0  }, // 쿨다운
      ],
      startTime:  '20s',                 // 방 목록에 데이터 쌓인 후 시작
      exec: 'roomFlowScenario',
      tags: { scenario: 'room_flow' },
    },
  },

  // ── 전역 임계값 ─────────────────────────────────────────
  thresholds: {
    // 전체 API 응답 시간 p95 < 500ms
    http_req_duration:          ['p(95)<500'],
    // 전체 실패율 < 1%
    http_req_failed:            ['rate<0.01'],
    // API 에러율 < 1%
    api_error_rate:             ['rate<0.01'],
    // 시나리오별 세분화 임계값
    'http_req_duration{scenario:auth}':      ['p(95)<400'],
    'http_req_duration{scenario:room_list}': ['p(95)<400'],
    'http_req_duration{scenario:room_flow}': ['p(95)<800'],
  },
};

// ── 유틸: 랜덤 닉네임 생성 ───────────────────────────────
export function generateNickname() {
  const prefixes = ['Fast', 'Cool', 'Happy', 'Quick', 'Smart', 'Bold', 'Bright', 'Sharp'];
  const suffixes = ['Tiger', 'Lion', 'Eagle', 'Wolf', 'Bear', 'Fox', 'Hawk', 'Deer'];
  const prefix = prefixes[Math.floor(Math.random() * prefixes.length)];
  const suffix = suffixes[Math.floor(Math.random() * suffixes.length)];
  const num    = Math.floor(Math.random() * 9000) + 1000;
  return `${prefix}${suffix}${num}`; // 예: FastTiger1234
}

// ── 유틸: 회원가입 → { nickname, deviceToken } 반환 ──────
function registerMember() {
  const nickname    = generateNickname();
  const deviceToken = uuidv4();
  const res = http.post(
    `${BASE_URL}/api/auth/register`,
    JSON.stringify({ nickname, deviceToken }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags:    { name: 'register' },
    },
  );

  const ok = check(res, {
    '회원가입 201': (r) => r.status === 201,
  });
  apiErrorRate.add(!ok ? 1 : 0);

  if (!ok) return null;
  totalRegistered.add(1);

  return {
    nickname,
    deviceToken,
  };
}

// ── 유틸: 인증 헤더 빌드 ─────────────────────────────────
function makeHeaders(deviceToken) {
  return {
    'Content-Type':   'application/json',
    'X-Device-Token':  deviceToken,
  };
}

// ── 기본 export (k6 필수, named exec 시나리오 사용 시에도 required) ──
// 직접 `k6 run main.js` 실행 시 이 함수가 기본 진입점이 됨.
// 세 시나리오가 모두 exec 지정되어 있으므로 이 함수는 실제 호출되지 않음.
export default function () {
  authScenario();
}

// ═══════════════════════════════════════════════════════════
// 1. 인증 시나리오 함수
// ═══════════════════════════════════════════════════════════
export function authScenario() {
  group('인증 플로우', () => {
    // ── 회원가입 ──────────────────────────────────────────
    const member = registerMember();
    if (!member) { sleep(1); return; }

    sleep(0.3);

    // ── 내 정보 조회 ───────────────────────────────────────
    const meRes = http.get(
      `${BASE_URL}/api/auth/me`,
      {
        headers: makeHeaders(member.deviceToken),
        tags:    { name: 'auth_me' },
      },
    );

    const meStatus = check(meRes, {
      '내 정보 200':  (r) => r.status === 200,
      '닉네임 일치':  (r) => {
        try { return JSON.parse(r.body).data.nickname === member.nickname; }
        catch (_) { return false; }
      },
    });
    // HTTP 실패(non-2xx)일 때만 에러율 카운트 — 닉네임 불일치는 API 에러가 아님
    apiErrorRate.add(meRes.status < 200 || meRes.status >= 300 ? 1 : 0);
  });

  sleep(1);
}

// ═══════════════════════════════════════════════════════════
// 2. 방 목록 읽기 시나리오 함수
// ═══════════════════════════════════════════════════════════
export function roomListScenario() {
  // 이 시나리오는 읽기 집중 → 매 반복마다 새 토큰 발급 대신
  // 시나리오 내부에서 토큰 캐싱 불가 (k6 VU 상태는 반복 간 유지)
  // 실용적 접근: 첫 반복에서 발급 후 __ITER로 재사용
  let deviceToken = __ENV._CACHED_TOKEN; // 환경변수 캐시는 없으므로 매번 발급

  group('방 목록 읽기', () => {
    const member = registerMember();
    if (!member) { sleep(0.5); return; }
    const headers = makeHeaders(member.deviceToken);

    // ── 여러 필터 조합 병렬 요청 (http.batch) ────────────
    const queries = [
      '',                                              // 전체
      '?meetingType=DELIVERY',                         // 배달
      '?meetingType=TOGETHER',                         // 같이먹기
      '?lat=37.5665&lng=126.9780&radius=3.0',          // 위치 기반
    ];

    // 요청 2개씩 묶어 배치
    const batchA = queries.slice(0, 2).map((q) => ({
      method: 'GET',
      url:    `${BASE_URL}/api/rooms${q}`,
      params: { headers, tags: { name: 'room_list' } },
    }));

    const batchB = queries.slice(2).map((q) => ({
      method: 'GET',
      url:    `${BASE_URL}/api/rooms${q}`,
      params: { headers, tags: { name: 'room_list' } },
    }));

    const resA = http.batch(batchA);
    sleep(0.1);
    const resB = http.batch(batchB);

    const allRes = [...resA, ...resB];
    let collectedIds = [];

    for (const res of allRes) {
      const ok = check(res, { '방 목록 200': (r) => r.status === 200 });
      apiErrorRate.add(!ok ? 1 : 0);
      try {
        const rooms = JSON.parse(res.body).data;
        if (Array.isArray(rooms) && rooms.length > 0) {
          totalRoomsViewed.add(rooms.length);
          collectedIds = collectedIds.concat(rooms.map((r) => r.id));
        }
      } catch (_) { /* 무시 */ }
    }

    sleep(0.2);

    // ── 수집된 방 중 1개 상세 조회 ───────────────────────
    if (collectedIds.length > 0) {
      const rid = collectedIds[Math.floor(Math.random() * collectedIds.length)];
      const detailRes = http.get(
        `${BASE_URL}/api/rooms/${rid}`,
        { headers, tags: { name: 'room_detail' } },
      );
      const detailOk = check(detailRes, {
        '방 상세 200': (r) => r.status === 200,
      });
      apiErrorRate.add(!detailOk ? 1 : 0);
    }
  });

  sleep(0.5);
}

// ═══════════════════════════════════════════════════════════
// 3. 전체 플로우 시나리오 함수 (호스트/참여자 분기)
// ═══════════════════════════════════════════════════════════
export function roomFlowScenario() {
  if (__VU % 2 === 1) {
    _hostFlow();
  } else {
    _participantFlow();
  }
}

// ── 호스트 플로우 ─────────────────────────────────────────
function _hostFlow() {
  group('호스트 플로우', () => {
    // Step 1: 회원가입
    const member = registerMember();
    if (!member) { sleep(1); return; }
    const headers = makeHeaders(member.deviceToken);

    sleep(0.5);

    // Step 2: 방 생성
    const title = `K6_${member.nickname}_${Date.now()}`;
    const createRes = http.post(
      `${BASE_URL}/api/rooms`,
      JSON.stringify({
        title,
        meetingType:        'DELIVERY',
        restaurantName:     'K6부하테스트식당',
        restaurantAddress:  '서울시 강남구 테헤란로 100',
        restaurantCategory: '치킨',
        latitude:           37.5665,
        longitude:          126.9780,
        deliveryFee:        3000,
        maxParticipants:    4,
        accountNumber:      '110-123-456789',
        accountHolder:      member.nickname,
        bankName:           '신한은행',
      }),
      { headers, tags: { name: 'room_create' } },
    );

    const created = check(createRes, {
      '방 생성 201': (r) => r.status === 201,
    });
    apiErrorRate.add(!created ? 1 : 0);
    if (!created) { sleep(1); return; }

    const roomId = JSON.parse(createRes.body).data.id;

    // Step 3: 참여자 입장 대기
    sleep(3);

    // Step 4: 호스트 메뉴 병렬 주문 (http.batch)
    const hostMenus = [
      { menuName: '후라이드치킨 한마리', quantity: 1, price: 18000 },
      { menuName: '양념치킨 반마리',    quantity: 1, price: 10000 },
    ];

    const orderBatch = hostMenus.map((menu) => ({
      method: 'POST',
      url:    `${BASE_URL}/api/rooms/${roomId}/orders`,
      body:   JSON.stringify(menu),
      params: { headers, tags: { name: 'order_add' } },
    }));

    const orderResponses = http.batch(orderBatch);
    for (const res of orderResponses) {
      const ok = check(res, { '호스트 주문 추가 201': (r) => r.status === 201 });
      if (ok) totalOrdersAdded.add(1);
      apiErrorRate.add(!ok ? 1 : 0);
    }

    // Step 5: 참여자 주문 대기
    sleep(2);

    // Step 6: 전체 주문 목록 + 정산 병렬 조회 (http.batch)
    const [listRes, settlementRes] = http.batch([
      {
        method: 'GET',
        url:    `${BASE_URL}/api/rooms/${roomId}/orders`,
        params: { headers, tags: { name: 'order_list' } },
      },
      {
        method: 'GET',
        url:    `${BASE_URL}/api/rooms/${roomId}/orders/settlement`,
        params: { headers, tags: { name: 'settlement_get' } },
      },
    ]);

    check(listRes, { '주문 목록 200': (r) => r.status === 200 });
    check(settlementRes, {
      '정산 조회 200':   (r) => r.status === 200,
      '정산 총액 > 0':   (r) => {
        try { return JSON.parse(r.body).data.totalMenuAmount > 0; }
        catch (_) { return false; }
      },
    });

    sleep(0.5);

    // Step 7: 방 주문 확정 (close)
    const closeRes = http.post(
      `${BASE_URL}/api/rooms/${roomId}/close`,
      null,
      { headers, tags: { name: 'room_close' } },
    );
    const closed = check(closeRes, { '방 확정 204': (r) => r.status === 204 });
    apiErrorRate.add(!closed ? 1 : 0);
  });

  sleep(1);
}

// ── 참여자 플로우 ─────────────────────────────────────────
function _participantFlow() {
  group('참여자 플로우', () => {
    // Step 1: 회원가입
    const member = registerMember();
    if (!member) { sleep(1); return; }
    const headers = makeHeaders(member.deviceToken);

    // 호스트 방 생성 대기
    sleep(2);

    // Step 2: 방 목록에서 K6 테스트 방 검색
    const listRes = http.get(
      `${BASE_URL}/api/rooms?meetingType=DELIVERY`,
      { headers, tags: { name: 'flow_room_search' } },
    );
    check(listRes, { '방 목록 200': (r) => r.status === 200 });

    let targetId = null;
    try {
      const rooms = JSON.parse(listRes.body).data;
      if (Array.isArray(rooms) && rooms.length > 0) {
        // K6 테스트 방 우선 선택
        const k6Room = rooms.find((r) => r.title && r.title.startsWith('K6_'));
        targetId = k6Room ? k6Room.id : rooms[0].id;
      }
    } catch (_) { /* 무시 */ }

    if (!targetId) { sleep(1); return; }

    sleep(0.3);

    // Step 3: 방 참여
    const joinRes = http.post(
      `${BASE_URL}/api/rooms/${targetId}/join`,
      null,
      { headers, tags: { name: 'room_join' } },
    );
    const joined = check(joinRes, { '방 참여 204': (r) => r.status === 204 });
    apiErrorRate.add(!joined ? 1 : 0);
    if (!joined) { sleep(1); return; }

    sleep(0.5);

    // Step 4: 참여자 주문 추가
    const participantMenu = { menuName: '콜라 1.5L', quantity: 1, price: 2500 };
    const orderRes = http.post(
      `${BASE_URL}/api/rooms/${targetId}/orders`,
      JSON.stringify(participantMenu),
      { headers, tags: { name: 'order_add' } },
    );
    const ordered = check(orderRes, { '참여자 주문 201': (r) => r.status === 201 });
    if (ordered) totalOrdersAdded.add(1);
    apiErrorRate.add(!ordered ? 1 : 0);

    sleep(0.5);

    // Step 5: 내 주문 + 정산 병렬 조회 (http.batch)
    const [myOrderRes, settlementRes] = http.batch([
      {
        method: 'GET',
        url:    `${BASE_URL}/api/rooms/${targetId}/orders?mine=true`,
        params: { headers, tags: { name: 'my_order_list' } },
      },
      {
        method: 'GET',
        url:    `${BASE_URL}/api/rooms/${targetId}/orders/settlement`,
        params: { headers, tags: { name: 'participant_settlement' } },
      },
    ]);

    check(myOrderRes,     { '내 주문 조회 200':      (r) => r.status === 200 });
    check(settlementRes,  { '참여자 정산 조회 200':  (r) => r.status === 200 });

    // 호스트 close 대기
    sleep(3);

    // Step 6: 방 나가기
    const leaveRes = http.post(
      `${BASE_URL}/api/rooms/${targetId}/leave`,
      null,
      { headers, tags: { name: 'room_leave' } },
    );
    check(leaveRes, { '방 나가기 204': (r) => r.status === 204 });
  });

  sleep(1);
}
