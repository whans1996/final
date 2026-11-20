import http from '@/api/http'

// 맞춤 개념 카드 추천 (F-16, docs/specs/F-16-recommendation.md 3절). 로그인 필요.

/**
 * @typedef {{ wrongCount: number, inMyCode: boolean, wishedCategory: boolean, levelFit: -1 | 0 | 1 }} RecommendationSignals
 * @typedef {{ concept: object, score: number, signals: RecommendationSignals, reason: string, reasonSource: 'AI' | 'RULE' }} RecommendationItem
 */

/**
 * 점수 순 추천 카드. size(1~10)를 생략하면 서버 기본 5개. 범위 밖이면 400 COMMON_400(details.size).
 * 캐시가 없는 카드의 이유를 LLM으로 만드는 동안 응답이 늦을 수 있다.
 * @param {{ size?: number }} [options]
 * @returns {Promise<{ items: RecommendationItem[] }>}
 */
export function fetchRecommendations({ size } = {}) {
  const params = size ? { size } : undefined
  return http.get('/recommendations', { params }).then((res) => res.data)
}
