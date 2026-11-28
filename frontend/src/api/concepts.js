import http from '@/api/http'

// 개념 카드 목록·검색 (F-09)·상세 (F-10)·리뷰 (F-11)·찜 (F-12). 각 spec 3절. 목록·상세·리뷰 목록·찜 목록은 로그인 없이 부를 수 있다.

const isEmpty = (value) => value === undefined || value === null || value === ''

/**
 * 공개 카드 목록. 빈 값(빈 문자열·null·undefined)은 쿼리에서 뺀다.
 * @param {{ keyword?: string, category?: string, difficulty?: string, sort?: 'POPULAR'|'LATEST', page?: number, size?: number }} query
 * @param {{ signal?: AbortSignal }} [options] 취소용 signal (V-4)
 * @returns {Promise<{ items: { id: number, name: string, category: string, difficulty: string, summary: string, wishCount: number, avgRating: number }[], page: number, size: number, total: number }>}
 */
export function fetchConcepts(
  { keyword, category, difficulty, sort, page, size } = {},
  { signal } = {},
) {
  const params = Object.fromEntries(
    Object.entries({ keyword, category, difficulty, sort, page, size }).filter(
      ([, value]) => !isEmpty(value),
    ),
  )
  return http.get('/concepts', { params, signal }).then((res) => res.data)
}

/**
 * 공개 카드 상세 (F-10). 로그인 상태면 인터셉터가 토큰을 붙여 appearances가 채워진다.
 * 없거나 비공개 카드면 404 CONCEPT_404.
 * K-1: viewerLevel은 로그인 회원의 수준(비로그인이면 null), levels는 언제나 BEGINNER·JUNIOR·SENIOR 순 3개이고
 * 없는 summary·body는 null이다. 화면은 viewerLevel의 body가 있으면 그것을, 없으면 공통 body + 그 수준 summary를 보인다.
 * @param {number} id
 * @returns {Promise<{ id: number, name: string, category: string, difficulty: string, summary: string, body: string|null, exampleCode: string|null, referenceUrl: string|null, wishCount: number, wished: boolean, avgRating: number, appearances: { analysisId: number, analysisTitle: string, fileId: number|null, filePath: string|null, lineHint: string|null, contextNote: string|null }[], related: { id: number, name: string, category: string, difficulty: string, summary: string, wishCount: number, avgRating: number }[], viewerLevel: 'BEGINNER'|'JUNIOR'|'SENIOR'|null, levels: { level: 'BEGINNER'|'JUNIOR'|'SENIOR', summary: string|null, body: string|null }[] }>}
 */
export function fetchConcept(id) {
  return http.get(`/concepts/${id}`).then((res) => res.data)
}

// ── 카드 리뷰 (F-11, spec 3절). 목록만 공개, 나머지는 로그인 필요 ──

/**
 * @typedef {{ id: number, rating: number, content: string, authorId: number|null, authorNickname: string, createdAt: string }} ConceptReview
 * @typedef {{ rating: number, content: string }} ConceptReviewBody
 * @typedef {{ review: ConceptReview, avgRating: number, reviewCount: number }} ConceptReviewSaved
 */

/**
 * 카드 리뷰 목록 (최신순).
 * @param {number} conceptId
 * @param {{ page?: number, size?: number }} query
 * @returns {Promise<{ items: ConceptReview[], page: number, size: number, total: number }>}
 */
export function fetchReviews(conceptId, { page, size } = {}) {
  const params = Object.fromEntries(
    Object.entries({ page, size }).filter(([, value]) => !isEmpty(value)),
  )
  return http.get(`/concepts/${conceptId}/reviews`, { params }).then((res) => res.data)
}

/**
 * 이 카드에 내가 쓴 리뷰. 없으면(204) null.
 * @param {number} conceptId
 * @returns {Promise<ConceptReview|null>}
 */
export function fetchMyReview(conceptId) {
  return http
    .get(`/concepts/${conceptId}/reviews/me`)
    .then((res) => (res.status === 204 || !res.data ? null : res.data))
}

/**
 * 리뷰 작성. 400 COMMON_400(details), 409 CONCEPT_409.
 * @param {number} conceptId
 * @param {ConceptReviewBody} body
 * @returns {Promise<ConceptReviewSaved>}
 */
export function createReview(conceptId, { rating, content }) {
  return http.post(`/concepts/${conceptId}/reviews`, { rating, content }).then((res) => res.data)
}

/**
 * 내 리뷰 수정. 404 CONCEPT_001(내 리뷰 없음).
 * @param {number} conceptId
 * @param {ConceptReviewBody} body
 * @returns {Promise<ConceptReviewSaved>}
 */
export function updateReview(conceptId, { rating, content }) {
  return http.put(`/concepts/${conceptId}/reviews/me`, { rating, content }).then((res) => res.data)
}

/**
 * 내 리뷰 삭제. 다시 계산한 평점을 돌려준다.
 * @param {number} conceptId
 * @returns {Promise<{ avgRating: number, reviewCount: number }>}
 */
export function deleteReview(conceptId) {
  return http.delete(`/concepts/${conceptId}/reviews/me`).then((res) => res.data)
}

// ── 카드 찜 (F-12, spec 3절). 찜·해제는 로그인 필요, 회원의 찜 목록은 공개 ──

/**
 * @typedef {{ wished: boolean, wishCount: number, reviewPlanDate: string|null }} ConceptWishResult
 */

/**
 * 카드 찜 (멱등). 새로 복습 계획이 생겼으면 reviewPlanDate에 날짜. 404 CONCEPT_404.
 * @param {number} conceptId
 * @returns {Promise<ConceptWishResult>}
 */
export function wishConcept(conceptId) {
  return http.post(`/concepts/${conceptId}/wish`).then((res) => res.data)
}

/**
 * 카드 찜 해제 (멱등). 바뀐 찜 수를 돌려준다.
 * @param {number} conceptId
 * @returns {Promise<ConceptWishResult>}
 */
export function unwishConcept(conceptId) {
  return http.delete(`/concepts/${conceptId}/wish`).then((res) => res.data)
}

/**
 * 회원이 찜한 공개 카드 (찜한 시각 최신순). 404 USER_404.
 * @param {number} userId
 * @param {{ page?: number, size?: number }} query
 * @returns {Promise<{ items: { id: number, name: string, category: string, difficulty: string, summary: string, wishCount: number, avgRating: number }[], page: number, size: number, total: number }>}
 */
export function fetchUserWishes(userId, { page, size } = {}) {
  const params = Object.fromEntries(
    Object.entries({ page, size }).filter(([, value]) => !isEmpty(value)),
  )
  return http.get(`/users/${userId}/wishes`, { params }).then((res) => res.data)
}
