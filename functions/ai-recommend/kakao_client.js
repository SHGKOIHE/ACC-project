'use strict';

// 경희대학교 국제캠퍼스 정문 인근 좌표 (반경 검색 기준점).
const KHU_INTERNATIONAL_CAMPUS = { longitude: '127.0798', latitude: '37.2454' };

const CATEGORY_GROUP_CODE = 'FD6'; // 카카오맵 카테고리 그룹: 음식점
const SEARCH_RADIUS_M = Number(process.env.KAKAO_SEARCH_RADIUS_M) || 1500;
const RESULTS_PER_CATEGORY = 3;
const TIMEOUT_MS = 3000;

async function searchByCategory(category, apiKey) {
  const url = new URL('https://dapi.kakao.com/v2/local/search/keyword.json');
  url.searchParams.set('query', `경희대 국제캠퍼스 ${category}`);
  url.searchParams.set('x', KHU_INTERNATIONAL_CAMPUS.longitude);
  url.searchParams.set('y', KHU_INTERNATIONAL_CAMPUS.latitude);
  url.searchParams.set('radius', String(SEARCH_RADIUS_M));
  url.searchParams.set('category_group_code', CATEGORY_GROUP_CODE);
  url.searchParams.set('sort', 'distance');
  url.searchParams.set('size', String(RESULTS_PER_CATEGORY));

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);

  try {
    const response = await fetch(url, {
      headers: { Authorization: `KakaoAK ${apiKey}` },
      signal: controller.signal,
    });

    if (!response.ok) {
      console.warn(`Kakao local search failed for ${category}: ${response.status}`);
      return [];
    }

    const data = await response.json();
    return (data.documents || []).map(doc => ({
      name: doc.place_name,
      address: doc.road_address_name || doc.address_name,
      distanceMeters: Number(doc.distance) || null,
      placeUrl: doc.place_url,
    }));
  } catch (e) {
    console.warn(`Kakao local search error for ${category}:`, e.message);
    return [];
  } finally {
    clearTimeout(timer);
  }
}

// 카테고리별 실제 식당 후보를 병렬로 조회한다. KAKAO_REST_API_KEY 미설정 시
// 네트워크 호출 없이 빈 결과를 반환해 기존(카테고리명 기반) 동작을 그대로 유지한다.
async function fetchCandidatesByCategory(categories) {
  const apiKey = process.env.KAKAO_REST_API_KEY;
  if (!apiKey) return {};

  const results = await Promise.allSettled(
    categories.map(category => searchByCategory(category, apiKey))
  );

  const byCategory = {};
  categories.forEach((category, index) => {
    const result = results[index];
    const restaurants = result.status === 'fulfilled' ? result.value : [];
    if (restaurants.length > 0) byCategory[category] = restaurants;
  });

  return byCategory;
}

module.exports = { fetchCandidatesByCategory, KHU_INTERNATIONAL_CAMPUS };
