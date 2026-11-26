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
 * 글 상세. 부를 때마다 조회수가 1 오른다(`countView: false`면 올리지 않는다, 글 수정 화면용 #209). 없으면 404 POST_404.
 * @param {number} id
 * @param {{ countView?: boolean }} [options]
 * @returns {Promise<PostDetail>}
 */
export function fetchPost(id, { countView = true } = {}) {
  const config = countView ? undefined : { params: { countView: false } }
  return http.get(`/posts/${id}`, config).then((res) => res.data)
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

// ── 댓글 (F-18, docs/specs/F-18-comments.md 3절). 목록은 로그인 없이 부를 수 있다 ──

/**
 * @typedef {{ id: number, content: string, authorId: number|null, authorNickname: string, edited: boolean, createdAt: string, updatedAt: string }} PostComment
 */

/**
 * 글의 댓글 (최근 200개를 오래된순으로, #208). total이 items 수보다 크면 오래된 댓글은 빠진 것이다. 없는 글이면 404 POST_404.
 * @param {number} postId
 * @returns {Promise<{ items: PostComment[], total: number }>}
 */
export function fetchComments(postId) {
  return http.get(`/posts/${postId}/comments`).then((res) => res.data)
}

/**
 * 댓글 작성 (201). 400 COMMON_400(details.content), 404 POST_404.
 * @param {number} postId
 * @param {{ content: string }} body
 * @returns {Promise<PostComment>}
 */
export function createComment(postId, { content }) {
  return http.post(`/posts/${postId}/comments`, { content }).then((res) => res.data)
}

/**
 * 내 댓글 수정. 400 COMMON_400(details.content), 403 POST_004, 404 POST_404·POST_003.
 * @param {number} postId
 * @param {number} commentId
 * @param {{ content: string }} body
 * @returns {Promise<PostComment>}
 */
export function updateComment(postId, commentId, { content }) {
  return http.put(`/posts/${postId}/comments/${commentId}`, { content }).then((res) => res.data)
}

/**
 * 내 댓글 삭제 (204). 403 POST_004, 404 POST_404·POST_003.
 * @param {number} postId
 * @param {number} commentId
 * @returns {Promise<void>}
 */
export function deleteComment(postId, commentId) {
  return http.delete(`/posts/${postId}/comments/${commentId}`).then(() => undefined)
}
