import http from '@/api/http'

// 커뮤니티 게시글 (F-17, docs/specs/F-17-community-board.md 3절). 목록·상세는 로그인 없이 부를 수 있다.

const isEmpty = (value) => value === undefined || value === null || value === ''

/**
 * @typedef {'QNA'|'REVIEW'|'FREE'} BoardType
 * @typedef {{ id: number, boardType: BoardType, title: string, attachmentType: 'ANALYSIS'|'CONCEPT'|null, commentCount: number, viewCount: number, authorId: number|null, authorNickname: string, createdAt: string }} PostSummary
 * @typedef {{ id: number, viewable: boolean, title: string|null, projectName: string|null }} PostAnalysisAttachment
 * @typedef {{ id: number, viewable: boolean, name: string|null, category: string|null, difficulty: string|null, summary: string|null }} PostConceptAttachment
 * @typedef {{ id: number, boardType: BoardType, title: string, content: string, authorId: number|null, authorNickname: string, viewCount: number, commentCount: number, analysis: PostAnalysisAttachment|null, concept: PostConceptAttachment|null, createdAt: string, updatedAt: string }} PostDetail
 * @typedef {{ boardType: BoardType, title: string, content: string, analysisId: number|null, conceptId: number|null }} PostBody
 */

/**
 * 글 목록 (최신순). 빈 값(빈 문자열·null·undefined)은 쿼리에서 뺀다.
 * @param {{ boardType?: BoardType|'', keyword?: string, page?: number, size?: number }} query
 * @returns {Promise<{ items: PostSummary[], page: number, size: number, total: number }>}
 */
export function fetchPosts({ boardType, keyword, page, size } = {}) {
  const params = Object.fromEntries(
    Object.entries({ boardType, keyword, page, size }).filter(([, value]) => !isEmpty(value)),
  )
  return http.get('/posts', { params }).then((res) => res.data)
}

/**
 * 글 상세. 부를 때마다 조회수가 1 오른다. 없으면 404 POST_404.
 * @param {number} id
 * @returns {Promise<PostDetail>}
 */
export function fetchPost(id) {
  return http.get(`/posts/${id}`).then((res) => res.data)
}

/**
 * 글 작성. 400 COMMON_400(details)·POST_001·POST_002.
 * @param {PostBody} body
 * @returns {Promise<{ id: number }>}
 */
export function createPost({ boardType, title, content, analysisId, conceptId }) {
  return http
    .post('/posts', { boardType, title, content, analysisId, conceptId })
    .then((res) => res.data)
}

/**
 * 내 글 수정 (보낸 값으로 통째로 바꾼다). 403 POST_403, 404 POST_404.
 * @param {number} id
 * @param {PostBody} body
 * @returns {Promise<PostDetail>}
 */
export function updatePost(id, { boardType, title, content, analysisId, conceptId }) {
  return http
    .put(`/posts/${id}`, { boardType, title, content, analysisId, conceptId })
    .then((res) => res.data)
}

/**
 * 내 글 삭제 (204). 403 POST_403, 404 POST_404.
 * @param {number} id
 * @returns {Promise<void>}
 */
export function deletePost(id) {
  return http.delete(`/posts/${id}`).then(() => undefined)
}
