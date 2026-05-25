'use strict';

const { handler } = require('./index');
const { calculateScore, detectCategory } = require('./rule_engine');

// Gemini 항상 빈 문자열 반환으로 mocking
jest.mock('./gemini_client', () => ({
  generateExplanation: jest.fn().mockResolvedValue(''),
}));

const VALID_KEY = 'test-secret';

beforeEach(() => {
  process.env.INTERNAL_SECRET_KEY = VALID_KEY;
});

afterEach(() => {
  process.env.INTERNAL_SECRET_KEY = VALID_KEY;
});

function makeEvent(body, key = VALID_KEY) {
  return {
    headers: { 'x-internal-key': key },
    body: JSON.stringify(body),
  };
}

// --- Handler tests ---

test('INTERNAL_SECRET_KEY 환경변수 미설정 시 500 반환', async () => {
  delete process.env.INTERNAL_SECRET_KEY;
  const event = makeEvent({}, VALID_KEY);
  const result = await handler(event);
  expect(result.statusCode).toBe(500);
});

test('X-Internal-Key 헤더 없으면 403 반환', async () => {
  const event = { headers: {}, body: JSON.stringify({}) };
  const result = await handler(event);
  expect(result.statusCode).toBe(403);
});

test('X-Internal-Key 불일치 시 403 반환', async () => {
  const event = makeEvent({}, 'wrong-key');
  const result = await handler(event);
  expect(result.statusCode).toBe(403);
});

test('정상 요청 시 recommendations 배열 포함 응답', async () => {
  const event = makeEvent({
    roomId: 1,
    participants: [
      { nickname: '짱구', orderItems: [{ name: '치킨', price: 15000 }] },
      { nickname: '철수', orderItems: [{ name: '피자', price: 20000 }] },
    ],
    filters: {},
  });

  const result = await handler(event);
  expect(result.statusCode).toBe(200);

  const parsed = JSON.parse(result.body);
  expect(Array.isArray(parsed.recommendations)).toBe(true);
  expect(parsed.recommendations.length).toBe(3);
  expect(parsed.recommendations[0]).toHaveProperty('rank', 1);
  expect(parsed.recommendations[0]).toHaveProperty('restaurantName');
  expect(parsed.recommendations[0]).toHaveProperty('score');
  expect(typeof parsed.explanation).toBe('string');
});

test('participants 없어도 200 응답', async () => {
  const event = makeEvent({ roomId: 1, filters: {} });
  const result = await handler(event);
  expect(result.statusCode).toBe(200);
});

// --- rule_engine unit tests ---

test('치킨 키워드 포함 아이템 → 치킨 카테고리 감지', () => {
  expect(detectCategory('후라이드치킨')).toBe('치킨');
  expect(detectCategory('치즈버거')).toBe('버거');
  expect(detectCategory('짜장면')).toBe('중식');
});

test('calculateScore: 카테고리 필터 일치 시 높은 점수', () => {
  const participants = [{ nickname: 'a', orderItems: [{ name: '치킨', price: 15000 }] }];
  const filters = { category: '치킨' };
  const score = calculateScore({ category: '치킨' }, participants, filters);
  expect(score).toBeGreaterThan(50);
});

test('calculateScore: 카테고리 필터 불일치 시 낮은 점수', () => {
  const participants = [{ nickname: 'a', orderItems: [] }];
  const filters = { category: '치킨' };
  const pizzaScore = calculateScore({ category: '피자' }, participants, filters);
  expect(pizzaScore).toBeLessThan(50);
});

test('calculateScore: 3인 이상 그룹 친화 카테고리 보너스', () => {
  const participants = [
    { nickname: 'a', orderItems: [] },
    { nickname: 'b', orderItems: [] },
    { nickname: 'c', orderItems: [] },
  ];
  const filters = {};
  const chickenScore = calculateScore({ category: '치킨' }, participants, filters);
  const ramenScore = calculateScore({ category: '일식' }, participants, filters);
  expect(chickenScore).toBeGreaterThan(ramenScore);
});
