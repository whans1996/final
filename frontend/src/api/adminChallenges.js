import http from '@/api/http'

// 관리자 학습 챌린지 관리 (F-24 spec 3절). 모두 ADMIN 토큰이 필요하다 (/api/admin/**).
// 등록·수정은 대표 이미지 때문에 multipart/form-data로 보낸다. Content-Type(경계 포함)은 axios가 FormData를 보고 정한다.

// 2MB 이미지 업로드는 느린 회선에서 기본 15초를 넘길 수 있다
const UPLOAD_TIMEOUT_MS = 60000

const isEmpty = (value) => value === undefined || value === null || value === ''

/**
 * @typedef {'UPCOMING'|'ONGOING'|'ENDED'} ChallengeStatus
 * @typedef {'REVIEW_COUNT'|'QUIZ_COUNT'|'ANALYSIS_COUNT'} ChallengeGoalType
 * @typedef {{ id: number, title: string, imageUrl: string|null, startDate: string, endDate: string, goalType: ChallengeGoalType, goalValue: number, status: ChallengeStatus, participantCount: number }} AdminChallengeSummary
 * @typedef {AdminChallengeSummary & { description: string|null, createdAt: string, updatedAt: string }} AdminChallengeDetail
 * @typedef {{ title: string, description?: string|null, startDate: string, endDate: string, goalType: ChallengeGoalType, goalValue: number|string, image?: File|null, removeImage?: boolean }} AdminChallengeForm
 */

/**
 * 챌린지 목록 (start_date DESC, id DESC). 빈 값은 쿼리에서 뺀다. 400 COMMON_400(details.status·page·size).
 * @param {{ status?: ChallengeStatus, page?: number, size?: number }} query
 * @returns {Promise<{ items: AdminChallengeSummary[], page: number, size: number, total: number }>}
 */
export function fetchAdminChallenges({ status, page, size } = {}) {
  const params = Object.fromEntries(
    Object.entries({ status, page, size }).filter(([, value]) => !isEmpty(value)),
  )
  return http.get('/admin/challenges', { params }).then((res) => res.data)
}

/**
 * 챌린지 상세. 404 CHALLENGE_404.
 * @param {number} id
 * @returns {Promise<AdminChallengeDetail>}
 */
export function fetchAdminChallenge(id) {
  return http.get(`/admin/challenges/${id}`).then((res) => res.data)
}

/** 폼 값 → multipart 파트. 설명이 비면 빈 문자열로 보내 서버가 null로 저장하게 하고, 이미지는 있을 때만 붙인다. */
function toFormData(form, { withRemoveImage }) {
  const data = new FormData()
  data.append('title', form.title)
  data.append('description', form.description ?? '')
  data.append('startDate', form.startDate)
  data.append('endDate', form.endDate)
  data.append('goalType', form.goalType)
  data.append('goalValue', String(form.goalValue))
  if (form.image) data.append('image', form.image)
  if (withRemoveImage) data.append('removeImage', String(Boolean(form.removeImage)))
  return data
}

/**
 * 챌린지 등록. 400 COMMON_400(details에 필드별 사유, 이미지는 details.image), 413 COMMON_413.
 * @param {AdminChallengeForm} form
 * @returns {Promise<{ id: number }>}
 */
export function createAdminChallenge(form) {
  return http
    .post('/admin/challenges', toFormData(form, { withRemoveImage: false }), {
      timeout: UPLOAD_TIMEOUT_MS,
    })
    .then((res) => res.data)
}

/**
 * 챌린지 전체 수정. image가 없고 removeImage가 false면 기존 이미지를 유지한다.
 * 400 COMMON_400, 404 CHALLENGE_404, 409 CHALLENGE_001(시작한 챌린지의 목표·시작일 변경), 413 COMMON_413.
 * @param {number} id
 * @param {AdminChallengeForm} form
 * @returns {Promise<AdminChallengeDetail>}
 */
export function updateAdminChallenge(id, form) {
  return http
    .put(`/admin/challenges/${id}`, toFormData(form, { withRemoveImage: true }), {
      timeout: UPLOAD_TIMEOUT_MS,
    })
    .then((res) => res.data)
}

/**
 * 챌린지 삭제. 참여 기록과 이미지도 함께 지워진다. 404 CHALLENGE_404.
 * @param {number} id
 * @returns {Promise<void>}
 */
export function deleteAdminChallenge(id) {
  return http.delete(`/admin/challenges/${id}`).then(() => undefined)
}
