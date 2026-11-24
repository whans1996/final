import http from '@/api/http'

// 팀 생성·초대 코드 가입·멤버 관리 (F-20a, docs/specs/F-20a-team.md 3절). 모두 로그인 필요.

export const TEAM_NAME_MAX = 30
export const TEAM_DESCRIPTION_MAX = 200
export const INVITE_CODE_LENGTH = 8

/**
 * @typedef {'OWNER' | 'MEMBER'} TeamRole
 * @typedef {{ id: number, name: string, description: string | null, myRole: TeamRole,
 *   memberCount: number, ownerNickname: string, joinedAt: string }} MyTeam
 * @typedef {{ userId: number, nickname: string, level: string, role: TeamRole, joinedAt: string }} TeamMember
 * @typedef {{ id: number, name: string, description: string | null, myRole: TeamRole,
 *   inviteCode: string | null, memberCount: number, maxMembers: number, createdAt: string,
 *   members: TeamMember[] }} TeamDetail
 */

/** 내가 가입한 시각 최신순, 최대 5개. @returns {Promise<MyTeam[]>} */
export function fetchMyTeams() {
  return http.get('/users/me/teams').then((res) => res.data)
}

/** 400 COMMON_400(name·description), 409 TEAM_003. @returns {Promise<{ id: number, inviteCode: string }>} */
export function createTeam(body) {
  return http.post('/teams', body).then((res) => res.data)
}

/** 멤버만. inviteCode는 팀장에게만 값이 있다. 404 TEAM_404. @returns {Promise<TeamDetail>} */
export function fetchTeam(id) {
  return http.get(`/teams/${id}`).then((res) => res.data)
}

/** 팀장. 이름·소개를 통째로 바꾼다. @returns {Promise<TeamDetail>} */
export function updateTeam(id, body) {
  return http.put(`/teams/${id}`, body).then((res) => res.data)
}

/** 팀장. 204. */
export function deleteTeam(id) {
  return http.delete(`/teams/${id}`).then(() => undefined)
}

/** 팀장. 이전 코드는 바로 무효다. @returns {Promise<{ inviteCode: string }>} */
export function regenerateInviteCode(id) {
  return http.post(`/teams/${id}/invite-code`).then((res) => res.data)
}

/** 404 TEAM_001, 409 TEAM_409·TEAM_002·TEAM_003, 400 COMMON_400(inviteCode). @returns {Promise<{ teamId: number }>} */
export function joinTeam(inviteCode) {
  return http.post('/teams/join', { inviteCode }).then((res) => res.data)
}

/** 팀원 탈퇴. 팀장이면 400 TEAM_004. 204. */
export function leaveTeam(id) {
  return http.delete(`/teams/${id}/members/me`).then(() => undefined)
}

/** 팀장이 팀원을 내보낸다. 204. 404 TEAM_005, 400 TEAM_004(자기 자신). */
export function removeTeamMember(id, userId) {
  return http.delete(`/teams/${id}/members/${userId}`).then(() => undefined)
}

// ── 공유 분석·팀 이해도 (F-20b, docs/specs/F-20b-team-understanding.md 3절) ──

export const MAX_TEAM_ANALYSES = 10

/**
 * @typedef {{ attemptId: number, score: number, total: number, createdAt: string }} LastAttempt
 * @typedef {{ userId: number, nickname: string }} SharedBy
 * @typedef {{ analysisId: number, title: string | null, projectName: string, level: string,
 *   sharedBy: SharedBy, sharedAt: string, questionCount: number, solvedMemberCount: number,
 *   myLastAttempt: LastAttempt | null, canUnshare: boolean }} TeamAnalysis
 * @typedef {{ layer: string, questionCount: number, correctRate: number | null }} ModuleUnderstanding
 * @typedef {{ userId: number, nickname: string, role: TeamRole, lastAttempt: LastAttempt | null,
 *   modules: { layer: string, correct: number, total: number }[] }} MemberUnderstanding
 * @typedef {{ analysisId: number, title: string | null, projectName: string, sharedBy: SharedBy,
 *   questionCount: number,
 *   memberCount: number, solvedMemberCount: number, modules: ModuleUnderstanding[],
 *   members: MemberUnderstanding[] }} TeamUnderstanding
 */

/** 멤버. 공유 최신순, 최대 10개. 404 TEAM_404. @returns {Promise<TeamAnalysis[]>} */
export function fetchTeamAnalyses(id) {
  return http.get(`/teams/${id}/analyses`).then((res) => res.data)
}

/**
 * 내 DONE·퀴즈 있는 분석을 공유한다. 400 COMMON_400, 404 TEAM_404·ANALYSIS_404,
 * 409 TEAM_009·TEAM_007·TEAM_008. @returns {Promise<{ analysisId: number, sharedAt: string }>}
 */
export function shareTeamAnalysis(id, analysisId) {
  return http.post(`/teams/${id}/analyses`, { analysisId }).then((res) => res.data)
}

/** 공유한 사람·팀장. 204. 403 TEAM_010, 404 TEAM_006. */
export function unshareTeamAnalysis(id, analysisId) {
  return http.delete(`/teams/${id}/analyses/${analysisId}`).then(() => undefined)
}

/** 멤버. 404 TEAM_404·TEAM_006. @returns {Promise<TeamUnderstanding>} */
export function fetchTeamUnderstanding(id, analysisId) {
  return http.get(`/teams/${id}/analyses/${analysisId}/understanding`).then((res) => res.data)
}

// ── 팀 기간 목표 (F-20c, docs/specs/F-20c-team-challenge.md 3절) ──

/**
 * @typedef {'UPCOMING' | 'ONGOING' | 'ENDED'} TeamChallengeStatus
 * @typedef {{ userId: number, nickname: string, role: TeamRole, attempt: LastAttempt | null,
 *   achieved: boolean }} TeamChallengeMember
 * @typedef {{ challengeId: number, analysisId: number, title: string | null, projectName: string,
 *   startDate: string, endDate: string, targetRate: number, status: TeamChallengeStatus,
 *   memberCount: number, solvedMemberCount: number, achievedMemberCount: number,
 *   achieved: boolean, members: TeamChallengeMember[], createdAt: string }} TeamChallenge
 */

/**
 * 멤버. 진행 중 → 예정 → 종료 순, 최대 20개. analysisId를 주면 그 분석의 목표만.
 * 404 TEAM_404. @returns {Promise<TeamChallenge[]>}
 */
export function fetchTeamChallenges(id, { analysisId } = {}) {
  const params = analysisId == null ? undefined : { analysisId }
  return http.get(`/teams/${id}/challenges`, { params }).then((res) => res.data)
}

/**
 * 팀장. body `{ analysisId, startDate, endDate, targetRate }` (날짜 YYYY-MM-DD).
 * 400 COMMON_400, 403 TEAM_403, 404 TEAM_006, 409 TEAM_011·TEAM_012. @returns {Promise<TeamChallenge>}
 */
export function createTeamChallenge(id, body) {
  return http.post(`/teams/${id}/challenges`, body).then((res) => res.data)
}

/** 팀장. 204. 403 TEAM_403, 404 TEAM_013. */
export function deleteTeamChallenge(id, challengeId) {
  return http.delete(`/teams/${id}/challenges/${challengeId}`).then(() => undefined)
}
