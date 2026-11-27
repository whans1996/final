import http from '@/api/http'

// 관리자 개념 카드 관리 (F-13 spec 3절). 모두 ADMIN 토큰이 필요하다 (/api/admin/**).

const isEmpty = (value) => value === undefined || value === null || value === ''

/**
 * @typedef {'DRAFT'|'PUBLISHED'|'HIDDEN'} ConceptStatus
 * @typedef {'SEED'|'AI'|'ADMIN'} ConceptSource
 * @typedef {{ level: 'BEGINNER'|'JUNIOR'|'SENIOR', summary: string|null, body: string|null }} ConceptLevel 수준별 요약·본문 (K-1)
 * @typedef {{ id: number, name: string, category: string, difficulty: string, summary: string, status: ConceptStatus, source: ConceptSource, wishCount: number, avgRating: number, updatedAt: string }} AdminConceptSummary
 * @typedef {{ id: number, name: string, category: string, difficulty: string, summary: string, body: string|null, exampleCode: string|null, referenceUrl: string|null, status: ConceptStatus, source: ConceptSource, wishCount: number, avgRating: number, reviewCount: number, linkedAnalysisCount: number, createdAt: string, updatedAt: string, levels: ConceptLevel[] }} AdminConceptDetail levels는 언제나 BEGINNER·JUNIOR·SENIOR 순 3개 (K-1)
 * @typedef {{ name: string, category: string, difficulty: string, summary: string, body: string|null, exampleCode: string|null, referenceUrl: string|null, status: ConceptStatus, levels?: ConceptLevel[]|null }} AdminConceptBody levels를 빼거나 null이면 PUT은 기존 수준별 칸을 유지하고, 보내면 6칸을 통째로 바꾼다 (K-1)
 */

/**
 * 상태·출처와 상관없는 카드 목록 (updated_at 최신순). 빈 값은 쿼리에서 뺀다. 400 COMMON_400(details.status·source).
 * @param {{ status?: ConceptStatus, source?: ConceptSource, keyword?: string, page?: number, size?: number }} query
 * @param {{ signal?: AbortSignal }} [options] 취소용 signal (V-4)
 * @returns {Promise<{ items: AdminConceptSummary[], page: number, size: number, total: number }>}
 */
export function fetchAdminConcepts({ status, source, keyword, page, size } = {}, { signal } = {}) {
  const params = Object.fromEntries(
    Object.entries({ status, source, keyword, page, size }).filter(([, value]) => !isEmpty(value)),
  )
  return http.get('/admin/concepts', { params, signal }).then((res) => res.data)
}

/**
 * 상태별 카드 수. 세 키가 항상 온다.
 * @returns {Promise<{ DRAFT: number, PUBLISHED: number, HIDDEN: number }>}
 */
export function fetchAdminConceptCounts() {
  return http.get('/admin/concepts/status-counts').then((res) => res.data)
}

/**
 * 카드 상세 (상태와 상관없이). 404 CONCEPT_404.
 * @param {number} id
 * @returns {Promise<AdminConceptDetail>}
 */
export function fetchAdminConcept(id) {
  return http.get(`/admin/concepts/${id}`).then((res) => res.data)
}

/**
 * 카드 생성 (source=ADMIN, status는 DRAFT·PUBLISHED). 400 COMMON_400, 409 CONCEPT_002.
 * @param {AdminConceptBody} body
 * @returns {Promise<{ id: number }>}
 */
export function createAdminConcept(body) {
  return http.post('/admin/concepts', body).then((res) => res.data)
}

/**
 * 카드 전체 수정. 400 COMMON_400(시드 이름 변경 포함), 404 CONCEPT_404, 409 CONCEPT_002·CONCEPT_003.
 * @param {number} id
 * @param {AdminConceptBody} body
 * @returns {Promise<AdminConceptDetail>}
 */
export function updateAdminConcept(id, body) {
  return http.put(`/admin/concepts/${id}`, body).then((res) => res.data)
}

/**
 * 초안(DRAFT) 삭제. 분석 연결도 함께 지워진다. 404 CONCEPT_404, 409 CONCEPT_003.
 * @param {number} id
 * @returns {Promise<void>}
 */
export function deleteAdminConcept(id) {
  return http.delete(`/admin/concepts/${id}`).then(() => undefined)
}
