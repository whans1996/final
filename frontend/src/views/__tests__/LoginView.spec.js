import { beforeEach, describe, expect, it, vi } from 'vitest'
import LoginView from '@/views/LoginView.vue'
import * as authApi from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { apiError, fill, mountAt, submit } from './viewTestUtils'

vi.mock('@/api/auth')

const SESSION = {
  accessToken: 'access-1',
  expiresIn: 1800,
  user: { id: 1, nickname: '코딩초보', level: 'BEGINNER', role: 'USER' },
}

describe('LoginView (F-02 AC8)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('AC8 로그인하면 store에 세션이 들어가고 redirect 주소로 이동한다', async () => {
    authApi.login.mockResolvedValue(SESSION)
    const { wrapper, router } = await mountAt(LoginView, '/login?redirect=/review-plans')

    await fill(wrapper, { email: 'user@example.com', password: 'vibe1234' })
    await submit(wrapper)

    expect(authApi.login).toHaveBeenCalledWith({ email: 'user@example.com', password: 'vibe1234' })
    expect(useAuthStore().isLoggedIn).toBe(true)
    await vi.waitFor(() => expect(router.currentRoute.value.fullPath).toBe('/review-plans'))
  })

  it('AC8 redirect가 없으면 홈으로 이동한다', async () => {
    authApi.login.mockResolvedValue(SESSION)
    const { wrapper, router } = await mountAt(LoginView, '/login')

    await fill(wrapper, { email: 'user@example.com', password: 'vibe1234' })
    await submit(wrapper)

    await vi.waitFor(() => expect(router.currentRoute.value.path).toBe('/'))
  })

  it('AC8 AUTH_001이면 폼 위에 메시지를 보여 주고 입력을 유지한다', async () => {
    authApi.login.mockRejectedValue(
      apiError(401, { code: 'AUTH_001', message: '이메일 또는 비밀번호가 올바르지 않습니다.' }),
    )
    const { wrapper, router } = await mountAt(LoginView, '/login')

    await fill(wrapper, { email: 'user@example.com', password: 'wrong1234' })
    await submit(wrapper)

    expect(wrapper.find('[role="alert"]').text()).toBe('이메일 또는 비밀번호가 올바르지 않습니다.')
    expect(wrapper.find('input[name="email"]').element.value).toBe('user@example.com')
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('AC8 비어 있으면 요청하지 않고 필드 아래에 안내한다', async () => {
    const { wrapper } = await mountAt(LoginView, '/login')

    await submit(wrapper)

    expect(authApi.login).not.toHaveBeenCalled()
    expect(wrapper.find('[data-error="email"]').text()).toBe('이메일을 입력하세요.')
    expect(wrapper.find('[data-error="password"]').text()).toBe('비밀번호를 입력하세요.')
  })

  it('AC8 가입 직후(joined)로 오면 가입 완료 안내를 보여 준다', async () => {
    const { wrapper } = await mountAt(LoginView, '/login?joined=1')
    expect(wrapper.find('[role="status"]').text()).toBe('가입되었습니다. 로그인해 주세요.')
  })

  it('AC8 회원가입 화면 링크가 있다', async () => {
    const { wrapper } = await mountAt(LoginView, '/login')
    expect(wrapper.find('a[href="/signup"]').exists()).toBe(true)
  })
})
