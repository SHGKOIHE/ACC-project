'use strict';

const { recommend } = require('./rule_engine');
const { generateExplanation } = require('./bedrock_client');

const INTERNAL_KEY_HEADER = 'x-internal-key';

exports.handler = async (event) => {
  // 환경변수 미설정 시 서버 오류 반환 (undefined !== undefined 인증 우회 방지)
  if (!process.env.INTERNAL_SECRET_KEY) {
    return {
      statusCode: 500,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ error: 'Server misconfiguration' }),
    };
  }

  // X-Internal-Key 검증
  const headers = event.headers || {};
  const receivedKey = headers['x-internal-key'] || headers['X-Internal-Key'];
  if (!receivedKey || receivedKey !== process.env.INTERNAL_SECRET_KEY) {
    return {
      statusCode: 403,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ error: 'Forbidden' }),
    };
  }

  let body;
  try {
    body = typeof event.body === 'string' ? JSON.parse(event.body) : event.body;
  } catch {
    return {
      statusCode: 400,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ error: 'Invalid JSON body' }),
    };
  }

  const { participants = [], filters = {} } = body;

  // 규칙 엔진 점수 계산
  const recommendations = recommend(participants, filters);

  // Bedrock 자연어 설명 (실패해도 recommendations는 반환)
  const explanation = await generateExplanation(recommendations, participants);

  return {
    statusCode: 200,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ recommendations, explanation }),
  };
};
