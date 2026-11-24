import http from '@/api/http'

// 학습 챌린지 목록·상세, 참여·취소, 참여한 챌린지 목록 (F-25 spec 3절).
// 목록·상세·회원별 목록은 공개, 참여·취소·내 목록은 로그인이 필요하다.

export const CHALLENGE_PAGE_SIZE = 12

const isEmpty = (value) => value === undefined || value === null || value === ''

/** 빈 값은 쿼리에서 뺀다 (서버 기본값을 쓰게) */
const toParams = (query) =>
  Object.fromEntries(Object.entries(query).filter(([, value]) => !isEmpty(value)))

/**
 * @typedef {'UPCOMING'|'ONGOING'|'ENDED'} ChallengeStatus
 * @typedef {'REVIEW_COUNT'|'QUIZ_COUNT'|'ANALYSIS_COUNT'} ChallengeGoalType
 * @typedef {{ progress: number, goalValue: number, completedAt: string|null, joinedAt: string }} Participation
 * @typedef {{ id: number, title: string, imageUrl: string|null, startDate: string, endDate: string, goalType: ChallengeGoalType, goalValue: number, status: ChallengeStatus, participantCount: number, joined: boolean }} ChallengeSummary
 * @typedef {Omit<ChallengeSummary, 'joined'> & { description: string|null, participation: Participation|null }} ChallengeDetail
 * @typedef {{ joined: boolean, participantCount: number, participation: Participation|null }} ParticipationResult
 * @typedef {Omit<ChallengeSummary, 'participantCount'|'joined'> & { progress: number, completedAt: string|null, joinedAt: string }} MyChallenge
 * @typedef {{ status?: ChallengeStatus|'', page?: number, size?: number }} ChallengeQuery
 */

/**
 * 챌린지 목록. status 기본 ONGOING. 로그인했으면 joined를 계산한다. 400 COMMON_400.
 * @param {ChallengeQuery} query
 * @returns {Promise<{ items: ChallengeSummary[], page: number, size: number, total: number }>}
 */
export function fetchChallenges({ status, page, size } = {}) {
  return http
    .get('/challenges', { params: toParams({ status, page, size }) })
    .then((res) => res.data)
}

/**
 * 챌린지 상세. 참여하지 않았거나 비로그인이면 participation null. 404 CHALLENGE_404.
 * @param {number} id
 * @returns {Promise<ChallengeDetail>}
 */
export function fetchChallenge(id) {
  return http.get(`/challenges/${id}`).then((res) => res.data)
}

/**
 * 참여. 멱등. 404 CHALLENGE_404, 409 CHALLENGE_002(종료).
 * @param {number} id
 * @returns {Promise<ParticipationResult>}
 */
export function joinChallenge(id) {
  return http.post(`/challenges/${id}/participation`).then((res) => res.data)
}

/**
 * 참여 취소. 멱등. 404 CHALLENGE_404, 409 CHALLENGE_002(종료)·CHALLENGE_003(달성).
 * @param {number} id
 * @returns {Promise<ParticipationResult>}
 */
export function cancelChallenge(id) {
  return http.delete(`/challenges/${id}/participation`).then((res) => res.data)
}

/**
 * 내가 참여한 챌린지 (joined_at DESC). status가 없으면 전체. 400 COMMON_400.
 * @param {ChallengeQuery} query
 * @returns {Promise<{ items: MyChallenge[], page: number, size: number, total: number }>}
 */
export function fetchMyChallenges({ status, page, size } = {}) {
  return http
    .get('/users/me/challenges', { params: toParams({ status, page, size }) })
    .then((res) => res.data)
}

/**
 * 회원이 참여한 챌린지 (공개). 404 USER_404(없거나 탈퇴).
 * @param {number} userId
 * @param {ChallengeQuery} query
 * @returns {Promise<{ items: MyChallenge[], page: number, size: number, total: number }>}
 */
export function fetchUserChallenges(userId, { status, page, size } = {}) {
  return http
    .get(`/users/${userId}/challenges`, { params: toParams({ status, page, size }) })
    .then((res) => res.data)
}
