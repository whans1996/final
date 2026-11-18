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

  /** 프로필 수정 후 헤더 등에 바로 반영한다 (F-03). 비로그인이면 아무것도 하지 않는다. */
  function updateUser(partial) {
    if (!user.value) return
    user.value = { ...user.value, ...partial }
  }

  async function login({ email, password }) {
    setSession(await authApi.login({ email, password }))
    return user.value
  }

  let refreshing = null
  let loggingOut = false

  /**
   * 서버 세션(Refresh 토큰)을 폐기한다. 서버 호출이 실패해도 이 기기에서는 로그아웃한다.
   * 진행 중인 재발급이 있으면 그 응답(새 쿠키)을 받은 뒤에 로그아웃해야 새 토큰까지 폐기된다.
   */
  async function logout() {
    loggingOut = true
    try {
      if (refreshing) await refreshing.catch(() => {})
      await authApi.logout()
    } catch {
      // 쿠키가 남아도 서버에서 14일 뒤 만료된다
    } finally {
      clear()
      loggingOut = false
    }
  }

  /**
   * 새 Access 토큰을 받아 돌려준다. 동시에 여러 번 불러도 서버 호출은 한 번이다 (F-02 AC9).
   * 실패하면 상태를 비우고 에러를 던진다. 로그아웃 중이면 시작하지 않고, 도착한 결과도 버린다.
   */
  function refresh() {
    if (loggingOut && !refreshing) return Promise.reject(new Error('로그아웃 중입니다.'))
    if (!refreshing) {
      refreshing = authApi
        .refresh()
        .then((session) => {
          if (loggingOut) throw new Error('로그아웃 중입니다.')
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
    updateUser,
    login,
    logout,
    refresh,
    restore,
  }
})
