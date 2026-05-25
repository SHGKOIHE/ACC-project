'use strict';

const CATEGORIES = ['치킨', '피자', '중식', '일식', '한식', '분식', '버거'];

const CATEGORY_KEYWORDS = {
  '치킨': ['치킨', '닭', 'chicken', '윙', '순살'],
  '피자': ['피자', 'pizza', '파스타'],
  '중식': ['짜장', '짬뽕', '중국', '탕수육', '마라'],
  '일식': ['초밥', '라멘', '돈카츠', '우동', '스시', '일식'],
  '한식': ['비빔밥', '국밥', '삼겹살', '된장', '불고기', '갈비'],
  '분식': ['떡볶이', '순대', '김밥', '튀김', '라볶이'],
  '버거': ['버거', '햄버거', 'burger', '샌드위치'],
};

const GROUP_FRIENDLY = new Set(['치킨', '피자', '중식', '한식']);

function detectCategory(itemName) {
  for (const [category, keywords] of Object.entries(CATEGORY_KEYWORDS)) {
    if (keywords.some(k => itemName.includes(k))) return category;
  }
  return null;
}

function buildCategoryCount(participants) {
  const counts = {};
  for (const p of participants) {
    for (const item of (p.orderItems || [])) {
      const cat = detectCategory(item.name || '');
      if (cat) counts[cat] = (counts[cat] || 0) + 1;
    }
  }
  return counts;
}

function calculateScore(restaurant, participants, filters) {
  const { category } = restaurant;
  const counts = buildCategoryCount(participants);
  const maxCount = Math.max(...Object.values(counts), 1);

  // Category preference 0-50
  const preferenceScore = Math.round(((counts[category] || 0) / maxCount) * 50);

  // Category filter match 0-30
  const filterScore = (!filters.category || filters.category === category) ? 30 : 0;

  // Participant count suitability 0-20
  const participantCount = participants.length;
  const groupScore =
    participantCount >= 3 && GROUP_FRIENDLY.has(category) ? 20 :
    participantCount >= 3 ? 10 :
    15;

  return Math.min(preferenceScore + filterScore + groupScore, 100);
}

function recommend(participants, filters) {
  const candidates = CATEGORIES.map(category => ({
    restaurantName: category + '집',
    category,
    score: calculateScore({ category }, participants, filters),
  }));

  candidates.sort((a, b) => b.score - a.score);

  return candidates.slice(0, 3).map((item, i) => ({
    rank: i + 1,
    restaurantName: item.restaurantName,
    score: item.score,
    reason: '',
  }));
}

module.exports = { calculateScore, recommend, detectCategory };
