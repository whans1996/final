import http from '@/api/http'

// 개념 카드 목록·검색 (F-09, docs/specs/F-09-concept-list.md 3절). 공개 API라 로그인 없이 부를 수 있다.

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
