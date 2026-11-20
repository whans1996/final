// 피드 활동 종류별 문장과 대상 링크 (F-04 AC11, SC-17). 여기 없는 type은 그리지 않는다.
export const FEED_TYPES = {
  ANALYSIS: { sentence: '새 분석을 공개했어요', to: (id) => `/analyses/${id}` },
  REVIEW: { sentence: '카드에 리뷰를 남겼어요', to: (id) => `/concepts/${id}` },
  WISH: { sentence: '카드를 찜했어요', to: (id) => `/concepts/${id}` },
  POST: { sentence: '글을 썼어요', to: (id) => `/community/${id}` },
  CHALLENGE_JOIN: { sentence: '챌린지에 참여했어요', to: (id) => `/challenges/${id}` },
  CHALLENGE_COMPLETE: { sentence: '챌린지를 달성했어요', to: (id) => `/challenges/${id}` },
}

export function isKnownFeedType(type) {
  return Object.hasOwn(FEED_TYPES, type)
}
