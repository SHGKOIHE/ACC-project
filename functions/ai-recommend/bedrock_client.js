'use strict';
const { BedrockRuntimeClient, InvokeModelCommand } = require('@aws-sdk/client-bedrock-runtime');

const REGION = process.env.AWS_REGION || 'ap-northeast-2';
const MODEL_ID = process.env.BEDROCK_MODEL_ID || 'anthropic.claude-3-haiku-20240307-v1:0';

const client = new BedrockRuntimeClient({ region: REGION });

function buildPrompt(participants, filters) {
  return `너는 음식 공동구매 앱의 추천 엔진이다. 참여자 주문 선호와 필터를 보고 음식점 카테고리/식당명을 추천해라.

입력 데이터:
${JSON.stringify({ participants, filters }, null, 2)}

규칙:
- 반드시 한국어 JSON 객체만 출력한다. 마크다운, 설명 문장, 코드블록은 금지한다.
- recommendations는 정확히 3개를 반환한다.
- rank는 1부터 3까지 중복 없이 오름차순이다.
- restaurantName은 실제 상호를 모르면 "치킨집", "피자집"처럼 카테고리 기반 이름으로 작성한다.
- score는 0~100 정수이며 입력 선호와 필터에 얼마나 맞는지 나타낸다.
- reason은 추천 이유를 한국어 한 문장으로 작성한다.
- explanation은 전체 추천 결과를 한국어 1~2문장으로 요약한다.

출력 스키마:
{"recommendations":[{"rank":1,"restaurantName":"","score":0,"reason":""}],"explanation":""}`;
}

function extractJson(text) {
  const trimmed = text.trim();
  if (trimmed.startsWith('{') && trimmed.endsWith('}')) return trimmed;

  const fenced = trimmed.match(/```(?:json)?\s*([\s\S]*?)\s*```/i);
  if (fenced) return fenced[1].trim();

  const start = trimmed.indexOf('{');
  const end = trimmed.lastIndexOf('}');
  if (start >= 0 && end > start) return trimmed.slice(start, end + 1);

  throw new Error('No JSON object found in Bedrock response');
}

function clampScore(score) {
  const numeric = Number(score);
  if (!Number.isFinite(numeric)) return 0;
  return Math.max(0, Math.min(100, Math.round(numeric)));
}

function normalizeRecommendation(item, index) {
  return {
    rank: index + 1,
    restaurantName: String(item.restaurantName || item.name || `${index + 1}위 추천`).slice(0, 50),
    score: clampScore(item.score ?? item.matchScore),
    reason: String(item.reason || '').slice(0, 160),
  };
}

function parseRecommendationResponse(text) {
  const parsed = JSON.parse(extractJson(text));
  if (!Array.isArray(parsed.recommendations) || parsed.recommendations.length === 0) {
    throw new Error('Bedrock response missing recommendations');
  }

  const recommendations = parsed.recommendations
    .slice(0, 3)
    .map(normalizeRecommendation);

  if (recommendations.length < 3) {
    throw new Error('Bedrock response returned fewer than 3 recommendations');
  }

  return {
    recommendations,
    explanation: String(parsed.explanation || '').slice(0, 300),
  };
}

async function generateRecommendations(participants, filters) {
  const prompt = buildPrompt(participants, filters);
  const payload = {
    anthropic_version: 'bedrock-2023-05-31',
    max_tokens: 700,
    temperature: 0.3,
    messages: [{ role: 'user', content: prompt }],
  };

  let timerId;
  const timeout = new Promise((_, reject) => {
    timerId = setTimeout(() => reject(new Error('timeout')), 8000);
  });

  const result = await Promise.race([
    client.send(new InvokeModelCommand({
      modelId: MODEL_ID,
      contentType: 'application/json',
      accept: 'application/json',
      body: JSON.stringify(payload),
    })),
    timeout,
  ]).finally(() => clearTimeout(timerId));

  const response = JSON.parse(Buffer.from(result.body).toString());
  const text = response.content?.[0]?.text;
  if (!text) throw new Error('Bedrock response missing content text');
  return parseRecommendationResponse(text);
}

module.exports = { generateRecommendations, parseRecommendationResponse };
