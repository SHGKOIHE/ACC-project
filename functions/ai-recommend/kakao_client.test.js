'use strict';

const { fetchCandidatesByCategory } = require('./kakao_client');

const ORIGINAL_FETCH = global.fetch;
const ORIGINAL_KEY = process.env.KAKAO_REST_API_KEY;

afterEach(() => {
  global.fetch = ORIGINAL_FETCH;
  if (ORIGINAL_KEY === undefined) delete process.env.KAKAO_REST_API_KEY;
  else process.env.KAKAO_REST_API_KEY = ORIGINAL_KEY;
});

test('KAKAO_REST_API_KEY 미설정 시 네트워크 호출 없이 빈 객체를 반환한다', async () => {
  delete process.env.KAKAO_REST_API_KEY;
  global.fetch = jest.fn();

  const result = await fetchCandidatesByCategory(['치킨', '피자']);

  expect(result).toEqual({});
  expect(global.fetch).not.toHaveBeenCalled();
});

test('정상 응답을 카테고리별 후보 목록으로 파싱한다', async () => {
  process.env.KAKAO_REST_API_KEY = 'test-key';
  global.fetch = jest.fn().mockResolvedValue({
    ok: true,
    json: async () => ({
      documents: [
        { place_name: '경희치킨', road_address_name: '경기 용인시 기흥구 deogyeong-daero 1732', distance: '350', place_url: 'https://place.map.kakao.com/1' },
        { place_name: '국제캠치킨', address_name: '경기 용인시 기흥구 서천동', distance: '420', place_url: 'https://place.map.kakao.com/2' },
      ],
    }),
  });

  const result = await fetchCandidatesByCategory(['치킨']);

  expect(global.fetch).toHaveBeenCalledTimes(1);
  const [calledUrl, calledOptions] = global.fetch.mock.calls[0];
  expect(String(calledUrl)).toContain('dapi.kakao.com/v2/local/search/keyword.json');
  expect(calledOptions.headers.Authorization).toBe('KakaoAK test-key');

  expect(result['치킨']).toEqual([
    { name: '경희치킨', address: '경기 용인시 기흥구 deogyeong-daero 1732', distanceMeters: 350, placeUrl: 'https://place.map.kakao.com/1' },
    { name: '국제캠치킨', address: '경기 용인시 기흥구 서천동', distanceMeters: 420, placeUrl: 'https://place.map.kakao.com/2' },
  ]);
});

test('일부 카테고리 호출이 실패해도 나머지 카테고리 결과는 유지한다', async () => {
  process.env.KAKAO_REST_API_KEY = 'test-key';
  global.fetch = jest.fn()
    .mockResolvedValueOnce({ ok: false, status: 500 })
    .mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        documents: [{ place_name: '경희피자', road_address_name: '경기 용인시', distance: '200', place_url: 'https://x' }],
      }),
    });

  const result = await fetchCandidatesByCategory(['치킨', '피자']);

  expect(result).not.toHaveProperty('치킨');
  expect(result['피자']).toHaveLength(1);
});

test('요청이 예외를 던져도 해당 카테고리는 빈 결과로 처리되어 전체가 실패하지 않는다', async () => {
  process.env.KAKAO_REST_API_KEY = 'test-key';
  global.fetch = jest.fn().mockRejectedValue(new Error('network down'));

  const result = await fetchCandidatesByCategory(['치킨']);

  expect(result).toEqual({});
});
