'use strict';

const { GoogleGenerativeAI } = require('@google/generative-ai');

async function generateExplanation(recommendations, participants) {
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) return '';

  try {
    const genAI = new GoogleGenerativeAI(apiKey);
    const model = genAI.getGenerativeModel({ model: 'gemini-1.5-flash' });

    const summary = recommendations.map(r =>
      `${r.rank}위: ${r.restaurantName} (점수 ${r.score})`
    ).join(', ');
    const participantCount = participants.length;

    const prompt =
      `${participantCount}명이 참여하는 음식 공동 구매 모임에 대한 음식 추천 결과입니다. ` +
      `다음 추천 결과를 한국어 2문장으로 설명해줘: ${summary}`;

    const timeout = new Promise((_, reject) =>
      setTimeout(() => reject(new Error('timeout')), 5000)
    );
    const result = await Promise.race([
      model.generateContent(prompt),
      timeout,
    ]);

    return result.response.text().trim();
  } catch (e) {
    console.warn('Gemini explanation failed:', e.message);
    return '';
  }
}

module.exports = { generateExplanation };
