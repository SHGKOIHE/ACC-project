'use strict';
const { BedrockRuntimeClient, InvokeModelCommand } = require('@aws-sdk/client-bedrock-runtime');

const REGION = process.env.AWS_REGION || 'ap-northeast-2';
const MODEL_ID = process.env.BEDROCK_MODEL_ID || 'anthropic.claude-3-haiku-20240307-v1:0';

const client = new BedrockRuntimeClient({ region: REGION });

async function generateExplanation(recommendations, participants) {
  try {
    const summary = recommendations.map(r =>
      `${r.rank}위: ${r.restaurantName} (점수 ${r.score})`
    ).join(', ');
    const participantCount = participants.length;

    const prompt = `${participantCount}명이 참여하는 음식 공동 구매 모임에 대한 음식 추천 결과입니다. 다음 추천 결과를 한국어 2문장으로 설명해줘: ${summary}`;

    const payload = {
      anthropic_version: 'bedrock-2023-05-31',
      max_tokens: 200,
      messages: [{ role: 'user', content: prompt }],
    };

    let timerId;
    const timeout = new Promise((_, reject) => {
      timerId = setTimeout(() => reject(new Error('timeout')), 5000);
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
    return response.content[0].text.trim();
  } catch (e) {
    console.warn('Bedrock explanation failed:', e.message);
    return '';
  }
}

module.exports = { generateExplanation };
