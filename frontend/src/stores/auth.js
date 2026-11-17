import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * 로그인 상태, Access 토큰, 사용자 수준(level)을 관리한다.
 *
 * 토큰을 어디에 보관할지(메모리/localStorage)와 Refresh 흐름은 F-02 spec에서 정한다.
 * 지금은 메모리에만 두므로 새로고침하면 로그아웃된다.
 */
export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(null)
  /** @type {import('vue').Ref<{ id: number, nickname: string, level: string, role: string } | null>} */
  const user = ref(null)

  const isLoggedIn = computed(() => !!accessToken.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const level = computed(() => user.value?.level ?? 'BEGINNER')

  function setSession(session) {
    accessToken.value = session.accessToken
    user.value = session.user ?? null
  }

  function clear() {
    accessToken.value = null
    user.value = null
  }

  return { accessToken, user, isLoggedIn, isAdmin, level, setSession, clear }
})
