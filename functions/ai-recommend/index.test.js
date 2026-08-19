'use strict';

const { handler } = require('./index');
const { calculateScore, recommend, detectCategory } = require('./rule_engine');

const { generateRecommendations } = require('./bedrock_client');
const { fetchCandidatesByCategory } = require('./kakao_client');

jest.mock('./bedrock_client', () => ({
  generateRecommendations: jest.fn(),
}));

jest.mock('./kakao_client', () => ({
  fetchCandidatesByCategory: jest.fn(),
}));

const VALID_KEY = 'test-secret';

beforeEach(() => {
  process.env.INTERNAL_SECRET_KEY = VALID_KEY;
  fetchCandidatesByCategory.mockResolvedValue({});
  generateRecommendations.mockResolvedValue({
    recommendations: [
      { rank: 1, restaurantName: '치킨', score: 92, reason: '치킨 선호가 높습니다.' },
      { rank: 2, restaurantName: '피자', score: 81, reason: '피자 주문도 있어 함께 먹기 좋습니다.' },
      { rank: 3, restaurantName: '중식', score: 70, reason: '단체 주문에 적합합니다.' },
    ],
    explanation: 'AI가 주문 선호를 기준으로 추천했습니다.',
  });
});

afterEach(() => {
  jest.clearAllMocks();
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
  expect(parsed.recommendations[0]).toHaveProperty('restaurantName', '치킨');
  expect(parsed.recommendations[0]).toHaveProperty('score', 92);
  expect(parsed.explanation).toBe('AI가 주문 선호를 기준으로 추천했습니다.');
  expect(generateRecommendations).toHaveBeenCalledWith(
    expect.any(Array),
    {},
    {}
  );
});

test('카카오맵 후보를 조회해 Bedrock 호출에 그대로 전달한다', async () => {
  const candidates = { 치킨: [{ name: '경희치킨', address: '용인시', distanceMeters: 300, placeUrl: 'https://x' }] };
  fetchCandidatesByCategory.mockResolvedValueOnce(candidates);

  const event = makeEvent({
    roomId: 1,
    participants: [{ nickname: '짱구', orderItems: [{ name: '치킨', price: 15000 }] }],
    filters: {},
  });

  await handler(event);

  expect(fetchCandidatesByCategory).toHaveBeenCalledWith(expect.any(Array));
  expect(generateRecommendations).toHaveBeenCalledWith(
    expect.any(Array),
    {},
    candidates
  );
});

test('Bedrock 실패 시 카카오맵 후보가 규칙 엔진 폴백에도 전달된다', async () => {
  const candidates = { 치킨: [{ name: '경희치킨', address: '용인시', distanceMeters: 300, placeUrl: 'https://x' }] };
  fetchCandidatesByCategory.mockResolvedValueOnce(candidates);
  generateRecommendations.mockRejectedValueOnce(new Error('bedrock down'));

  const event = makeEvent({
    roomId: 1,
    participants: [{ nickname: '짱구', orderItems: [{ name: '치킨', price: 15000 }] }],
    filters: { category: '치킨' },
  });

  const result = await handler(event);
  const parsed = JSON.parse(result.body);

  expect(parsed.fallback).toBe(true);
  expect(parsed.recommendations[0]).toMatchObject({ restaurantName: '경희치킨' });
});

test('Bedrock 추천 실패 시 규칙 엔진 결과로 폴백', async () => {
  generateRecommendations.mockRejectedValueOnce(new Error('bedrock down'));
  const event = makeEvent({
    roomId: 1,
    participants: [{ nickname: '짱구', orderItems: [{ name: '치킨', price: 15000 }] }],
    filters: { category: '치킨' },
  });

  const result = await handler(event);
  expect(result.statusCode).toBe(200);

  const parsed = JSON.parse(result.body);
  expect(parsed.fallback).toBe(true);
  expect(parsed.recommendations).toHaveLength(3);
  expect(parsed.recommendations[0]).toMatchObject({ restaurantName: '치킨' });
  expect(parsed.recommendations[0].reason).not.toBe('');
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
  expect(detectCategory('크림파스타')).toBe('양식');
  expect(detectCategory('CHICKEN 샐러드')).toBe('치킨');
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


test('자유 입력의 음식 키워드가 폴백 추천 순위에 반영된다', () => {
  const recommendations = recommend([], { userMessage: '오늘은 마라탕이나 짬뽕처럼 매운 중식이 먹고 싶어요' });

  expect(recommendations[0]).toMatchObject({ restaurantName: '중식', score: 15 });
  expect(recommendations[0].reason).toContain('입력한 요청');
});

test('폴백 결과는 실제 식당이나 검증되지 않은 배달 정보를 주장하지 않는다', () => {
  const recommendations = recommend([], {});
  const serialized = JSON.stringify(recommendations);

  expect(serialized).not.toContain('맛집');
  expect(serialized).not.toContain('배달이 빠');
  expect(serialized).not.toContain('배달비');
  expect(recommendations.every(item => !item.restaurantName.endsWith('집'))).toBe(true);
});
