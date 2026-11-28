import { ref, watch } from 'vue'
import { defineStore } from 'pinia'
import * as notificationsApi from '@/api/notifications'
import { useAuthStore } from '@/stores/auth'

/**
 * 헤더 종 배지의 안 읽은 알림 수 (NT-1 AC13·AC14·AC16). 목록은 드롭다운이 열 때마다 새로 부르므로 여기 두지 않는다.
 * 로그아웃(auth 초기화)하면 0으로 돌린다.
 */
export const useNotificationsStore = defineStore('notifications', () => {
  const unreadCount = ref(0)
  const auth = useAuthStore()

  watch(
    () => auth.isLoggedIn,
    (loggedIn) => {
      if (!loggedIn) unreadCount.value = 0
    },
  )

  /** 실패하면 배지를 그대로 두고 에러를 던진다 (부르는 쪽이 다음 주기에 다시 한다). */
  async function refreshCount() {
    const { count } = await notificationsApi.fetchUnreadCount()
    if (auth.isLoggedIn) unreadCount.value = count
  }

  /** 배지를 바로 1 줄이고 요청을 보낸다. 실패해도 다음 조회가 바로잡는다. */
  function markRead(id) {
    unreadCount.value = Math.max(0, unreadCount.value - 1)
    return notificationsApi.markNotificationRead(id)
  }

  /** 성공하면 배지를 0으로. 실패하면 에러를 던진다. */
  async function markAllRead() {
    await notificationsApi.markAllNotificationsRead()
    unreadCount.value = 0
  }

  return { unreadCount, refreshCount, markRead, markAllRead }
})
