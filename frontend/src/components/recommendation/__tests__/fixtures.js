/** BE RecommendationItemResponse 모양의 테스트 데이터 (F-16 3.1절). */
export function recommendation(id, partial = {}) {
  return {
    concept: {
      id,
      name: `카드${id}`,
      category: 'security',
      difficulty: 'JUNIOR',
      summary: `카드${id} 요약`,
      avgRating: 4.5,
      wishCount: 2,
    },
    score: 10,
    signals: { wrongCount: 2, inMyCode: true, wishedCategory: true, levelFit: 1 },
    reason: `카드${id}를 볼 이유`,
    reasonSource: 'AI',
    ...partial,
  }
}
