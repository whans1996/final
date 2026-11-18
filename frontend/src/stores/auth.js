import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'

/**
 * 로그인 상태, Access 토큰, 사용자 수준(level)을 관리한다 (F-02).
 *
 * Access 토큰은 메모리에만 둔다(localStorage 금지). Refresh 토큰은 HttpOnly 쿠키라 JS가 볼 수 없고,
 * 새로고침하면 restore()가 refresh로 세션을 되살린다.
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

  async function login({ email, password }) {
    setSession(await authApi.login({ email, password }))
    return user.value
  }

  /** 서버 세션(Refresh 토큰)을 폐기한다. 서버 호출이 실패해도 이 기기에서는 로그아웃한다. */
  async function logout() {
    try {
      await authApi.logout()
    } catch {
      // 쿠키가 남아도 서버에서 14일 뒤 만료된다
    } finally {
      clear()
    }
  }

  let refreshing = null

  /**
   * 새 Access 토큰을 받아 돌려준다. 동시에 여러 번 불러도 서버 호출은 한 번이다 (F-02 AC9).
   * 실패하면 상태를 비우고 에러를 던진다.
   */
  function refresh() {
    if (!refreshing) {
      refreshing = authApi
        .refresh()
        .then((session) => {
          setSession(session)
          return session.accessToken
        })
        .catch((error) => {
          clear()
          throw error
        })
        .finally(() => {
          refreshing = null
        })
    }
    return refreshing
  }

  /** 앱 시작 시 1회. 쿠키가 없거나 만료되었으면 조용히 비로그인으로 시작한다. */
  async function restore() {
    try {
      await refresh()
    } catch {
      // 비로그인 상태로 시작
    }
  }

  return {
    accessToken,
    user,
    isLoggedIn,
    isAdmin,
    level,
    setSession,
    clear,
    login,
    logout,
    refresh,
    restore,
  }
})
