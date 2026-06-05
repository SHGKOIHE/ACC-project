/**
 * ai_recommend.js — AI 추천 부하 테스트
 *
 * 대상: POST /api/recommend (AiStandaloneRecommendController)
 * 인증: X-Device-Token (setup에서 사전 등록)
 *
 * 요청 필드: latitude, longitude, category, maxDeliveryFee, userMessage
 *   - mood/budget/spicyLevel/mealType → userMessage 문자열로 변환
 *
 * 단계:
 *   0s  ~  1m : 10 VU  (워밍업)
 *   1m  ~  3m : 50 VU  (중간 부하)
 *   3m  ~  4m : 100 VU (최대 부하)
 *   4m  ~  6m : 10 VU  (복구 확인)
 *
 * 실행:
 *   k6 run k6/scenarios/ai_recommend.js
 *   k6 run k6/scenarios/ai_recommend.js --out json=ai_recommend_result.json
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { uuidv4 } from './auth.js';

const BASE_URL = __ENV.BASE_URL || 'https://40ocxlcwfl.execute-api.ap-northeast-2.amazonaws.com';

// ── 커스텀 메트릭 ────────────────────────────────────────────
const recommendSuccess  = new Counter('ai_recommend_success');
const recommendFail     = new Counter('ai_recommend_fail');
const recommendErrorRate = new Rate('ai_recommend_error_rate');
const recommendLatency  = new Trend('ai_recommend_latency', true);
const throttleCount     = new Counter('ai_recommend_throttle');

// ── 사전 등록: 100 VU 분량의 토큰 생성 ──────────────────────
export function setup() {
  const tokens = [];
  for (let i = 0; i < 100; i++) {
    const token = uuidv4();
    const res = http.post(
      `${BASE_URL}/api/auth/register`,
      JSON.stringify({ nickname: `rec${i}`, deviceToken: token }),
      { headers: { 'Content-Type': 'application/json' }, timeout: '10s' },
    );
    if (res.status === 200 || res.status === 201) {
      tokens.push(token);
    }
  }
  return { tokens };
}

// ── 다양한 요청 시나리오 ─────────────────────────────────────
// mood/budget/spicyLevel/mealType → userMessage로 통합
const SCENARIOS = [
  {
    category: '한식',
    maxDeliveryFee: 10000,
    userMessage: '든든하게 먹고 싶어요. 매운 정도는 중간, 배달로 시킬 예정이에요.',
    latitude: 37.5665,
    longitude: 126.9780,
  },
  {
    category: '치킨',
    maxDeliveryFee: 3000,
    userMessage: '친구들이랑 같이 먹을 거예요. 매콤한 거 좋아해요.',
    latitude: 37.4979,
    longitude: 127.0276,
  },
  {
    category: '피자',
    maxDeliveryFee: 5000,
    userMessage: '단체 주문이라 양 많이 시킬 예정이에요.',
    latitude: 37.5172,
    longitude: 127.0473,
  },
  {
    category: null,
    maxDeliveryFee: 2000,
    userMessage: '혼밥인데 가성비 좋은 메뉴 추천해주세요.',
    latitude: 37.5326,
    longitude: 127.0246,
  },
  {
    category: '중식',
    maxDeliveryFee: 4000,
    userMessage: '자극적이지 않은 걸로 부드럽게 먹고 싶어요.',
    latitude: 37.5563,
    longitude: 126.9723,
  },
];

// ── 시나리오 옵션 ────────────────────────────────────────────
export const options = {
  setupTimeout: '3m',
  stages: [
    { duration: '1m', target: 10  }, // 워밍업
    { duration: '2m', target: 50  }, // 중간 부하
    { duration: '1m', target: 100 }, // 최대 부하
    { duration: '2m', target: 10  }, // 복구 확인
  ],
  thresholds: {
    http_req_failed:        ['rate<0.05'],
    http_req_duration:      ['avg<3000', 'p(95)<5000'],
    ai_recommend_error_rate: ['rate<0.05'],
    ai_recommend_latency:   ['p(95)<5000'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

// ── 메인 시나리오 ─────────────────────────────────────────────
export default function (data) {
  const token   = data.tokens[__VU % data.tokens.length];
  const payload = SCENARIOS[__VU % SCENARIOS.length];

  group('AI 추천 요청', () => {
    const res = http.post(
      `${BASE_URL}/api/recommend`,
      JSON.stringify(payload),
      {
        headers: {
          'Content-Type':   'application/json',
          'X-Device-Token': token,
        },
        tags:    { name: 'ai_recommend' },
        timeout: '30s',
      },
    );

    if (res.status === 429) throttleCount.add(1);

    recommendLatency.add(res.timings.duration);

    const ok = check(res, {
      'status is 200':      (r) => r.status === 200,
      'response time < 3s': (r) => r.timings.duration < 3000,
      '추천 결과 존재':       (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.data !== undefined;
        } catch (_) { return false; }
      },
    });

    recommendErrorRate.add(!ok ? 1 : 0);

    if (ok) {
      recommendSuccess.add(1);
    } else {
      recommendFail.add(1);
    }
  });

  sleep(1);
}
