import http from '@/api/http'

// 헤더 알림 (NT-1, docs/specs/NT-1-header-notifications.md 3절). 모두 로그인 필요

export const NOTIFICATION_PAGE_SIZE = 20

/**
 * @typedef {{ id: number, nickname: string }} NotificationActor
 * @typedef {{ id: number, type: string, message: string, link: string,
 *   actor: NotificationActor | null, read: boolean, createdAt: string }} Notification
 */

/** 최신순. @returns {Promise<{ items: Notification[], page: number, size: number, total: number }>} */
export function fetchNotifications({ page = 1, size = NOTIFICATION_PAGE_SIZE } = {}) {
  return http.get('/notifications', { params: { page, size } }).then((res) => res.data)
}

/** @returns {Promise<{ count: number }>} */
export function fetchUnreadCount() {
  return http.get('/notifications/unread-count').then((res) => res.data)
}

/** 멱등 (이미 읽었어도 204). 없거나 남의 알림이면 404 NOTIFICATION_404 */
export function markNotificationRead(id) {
  return http.patch(`/notifications/${id}/read`).then(() => undefined)
}

export function markAllNotificationsRead() {
  return http.patch('/notifications/read-all').then(() => undefined)
}
