import http from '@/api/http'

// 개념 카드 목록·검색 (F-09)·상세 (F-10). 각 spec 3절. 공개 API라 로그인 없이 부를 수 있다.

const isEmpty = (value) => value === undefined || value === null || value === ''

/**
 * 공개 카드 목록. 빈 값(빈 문자열·null·undefined)은 쿼리에서 뺀다.
 * @param {{ keyword?: string, category?: string, difficulty?: string, sort?: 'POPULAR'|'LATEST', page?: number, size?: number }} query
 * @returns {Promise<{ items: { id: number, name: string, category: string, difficulty: string, summary: string, wishCount: number, avgRating: number }[], page: number, size: number, total: number }>}
 */
export function fetchConcepts({ keyword, category, difficulty, sort, page, size } = {}) {
  const params = Object.fromEntries(
    Object.entries({ keyword, category, difficulty, sort, page, size }).filter(
      ([, value]) => !isEmpty(value),
    ),
  )
  return http.get('/concepts', { params }).then((res) => res.data)
}

/**
 * 공개 카드 상세 (F-10). 로그인 상태면 인터셉터가 토큰을 붙여 appearances가 채워진다.
 * 없거나 비공개 카드면 404 CONCEPT_404.
 * @param {number} id
 * @returns {Promise<{ id: number, name: string, category: string, difficulty: string, summary: string, body: string|null, exampleCode: string|null, referenceUrl: string|null, wishCount: number, avgRating: number, appearances: { analysisId: number, analysisTitle: string, fileId: number|null, filePath: string|null, lineHint: string|null, contextNote: string|null }[], related: { id: number, name: string, category: string, difficulty: string, summary: string, wishCount: number, avgRating: number }[] }>}
 */
export function fetchConcept(id) {
  return http.get(`/concepts/${id}`).then((res) => res.data)
}
