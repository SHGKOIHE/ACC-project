import http from 'k6/http';
import { check, sleep } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
  vus: 50,
  duration: '5m',
  thresholds: {
    http_req_duration: ['avg<500'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = 'https://40ocxlcwfl.execute-api.ap-northeast-2.amazonaws.com';

export function setup() {
  const tokens = [];
  for (let i = 0; i < 50; i++) {
    const token = uuidv4();
    const res = http.post(
      `${BASE_URL}/api/auth/register`,
      JSON.stringify({ nickname: `k6user${i}`, deviceToken: token }),
      { headers: { 'Content-Type': 'application/json' } }
    );
    if (res.status === 200 || res.status === 201) {
      tokens.push(token);
    }
  }
  return { tokens };
}

export default function (data) {
  const token = data.tokens[__VU % data.tokens.length];

  const res = http.get(`${BASE_URL}/api/rooms`, {
    headers: { 'X-Device-Token': token },
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);
}
