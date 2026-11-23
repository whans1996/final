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
