'use strict';

const { buildPrompt, parseRecommendationResponse } = require('./bedrock_client');

test('Bedrock JSON 응답을 추천 결과로 정규화한다', () => {
  const result = parseRecommendationResponse(JSON.stringify({
    recommendations: [
      { rank: 10, restaurantName: '치킨집', score: 120, reason: '치킨 선호' },
      { rank: 5, name: '피자집', matchScore: 80.4, reason: '공유하기 좋음' },
      { restaurantName: '중식집', score: -1 },
    ],
    explanation: '추천 요약',
  }));

  expect(result.recommendations).toEqual([
    { rank: 1, restaurantName: '치킨집', score: 100, reason: '치킨 선호' },
    { rank: 2, restaurantName: '피자집', score: 80, reason: '공유하기 좋음' },
    { rank: 3, restaurantName: '중식집', score: 0, reason: '' },
  ]);
  expect(result.explanation).toBe('추천 요약');
});

test('Bedrock 코드블록 응답에서도 JSON만 추출한다', () => {
  const result = parseRecommendationResponse('```json\n{"recommendations":[{"restaurantName":"A","score":1},{"restaurantName":"B","score":2},{"restaurantName":"C","score":3}],"explanation":"ok"}\n```');

  expect(result.recommendations).toHaveLength(3);
  expect(result.explanation).toBe('ok');
});

test('추천이 3개 미만이면 실패시켜 폴백을 유도한다', () => {
  expect(() => parseRecommendationResponse('{"recommendations":[{"restaurantName":"A"}],"explanation":""}'))
    .toThrow('fewer than 3');
});


test('프롬프트는 국제캠퍼스 음식 추천 범위와 사실성 제약을 명시한다', () => {
  const prompt = buildPrompt([], {
    category: '치킨',
    userMessage: '여럿이 먹을 음식',
    maxDeliveryFee: 3000,
    latitude: 37.2424,
    longitude: 127.08133,
  });

  expect(prompt).toContain('경희대학교 국제캠퍼스');
  expect(prompt).toContain('음식 또는 메뉴 카테고리');
  expect(prompt).toContain('실제 식당, 가격, 배달비, 거리');
  expect(prompt).not.toContain('37.2424');
  expect(prompt).not.toContain('maxDeliveryFee');
});
