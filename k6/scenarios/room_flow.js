/**
 * room_flow.js — 방 생성 → 참여 → 주문 → 정산 전체 플로우
 *
 * 시나리오 구조:
 *   - 가상 유저 2명이 같은 방에서 상호작용
 *   - VU 짝수 번호(0, 2, 4...) → 호스트 역할
 *   - VU 홀수 번호(1, 3, 5...) → 참여자 역할
 *   - __VU 를 기반으로 역할 분기
 *
 * 동기화 전략:
 *   - 호스트가 방 생성 후 roomId를 SharedArray에 저장
 *   - 참여자는 방 목록에서 호스트가 만든 방을 검색하여 참여
 *   - sleep()로 호스트/참여자 타이밍 조율
 *
 * 테스트 대상:
 *   - POST /api/auth/register
 *   - POST /api/rooms                      (호스트)
 *   - GET  /api/rooms                      (참여자: 방 검색)
 *   - POST /api/rooms/{id}/join            (참여자)
 *   - POST /api/rooms/{roomId}/orders      (호스트 + 참여자)
 *   - GET  /api/rooms/{roomId}/orders      (양쪽)
 *   - GET  /api/rooms/{roomId}/orders/settlement (양쪽)
 *   - POST /api/rooms/{id}/close           (호스트)
 *
 * 실행 예시:
 *   k6 run scenarios/room_flow.js
 *   k6 run -e BASE_URL=http://192.168.0.10:8080 scenarios/room_flow.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { generateNickname, uuidv4 } from './auth.js';

// ── 환경변수 ──────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'https://40ocxlcwfl.execute-api.ap-northeast-2.amazonaws.com';

// ── 커스텀 메트릭 ─────────────────────────────────────────
const roomCreated     = new Counter('room_flow_created');
const roomJoined      = new Counter('room_flow_joined');
const orderAdded      = new Counter('room_flow_order_added');
const settlementOk    = new Counter('room_flow_settlement_ok');
const flowLatency     = new Trend('room_flow_e2e_latency', true);

// ── 시나리오 옵션 ─────────────────────────────────────────
// 호스트/참여자 각 5 VU × 2역할 = 실질 10 VU 동시 진행
export const options = {
  scenarios: {
    room_flow: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 10 }, // 워밍업 (호스트 5 + 참여자 5)
        { duration: '40s', target: 10 }, // 부하 유지
        { duration: '10s', target: 0  }, // 쿨다운
      ],
    },
  },
  thresholds: {
    http_req_duration:      ['p(95)<500'],
    http_req_failed:        ['rate<0.01'],
    room_flow_e2e_latency:  ['p(95)<3000'], // 전체 플로우 3초 이내
  },
};

// ── 유틸: 회원가입 → deviceToken 반환 ───────────────────
function register() {
  const nickname    = generateNickname();
  const deviceToken = uuidv4();
  const res = http.post(
    `${BASE_URL}/api/auth/register`,
    JSON.stringify({ nickname, deviceToken }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'flow_register' } },
  );
  check(res, { '회원가입 201': (r) => r.status === 201 });
  if (res.status !== 201) return null;
  return {
    nickname,
    deviceToken,
  };
}

// ── 유틸: 인증 헤더 반환 ─────────────────────────────────
function authHeaders(deviceToken) {
  return {
    'Content-Type':   'application/json',
    'X-Device-Token':  deviceToken,
  };
}

// ── 호스트 플로우 ─────────────────────────────────────────
function hostFlow() {
  const flowStart = Date.now();

  // Step 1: 회원가입
  const member = register();
  if (!member) return;
  const headers = authHeaders(member.deviceToken);

  sleep(0.5);

  // Step 2: 방 생성
  // 테스트용 고정 식당 정보 + 타임스탬프로 방 식별
  const roomTitle = `K6테스트_${member.nickname}_${Date.now()}`;
  const createRes = http.post(
    `${BASE_URL}/api/rooms`,
    JSON.stringify({
      title:              roomTitle,
      meetingType:        'DELIVERY',
      restaurantName:     'K6테스트식당',
      restaurantAddress:  '서울시 강남구 테헤란로 1',
      restaurantCategory: '치킨',
      latitude:           37.5665,
      longitude:          126.9780,
      deliveryFee:        3000,
      maxParticipants:    4,
      accountNumber:      '123-456-789',
      accountHolder:      member.nickname,
      bankName:           '카카오뱅크',
    }),
    { headers, tags: { name: 'room_create' } },
  );

  const created = check(createRes, {
    '방 생성 201': (r) => r.status === 201,
    '방 ID 존재':  (r) => {
      try { return typeof JSON.parse(r.body).data.id === 'string'; }
      catch (_) { return false; }
    },
  });

  if (!created) return;
  roomCreated.add(1);

  const roomId = JSON.parse(createRes.body).data.id;

  // Step 3: 참여자가 입장할 시간 대기 (3초)
  sleep(3);

  // Step 4: 호스트 주문 추가
  const menus = [
    { menuName: '후라이드치킨', quantity: 1, price: 18000 },
    { menuName: '양념치킨',    quantity: 1, price: 19000 },
  ];

  const orderRequests = menus.map((menu) => ({
    method: 'POST',
    url:    `${BASE_URL}/api/rooms/${roomId}/orders`,
    body:   JSON.stringify(menu),
    params: { headers, tags: { name: 'order_add' } },
  }));

  // 호스트 메뉴 병렬 주문 추가
  const orderResponses = http.batch(orderRequests);
  for (const res of orderResponses) {
    if (check(res, { '주문 추가 201': (r) => r.status === 201 })) {
      orderAdded.add(1);
    }
  }

  sleep(2); // 참여자 주문 대기

  // Step 5: 전체 주문 목록 조회
  const listRes = http.get(
    `${BASE_URL}/api/rooms/${roomId}/orders`,
    { headers, tags: { name: 'order_list' } },
  );
  check(listRes, {
    '주문 목록 조회 200': (r) => r.status === 200,
    '주문 배열 존재':     (r) => {
      try { return Array.isArray(JSON.parse(r.body).data); }
      catch (_) { return false; }
    },
  });

  sleep(0.5);

  // Step 6: 정산 조회
  const settlementRes = http.get(
    `${BASE_URL}/api/rooms/${roomId}/orders/settlement`,
    { headers, tags: { name: 'settlement_get' } },
  );
  const settled = check(settlementRes, {
    '정산 조회 200':          (r) => r.status === 200,
    '정산 총액 > 0':          (r) => {
      try { return JSON.parse(r.body).data.totalMenuAmount > 0; }
      catch (_) { return false; }
    },
  });
  if (settled) settlementOk.add(1);

  sleep(0.5);

  // Step 7: 방 주문 확정 (호스트만 가능) — close 엔드포인트
  const closeRes = http.post(
    `${BASE_URL}/api/rooms/${roomId}/close`,
    null,
    { headers, tags: { name: 'room_close' } },
  );
  check(closeRes, {
    '방 주문 확정 204': (r) => r.status === 204,
  });

  flowLatency.add(Date.now() - flowStart);
  sleep(1);
}

// ── 참여자 플로우 ─────────────────────────────────────────
function participantFlow() {
  // Step 1: 회원가입
  const member = register();
  if (!member) return;
  const headers = authHeaders(member.deviceToken);

  // 호스트가 방을 만들 때까지 대기
  sleep(2);

  // Step 2: 방 목록에서 K6 테스트 방 검색
  const listRes = http.get(
    `${BASE_URL}/api/rooms?meetingType=DELIVERY`,
    { headers, tags: { name: 'flow_room_list' } },
  );

  check(listRes, { '방 목록 200': (r) => r.status === 200 });

  let targetRoomId = null;
  try {
    const rooms = JSON.parse(listRes.body).data;
    if (Array.isArray(rooms) && rooms.length > 0) {
      // K6 테스트 방 우선 선택, 없으면 첫 번째 방
      const k6Room = rooms.find((r) => r.title && r.title.startsWith('K6테스트_'));
      targetRoomId = k6Room ? k6Room.id : rooms[0].id;
    }
  } catch (_) {
    // 파싱 실패 시 종료
    sleep(1);
    return;
  }

  if (!targetRoomId) {
    sleep(1);
    return;
  }

  sleep(0.5);

  // Step 3: 방 참여
  const joinRes = http.post(
    `${BASE_URL}/api/rooms/${targetRoomId}/join`,
    null,
    { headers, tags: { name: 'room_join' } },
  );
  const joined = check(joinRes, {
    '방 참여 204': (r) => r.status === 204,
  });
  if (joined) roomJoined.add(1);

  sleep(1);

  // Step 4: 참여자 주문 추가
  const participantMenus = [
    { menuName: '콜라', quantity: 2, price: 2000 },
  ];

  const orderRequests = participantMenus.map((menu) => ({
    method: 'POST',
    url:    `${BASE_URL}/api/rooms/${targetRoomId}/orders`,
    body:   JSON.stringify(menu),
    params: { headers, tags: { name: 'order_add' } },
  }));

  const orderResponses = http.batch(orderRequests);
  for (const res of orderResponses) {
    if (check(res, { '참여자 주문 추가 201': (r) => r.status === 201 })) {
      orderAdded.add(1);
    }
  }

  sleep(0.5);

  // Step 5: 내 주문만 조회 (mine=true)
  const myOrderRes = http.get(
    `${BASE_URL}/api/rooms/${targetRoomId}/orders?mine=true`,
    { headers, tags: { name: 'my_order_list' } },
  );
  check(myOrderRes, {
    '내 주문 조회 200': (r) => r.status === 200,
  });

  sleep(0.5);

  // Step 6: 정산 조회
  const settlementRes = http.get(
    `${BASE_URL}/api/rooms/${targetRoomId}/orders/settlement`,
    { headers, tags: { name: 'participant_settlement' } },
  );
  check(settlementRes, {
    '참여자 정산 조회 200': (r) => r.status === 200,
  });

  sleep(2); // 호스트 close 대기

  // Step 7: 방 나가기
  const leaveRes = http.post(
    `${BASE_URL}/api/rooms/${targetRoomId}/leave`,
    null,
    { headers, tags: { name: 'room_leave' } },
  );
  check(leaveRes, {
    '방 나가기 204': (r) => r.status === 204,
  });

  sleep(1);
}

// ── 메인: VU 번호로 역할 분기 ─────────────────────────────
export default function () {
  // __VU는 1부터 시작하는 가상 유저 번호
  // 홀수 VU → 호스트, 짝수 VU → 참여자
  if (__VU % 2 === 1) {
    hostFlow();
  } else {
    participantFlow();
  }
}
