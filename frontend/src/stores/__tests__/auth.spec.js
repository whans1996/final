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

  it('F-03 AC10 updateUser는 로그인 상태를 유지한 채 닉네임·수준만 바꾼다', () => {
    const auth = useAuthStore()
    auth.setSession(SESSION)

    auth.updateUser({ nickname: '주니어개발자', level: 'SENIOR' })

    expect(auth.isLoggedIn).toBe(true)
    expect(auth.accessToken).toBe('access-1')
    expect(auth.user).toEqual({ id: 1, nickname: '주니어개발자', level: 'SENIOR', role: 'USER' })
    expect(auth.level).toBe('SENIOR')
  })

  it('F-03 AC10 비로그인이면 updateUser는 아무것도 하지 않는다', () => {
    const auth = useAuthStore()
    auth.updateUser({ nickname: '주니어개발자' })
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
  it('AC8 재발급 중에 로그아웃하면 재발급이 끝난 뒤 서버 로그아웃을 불러 새 토큰까지 폐기하고, 세션을 되살리지 않는다', async () => {
    vi.mocked(authApi.refresh).mockReset()
    vi.mocked(authApi.logout).mockReset()
    let resolveRefresh
    authApi.refresh.mockReturnValue(new Promise((resolve) => (resolveRefresh = resolve)))
    authApi.logout.mockResolvedValue()
    const auth = useAuthStore()
    auth.setSession(SESSION)

    const pendingRefresh = auth.refresh().catch((error) => error)
    const loggingOut = auth.logout()
    await Promise.resolve()
    expect(authApi.logout).not.toHaveBeenCalled()

    resolveRefresh({ ...SESSION, accessToken: 'access-2' })
    await loggingOut

    expect(authApi.logout).toHaveBeenCalledTimes(1)
    expect(auth.isLoggedIn).toBe(false)
    expect(await pendingRefresh).toBeInstanceOf(Error)
  })

  it('F-03 AC10 clear 뒤에 도착한 재발급 응답은 세션을 되살리지 않는다 (탈퇴, Codex 리뷰)', async () => {
    vi.mocked(authApi.refresh).mockReset()
    let resolveRefresh
    authApi.refresh.mockReturnValue(new Promise((resolve) => (resolveRefresh = resolve)))
    const auth = useAuthStore()
    auth.setSession(SESSION)

    // 탈퇴 직전에 시작된 재발급이 탈퇴 성공(clear) 뒤에 응답한다
    const pendingRefresh = auth.refresh().catch((error) => error)
    auth.clear()
    resolveRefresh({ ...SESSION, accessToken: 'access-2' })

    expect(await pendingRefresh).toBeInstanceOf(Error)
    expect(auth.isLoggedIn).toBe(false)
    expect(auth.user).toBeNull()
  })

  it('F-03 AC10 clear 뒤에 새로 시작한 재발급은 정상으로 세션을 받는다', async () => {
    vi.mocked(authApi.refresh).mockReset()
    authApi.refresh.mockResolvedValue(SESSION)
    const auth = useAuthStore()
    auth.clear()

    await expect(auth.refresh()).resolves.toBe('access-1')
    expect(auth.isLoggedIn).toBe(true)
  })

  it('AC8 로그아웃 중에는 새 재발급을 시작하지 않는다', async () => {
    vi.mocked(authApi.refresh).mockReset()
    vi.mocked(authApi.logout).mockReset()
    let resolveLogout
    authApi.logout.mockReturnValue(new Promise((resolve) => (resolveLogout = resolve)))
    const auth = useAuthStore()
    auth.setSession(SESSION)

    const loggingOut = auth.logout()
    await expect(auth.refresh()).rejects.toThrow('로그아웃 중입니다.')
    resolveLogout()
    await loggingOut

    expect(authApi.refresh).not.toHaveBeenCalled()
    expect(auth.isLoggedIn).toBe(false)
  })
})
