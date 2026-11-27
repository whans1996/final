// 헤더 검색 대상 (F-29b AC9). 지금 keyword 검색 API가 있는 목록 화면만 둔다.
// 챌린지·팀은 keyword API가 생기면 여기에 한 줄을 더한다. filters의 'category'는 개념 카드 카테고리 선택을 켠다.
export const SEARCH_TARGETS = [
  { key: 'concepts', label: '개념 카드', to: '/concepts', filters: ['category'] }, // GET /api/concepts
  { key: 'posts', label: '커뮤니티', to: '/community' }, // GET /api/posts
]

/** 검색 대상·검색어·필터로 이동할 위치를 만든다. 검색어가 비면(공백만이면) null */
export function searchLocation(targetKey, keyword, filters = {}) {
  const target = SEARCH_TARGETS.find((t) => t.key === targetKey) ?? SEARCH_TARGETS[0]
  const text = String(keyword ?? '').trim()
  if (!text) return null
  const query = { keyword: text }
  for (const name of target.filters ?? []) {
    if (filters[name]) query[name] = filters[name]
  }
  return { path: target.to, query }
}
