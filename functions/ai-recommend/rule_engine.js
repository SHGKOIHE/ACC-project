'use strict';

const CATEGORIES = ['치킨', '피자', '중식', '일식', '한식', '양식', '분식', '버거'];

const CATEGORY_KEYWORDS = {
  '치킨': ['치킨', '닭', 'chicken', '윙', '순살'],
  '피자': ['피자', 'pizza'],
  '중식': ['짜장', '짬뽕', '중국', '탕수육', '마라'],
  '일식': ['초밥', '라멘', '돈카츠', '우동', '스시', '일식'],
  '한식': ['비빔밥', '국밥', '삼겹살', '된장', '불고기', '갈비', '한식'],
  '양식': ['파스타', '스테이크', '리조또', '샐러드', '양식'],
  '분식': ['떡볶이', '순대', '김밥', '튀김', '라볶이', '분식'],
  '버거': ['버거', '햄버거', 'burger', '샌드위치'],
};

const GROUP_FRIENDLY = new Set(['치킨', '피자', '중식', '한식']);

function detectCategories(text) {
  const normalized = String(text || '').toLowerCase();
  return Object.entries(CATEGORY_KEYWORDS)
    .filter(([, keywords]) => keywords.some(keyword => normalized.includes(keyword.toLowerCase())))
    .map(([category]) => category);
}

function detectCategory(text) {
  return detectCategories(text)[0] || null;
}

function buildCategoryCount(participants) {
  const counts = {};
  for (const participant of participants) {
    for (const item of (participant.orderItems || [])) {
      for (const category of detectCategories(item.name)) {
        counts[category] = (counts[category] || 0) + 1;
      }
    }
  }
  return counts;
}

function calculateScore(candidate, participants = [], filters = {}) {
  const { category } = candidate;
  const counts = buildCategoryCount(participants);
  const maxCount = Math.max(...Object.values(counts), 1);
  const messageCategories = new Set(detectCategories(filters.userMessage));

  // Observed order preference: 0-40
  const preferenceScore = Math.round(((counts[category] || 0) / maxCount) * 40);
  // Explicit category selection: 0-35
  const filterScore = filters.category === category ? 35 : 0;
  // Free-text food signal: 0-15
  const messageScore = messageCategories.has(category) ? 15 : 0;
  // Group-sharing suitability: 0-10
  const groupScore = participants.length >= 3 && GROUP_FRIENDLY.has(category) ? 10 : 0;

  return Math.min(preferenceScore + filterScore + messageScore + groupScore, 100);
}

function buildReason(category, participants, filters) {
  const signals = [];
  const counts = buildCategoryCount(participants);
  const messageCategories = new Set(detectCategories(filters.userMessage));

  if (filters.category === category) signals.push(`선택한 ${category} 카테고리`);
  if ((counts[category] || 0) > 0) signals.push('참여자 주문 선호');
  if (messageCategories.has(category)) signals.push('입력한 요청');
  if (participants.length >= 3 && GROUP_FRIENDLY.has(category)) signals.push('여럿이 나누기 좋은 특성');

  return signals.length > 0
    ? `${signals.join(', ')}을 반영한 추천입니다.`
    : '특정 선호가 없을 때 선택하기 좋은 음식 카테고리입니다.';
}

function recommend(participants = [], filters = {}) {
  const candidates = CATEGORIES.map(category => ({
    category,
    score: calculateScore({ category }, participants, filters),
  }));

  candidates.sort((a, b) => b.score - a.score);

  return candidates.slice(0, 3).map((item, index) => ({
    rank: index + 1,
    // Kept for API compatibility; the value is a food/menu category, not a real restaurant.
    restaurantName: item.category,
    score: item.score,
    reason: buildReason(item.category, participants, filters),
  }));
}

module.exports = { calculateScore, recommend, detectCategory };
