import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('처음에는 비로그인이고 수준은 BEGINNER', () => {
    const auth = useAuthStore()
    expect(auth.isLoggedIn).toBe(false)
    expect(auth.isAdmin).toBe(false)
    expect(auth.level).toBe('BEGINNER')
  })

  it('세션을 저장하면 로그인 상태와 사용자 수준·역할을 알려 준다', () => {
    const auth = useAuthStore()
    auth.setSession({
      accessToken: 'token',
      user: { id: 1, nickname: 'vibecoder', level: 'JUNIOR', role: 'ADMIN' },
    })
    expect(auth.isLoggedIn).toBe(true)
    expect(auth.level).toBe('JUNIOR')
    expect(auth.isAdmin).toBe(true)
  })

  it('clear하면 비로그인 상태로 돌아간다', () => {
    const auth = useAuthStore()
    auth.setSession({ accessToken: 'token', user: { id: 1, role: 'USER', level: 'SENIOR' } })
    auth.clear()
    expect(auth.isLoggedIn).toBe(false)
    expect(auth.user).toBeNull()
  })
})
