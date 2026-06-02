/**
 * room_list.js — 방 목록 조회 부하 테스트 (읽기 집중)
 *
 * 테스트 대상:
 *   - POST /api/auth/register         : 토큰 발급 (셋업)
 *   - GET  /api/rooms                 : 방 목록 (카테고리/위치/타입 필터 조합)
 *   - GET  /api/rooms/{id}            : 방 상세 조회
 *
 * 전략:
 *   - 읽기 위주 고부하 (VU 50명, 지속 60초)
 *   - http.batch()로 여러 필터 조합 병렬 요청
 *   - 상세 조회는 목록 결과에서 무작위 방 선택
 *
 * 실행 예시:
 *   k6 run scenarios/room_list.js
 *   k6 run -e BASE_URL=http://192.168.0.10:8080 scenarios/room_list.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { generateNickname, uuidv4 } from './auth.js';

// ── 환경변수 ──────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'https://40ocxlcwfl.execute-api.ap-northeast-2.amazonaws.com';

// ── 커스텀 메트릭 ─────────────────────────────────────────
const listLatency   = new Trend('room_list_latency', true);
const detailLatency = new Trend('room_detail_latency', true);
const listEmptyRate = new Rate('room_list_empty');

// ── 시나리오 옵션 ─────────────────────────────────────────
export const options = {
  scenarios: {
    room_list_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '15s', target: 10 }, // 워밍업
        { duration: '30s', target: 50 }, // 부하 증가
        { duration: '60s', target: 50 }, // 최대 부하 유지
        { duration: '15s', target: 0  }, // 쿨다운
      ],
    },
  },
  thresholds: {
    http_req_duration:  ['p(95)<500'],
    http_req_failed:    ['rate<0.01'],
    room_list_latency:  ['p(95)<400'],
    room_detail_latency:['p(95)<400'],
  },
};

// ── 필터 조합 데이터 ──────────────────────────────────────
// 서울 주요 좌표 기반 반경 검색 조합
const SEARCH_PARAMS = [
  // 필터 없음 (전체 목록)
  '',
  // 배달 타입만
  'meetingType=DELIVERY',
  // 같이먹기 타입만
  'meetingType=TOGETHER',
  // 카테고리 필터
  'category=한식',
  'category=중식',
  'category=일식',
  // 위치 기반 (서울 시청 근처)
  'lat=37.5665&lng=126.9780&radius=3.0',
  // 위치 + 타입 복합
  'lat=37.5665&lng=126.9780&radius=5.0&meetingType=DELIVERY',
  // 위치 + 카테고리
  'lat=37.5665&lng=126.9780&radius=2.0&category=치킨',
];

// ── 셋업: 공통 deviceToken 발급 ───────────────────────────
// 각 VU가 자체 토큰을 발급하여 독립적으로 동작
export function setup() {
  // setup 단계에서는 단일 토큰만 확인 (실제 VU별 토큰은 default에서 발급)
  return {};
}

// ── 메인 시나리오 함수 ────────────────────────────────────
export default function () {
  // ── Step 1: VU별 회원가입 (토큰 발급) ────────────────
  const nickname    = generateNickname();
  const deviceToken = uuidv4();
  const regRes = http.post(
    `${BASE_URL}/api/auth/register`,
    JSON.stringify({ nickname, deviceToken }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'setup_register' },
    },
  );

  if (regRes.status !== 201) {
    sleep(1);
    return;
  }

  // deviceToken은 클라이언트가 생성한 UUID 사용 (응답에 포함되지 않음)
  const authHeaders = {
    'Content-Type':   'application/json',
    'X-Device-Token':  deviceToken,
  };

  sleep(0.2);

  // ── Step 2: 여러 필터 조합을 병렬로 방 목록 조회 ─────
  // 한 번에 최대 3개 필터를 병렬 요청 (http.batch 활용)
  const batchSize  = 3;
  const startIdx   = Math.floor(Math.random() * (SEARCH_PARAMS.length - batchSize + 1));
  const batchParams = SEARCH_PARAMS.slice(startIdx, startIdx + batchSize);

  const batchRequests = batchParams.map((params) => ({
    method: 'GET',
    url:    `${BASE_URL}/api/rooms${params ? '?' + params : ''}`,
    params: {
      headers: authHeaders,
      tags:    { name: 'room_list' },
    },
  }));

  const batchStart = Date.now();
  const batchResponses = http.batch(batchRequests);
  listLatency.add(Date.now() - batchStart);

  // 배치 응답 검증 및 방 ID 수집
  let roomIds = [];
  for (const res of batchResponses) {
    check(res, {
      '방 목록 조회 200 응답': (r) => r.status === 200,
      '응답 data 배열 존재':   (r) => {
        try {
          return Array.isArray(JSON.parse(r.body).data);
        } catch (_) {
          return false;
        }
      },
    });

    try {
      const rooms = JSON.parse(res.body).data;
      if (Array.isArray(rooms)) {
        listEmptyRate.add(rooms.length === 0 ? 1 : 0);
        roomIds = roomIds.concat(rooms.map((r) => r.id));
      }
    } catch (_) {
      // 파싱 실패 무시
    }
  }

  sleep(0.3);

  // ── Step 3: 수집된 방 ID 중 랜덤하게 상세 조회 ───────
  if (roomIds.length === 0) {
    sleep(1);
    return;
  }

  // 중복 제거 후 최대 2개 무작위 선택
  const uniqueIds    = [...new Set(roomIds)];
  const pickCount    = Math.min(2, uniqueIds.length);
  const shuffled     = uniqueIds.sort(() => Math.random() - 0.5).slice(0, pickCount);

  const detailRequests = shuffled.map((id) => ({
    method: 'GET',
    url:    `${BASE_URL}/api/rooms/${id}`,
    params: {
      headers: authHeaders,
      tags:    { name: 'room_detail' },
    },
  }));

  const detailStart = Date.now();
  const detailResponses = http.batch(detailRequests);
  detailLatency.add(Date.now() - detailStart);

  for (const res of detailResponses) {
    check(res, {
      '방 상세 조회 200 응답':      (r) => r.status === 200,
      '방 상세에 id 필드 존재':     (r) => {
        try {
          return typeof JSON.parse(r.body).data.id === 'string';
        } catch (_) {
          return false;
        }
      },
    });
  }

  sleep(1);
}
