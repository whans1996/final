import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import * as authApi from '@/api/auth'

vi.mock('@/api/auth')

const SESSION = {
  accessToken: 'access-1',
  expiresIn: 1800,
  user: { id: 1, nickname: '코딩초보', level: 'JUNIOR', role: 'USER' },
}

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

describe('useAuthStore 로그인 흐름 (F-02 AC8)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.resetAllMocks()
  })

  it('AC8 login하면 Access 토큰과 회원 정보를 메모리에 둔다', async () => {
    authApi.login.mockResolvedValue(SESSION)
    const auth = useAuthStore()

    await auth.login({ email: 'user@example.com', password: 'vibe1234' })

    expect(authApi.login).toHaveBeenCalledWith({ email: 'user@example.com', password: 'vibe1234' })
    expect(auth.isLoggedIn).toBe(true)
    expect(auth.accessToken).toBe('access-1')
    expect(auth.user.nickname).toBe('코딩초보')
    expect(auth.level).toBe('JUNIOR')
  })

  it('AC8 login이 실패하면 비로그인 상태로 남고 에러를 그대로 던진다', async () => {
    const error = Object.assign(new Error('401'), { response: { data: { code: 'AUTH_001' } } })
    authApi.login.mockRejectedValue(error)
    const auth = useAuthStore()

    await expect(auth.login({ email: 'a@b.c', password: 'x' })).rejects.toBe(error)
    expect(auth.isLoggedIn).toBe(false)
  })

  it('AC8 logout하면 서버 세션을 폐기하고 상태를 비운다 (서버 호출이 실패해도 비운다)', async () => {
    const auth = useAuthStore()
    auth.setSession(SESSION)
    authApi.logout.mockRejectedValue(new Error('network'))

    await auth.logout()

    expect(authApi.logout).toHaveBeenCalledTimes(1)
    expect(auth.isLoggedIn).toBe(false)
  })

  it('AC8 restore는 refresh로 세션을 복원한다', async () => {
    authApi.refresh.mockResolvedValue(SESSION)
    const auth = useAuthStore()

    await auth.restore()

    expect(auth.isLoggedIn).toBe(true)
    expect(auth.user.id).toBe(1)
  })

  it('AC8 restore는 refresh가 실패해도 에러 없이 비로그인으로 시작한다', async () => {
    authApi.refresh.mockRejectedValue(new Error('AUTH_002'))
    const auth = useAuthStore()

    await expect(auth.restore()).resolves.toBeUndefined()
    expect(auth.isLoggedIn).toBe(false)
  })

  it('AC9 refresh를 동시에 여러 번 불러도 서버 호출은 한 번이고 같은 새 토큰을 돌려준다', async () => {
    authApi.refresh.mockResolvedValue(SESSION)
    const auth = useAuthStore()

    const tokens = await Promise.all([auth.refresh(), auth.refresh()])

    expect(authApi.refresh).toHaveBeenCalledTimes(1)
    expect(tokens).toEqual(['access-1', 'access-1'])
  })
})
