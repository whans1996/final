import http from '@/api/http'

// 활동 피드 (F-04 AC6~AC8, docs/specs/F-04-follow-feed.md 3절). 로그인 필요

export const FEED_PAGE_SIZE = 20

/**
 * @typedef {'ANALYSIS'|'REVIEW'|'WISH'|'POST'|'CHALLENGE_JOIN'|'CHALLENGE_COMPLETE'} ActivityType
 * @typedef {{
 *   id: number, type: ActivityType, createdAt: string,
 *   actor: { id: number, nickname: string },
 *   target: { id: number, title: string, rating: number|null }
 * }} FeedItem
 */

/** 팔로우한 사람의 활동 최신순. @returns {Promise<{ items: FeedItem[], page: number, size: number, total: number }>} */
export function fetchFeed(page = 1) {
  return http.get('/feed', { params: { page, size: FEED_PAGE_SIZE } }).then((res) => res.data)
}
