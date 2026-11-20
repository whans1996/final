import http from '@/api/http'

// 팔로우·언팔로우, 팔로우 상태, 팔로워·팔로잉 목록 (F-04, docs/specs/F-04-follow-feed.md 3절)

export const FOLLOW_PAGE_SIZE = 20

/**
 * @typedef {{ following: boolean, followerCount: number, followingCount: number }} FollowStatus
 * @typedef {{ id: number, nickname: string, level: string, followedAt: string }} FollowUser
 */

/** 공개. 비로그인·내 프로필이면 following=false. 없거나 탈퇴했으면 404 USER_404. @returns {Promise<FollowStatus>} */
export function fetchFollowStatus(id) {
  return http.get(`/users/${id}/follow`).then((res) => res.data)
}

/** 멱등 (이미 팔로우 중이어도 200). 자기 자신이면 400 USER_002. @returns {Promise<FollowStatus>} */
export function follow(id) {
  return http.post(`/users/${id}/follow`).then((res) => res.data)
}

/** 멱등 (팔로우하지 않았어도 200). @returns {Promise<FollowStatus>} */
export function unfollow(id) {
  return http.delete(`/users/${id}/follow`).then((res) => res.data)
}

/** @returns {Promise<{ items: FollowUser[], page: number, size: number, total: number }>} */
export function fetchFollowers(id, page = 1) {
  return http
    .get(`/users/${id}/followers`, { params: { page, size: FOLLOW_PAGE_SIZE } })
    .then((res) => res.data)
}

/** @returns {Promise<{ items: FollowUser[], page: number, size: number, total: number }>} */
export function fetchFollowings(id, page = 1) {
  return http
    .get(`/users/${id}/followings`, { params: { page, size: FOLLOW_PAGE_SIZE } })
    .then((res) => res.data)
}
