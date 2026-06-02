/**
 * ai_surge.js — AI 추론 급증 시나리오
 *
 * 시나리오: ChatGPT처럼 AI 기능에 트래픽이 몰리는 상황
 *   Lambda + Bedrock (Claude Haiku) AI 추천 엔드포인트 집중 부하
 *
 * 측정 포인트:
 *   - Lambda 콜드 스타트 여부 (첫 요청 vs 후속 요청 응답시간)
 *   - Bedrock 응답시간 (AI 추론 시간 포함)
 *   - 동시 Lambda 실행 한도 도달 시 동작
 *   - 오류율 (Lambda throttling, Bedrock 제한 등)
 *
 * 엔드포인트:
 *   POST /api/recommend
 *   body: { latitude, longitude, category, maxDeliveryFee, userMessage }
 *
 * 실행:
 *   k6 run -e BASE_URL=http://localhost:8080 k6/scenarios/ai_surge.js
 *   k6 run -e BASE_URL=https://your-lambda-url k6/scenarios/ai_surge.js --out json=ai_surge_result.json
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { generateNickname, uuidv4 } from './auth.js';

const BASE_URL = __ENV.BASE_URL || 'https://40ocxlcwfl.execute-api.ap-northeast-2.amazonaws.com';

// ── 커스텀 메트릭 ────────────────────────────────────────────
const aiRequestCount     = new Counter('ai_total_requests');
const aiSuccessCount     = new Counter('ai_success_count');
const aiErrorRate        = new Rate('ai_error_rate');
const aiResponseTime     = new Trend('ai_response_time', true);    // Bedrock 포함 전체 응답
const coldStartLatency   = new Trend('ai_cold_start_latency', true); // 첫 요청 (VU당 1회)
const warmLatency        = new Trend('ai_warm_latency', true);     // 후속 요청 (캐시된 컨테이너)
const throttleCount      = new Counter('ai_throttle_count');       // Lambda throttling 횟수

// ── 시나리오 옵션 ────────────────────────────────────────────
// AI 서비스 특성상 응답시간이 길어 임계값을 완화
export const options = {
  scenarios: {
    // ── Phase 1: 점진적 증가 (새벽 배치 후 아침 트래픽 패턴)
    ai_rampup: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s',  target: 5   }, // 초기 (콜드 스타트 구간)
        { duration: '60s',  target: 20  }, // 점진적 증가
        { duration: '60s',  target: 50  }, // 최대 부하 (AI 급증 시뮬레이션)
        { duration: '30s',  target: 20  }, // 감소
        { duration: '30s',  target: 0   }, // 쿨다운
      ],
      exec: 'aiSurgeScenario',
      tags: { scenario: 'ai_surge' },
    },
  },

  thresholds: {
    // AI 추론 포함이므로 응답시간 기준 완화 (p95 5초)
    http_req_duration:     ['p(95)<5000'],
    http_req_failed:       ['rate<0.05'],
    ai_error_rate:         ['rate<0.05'],
    // 정상(웜) 응답 p95 3초 (Bedrock 추론시간 포함)
    ai_warm_latency:       ['p(95)<3000'],
    // 콜드 스타트는 별도 측정 (허용치 10초)
    ai_cold_start_latency: ['p(95)<10000'],
  },
};

// ── 테스트 입력 데이터 ────────────────────────────────────────
// 다양한 사용자 요청 패턴 (Bedrock 프롬프트 다양성 테스트)
const AI_REQUESTS = [
  {
    latitude:       37.5665,
    longitude:      126.9780,
    category:       '치킨',
    maxDeliveryFee: 3000,
    userMessage:    '친구들이랑 시켜먹을 건데 매운 거 좋아해요',
  },
  {
    latitude:       37.4979,
    longitude:      127.0276,
    category:       null,
    maxDeliveryFee: 5000,
    userMessage:    '혼자 먹을 거라 양 많고 배달비 저렴한 곳으로',
  },
  {
    latitude:       37.5172,
    longitude:      127.0473,
    category:       '한식',
    maxDeliveryFee: null,
    userMessage:    null,
  },
  {
    latitude:       37.5563,
    longitude:      126.9723,
    category:       '피자',
    maxDeliveryFee: 2000,
    userMessage:    '단체 주문이라 양 많이 시킬 예정이에요',
  },
  {
    latitude:       37.5326,
    longitude:      127.0246,
    category:       '버거',
    maxDeliveryFee: 4000,
    userMessage:    '빠른 배달이 중요해요',
  },
];

// ── 유틸 ─────────────────────────────────────────────────────
function register() {
  const nickname    = generateNickname();
  const deviceToken = uuidv4();
  const res = http.post(
    `${BASE_URL}/api/auth/register`,
    JSON.stringify({ nickname, deviceToken }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'register' }, timeout: '5s' },
  );
  if (res.status !== 201) return null;
  return { nickname, deviceToken };
}

// ── 메인 시나리오 ─────────────────────────────────────────────
export function aiSurgeScenario() {
  // Step 1: 회원가입
  const member = register();
  if (!member) { sleep(1); return; }

  const headers = {
    'Content-Type':   'application/json',
    'X-Device-Token':  member.deviceToken,
  };

  sleep(0.5);

  // Step 2: AI 추천 요청
  // __ITER: VU당 반복 횟수 (0부터 시작)
  // 첫 번째 반복 = 콜드 스타트 가능성 높음
  const isFirstRequest = __ITER === 0;
  const payload = AI_REQUESTS[Math.floor(Math.random() * AI_REQUESTS.length)];

  group('AI 추천 요청', () => {
    aiRequestCount.add(1);

    const reqStart = Date.now();
    const res = http.post(
      `${BASE_URL}/api/recommend`,
      JSON.stringify(payload),
      {
        headers,
        tags:    { name: 'ai_recommend', cold_start: isFirstRequest ? 'true' : 'false' },
        timeout: '30s', // Bedrock 추론 시간 고려한 긴 타임아웃
      },
    );
    const duration = Date.now() - reqStart;

    // Lambda throttling 감지 (429 Too Many Requests)
    if (res.status === 429) {
      throttleCount.add(1);
    }

    const ok = check(res, {
      'AI 추천 200':               (r) => r.status === 200,
      '추천 결과 존재':             (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.data && Array.isArray(body.data.recommendations);
        } catch (_) { return false; }
      },
      '추천 결과 1개 이상':         (r) => {
        try {
          return JSON.parse(r.body).data.recommendations.length > 0;
        } catch (_) { return false; }
      },
      'restaurantName 필드 존재':   (r) => {
        try {
          const recs = JSON.parse(r.body).data.recommendations;
          return recs.length > 0 && typeof recs[0].restaurantName === 'string';
        } catch (_) { return false; }
      },
    });

    aiErrorRate.add(!ok ? 1 : 0);
    aiResponseTime.add(duration);

    if (ok) {
      aiSuccessCount.add(1);
      // 콜드 스타트 vs 웜 응답 분리 측정
      if (isFirstRequest) {
        coldStartLatency.add(duration);
      } else {
        warmLatency.add(duration);
      }
    }

    // 응답시간 로깅 (디버깅용)
    // console.log(`AI 응답 ${duration}ms | cold_start=${isFirstRequest} | status=${res.status}`);
  });

  // 실제 사용자 패턴: AI 결과 확인 후 잠시 대기
  sleep(2);
}

// 단독 실행 시 default export 필요
export default aiSurgeScenario;
