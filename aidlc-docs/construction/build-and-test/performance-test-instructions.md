# Performance Test Instructions
# 배달비 절약을 위한 음식 공동 구매 앱

**작성일**: 2026-05-24

---

## NFR 성능 목표

| 항목 | 목표 |
|------|------|
| REST API 응답 | p95 < 500ms |
| AI 추천 응답 | p95 < 3초 |
| WebSocket 동시 연결 | 100명 |

---

## k6 설치

```bash
# macOS
brew install k6

# Ubuntu
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
  --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" \
  | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt update && sudo apt install k6
```

---

## 시나리오 1: REST API 부하 테스트

```javascript
// load-test-api.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 20 },
    { duration: '1m',  target: 50 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed:   ['rate<0.01'],
  },
};

export default function () {
  const res = http.get('http://localhost:8080/api/rooms', {
    headers: { 'X-Device-Token': 'perf-test-token' },
  });
  check(res, { 'status 200': (r) => r.status === 200 });
  sleep(1);
}
```

```bash
k6 run load-test-api.js
```

---

## 시나리오 2: WebSocket 동시 연결

```javascript
// load-test-ws.js
import ws from 'k6/ws';
import { check } from 'k6';

export const options = {
  vus: 100,
  duration: '30s',
};

export default function () {
  const url = 'ws://localhost:8080/ws';
  const res = ws.connect(url, { headers: { 'X-Device-Token': `token-${__VU}` } }, (socket) => {
    socket.on('open', () => {
      socket.send('CONNECT\naccept-version:1.2\nhost:localhost\n\n\x00');
    });
    socket.on('message', (data) => {
      check(data, { 'connected': (d) => d.includes('CONNECTED') });
    });
    socket.setTimeout(() => socket.close(), 20000);
  });
  check(res, { 'connected': (r) => r && r.status === 101 });
}
```

```bash
k6 run load-test-ws.js
```

---

## 시나리오 3: AI 추천 응답 시간

```javascript
// load-test-ai.js
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 10,
  duration: '30s',
  thresholds: {
    http_req_duration: ['p(95)<3000'],
  },
};

export default function () {
  const res = http.post(
    'http://localhost:8080/api/rooms/1/recommend',
    null,
    { headers: { 'X-Device-Token': 'perf-test-token' } }
  );
  check(res, { 'status 200': (r) => r.status === 200 });
}
```

```bash
k6 run load-test-ai.js
```
