import { beforeEach, describe, expect, it, vi } from 'vitest'
import SignupView from '@/views/SignupView.vue'
import * as authApi from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { apiError, fill, mountAt, submit } from './viewTestUtils'

vi.mock('@/api/auth')

const VALID = { email: 'user@example.com', password: 'vibe1234', nickname: '코딩초보' }
const SESSION = {
  accessToken: 'access-1',
  expiresIn: 1800,
  user: { id: 1, nickname: '코딩초보', level: 'JUNIOR', role: 'USER' },
}

async function chooseLevel(wrapper, level) {
  await wrapper.find(`input[name="level"][value="${level}"]`).setValue(true)
}

describe('SignupView (F-01 AC7)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('AC7 수준 3가지를 고를 수 있다', async () => {
    const { wrapper } = await mountAt(SignupView, '/signup')
    const values = wrapper.findAll('input[name="level"]').map((r) => r.element.value)
    expect(values).toEqual(['BEGINNER', 'JUNIOR', 'SENIOR'])
  })

  it('AC7 클라이언트 검증에 실패하면 요청하지 않고 필드 아래에 사유를 보여 준다', async () => {
    const { wrapper } = await mountAt(SignupView, '/signup')

    await fill(wrapper, { email: 'not-an-email', password: 'short', nickname: '!' })
    await submit(wrapper)

    expect(authApi.signup).not.toHaveBeenCalled()
    expect(wrapper.find('[data-error="email"]').text()).toBe('올바른 이메일 형식이 아닙니다.')
    expect(wrapper.find('[data-error="password"]').text()).toBe(
      '비밀번호는 8~64자이며 영문과 숫자를 모두 포함해야 합니다.',
    )
    expect(wrapper.find('[data-error="nickname"]').text()).toBe(
      '닉네임은 2~20자의 한글·영문·숫자·_만 사용할 수 있습니다.',
    )
    expect(wrapper.find('[data-error="level"]').text()).toBe('학습 수준을 선택하세요.')
  })

  it('AC7 성공하면 같은 자격 증명으로 로그인하고 redirect 주소로 이동한다', async () => {
    authApi.signup.mockResolvedValue({ id: 1 })
    authApi.login.mockResolvedValue(SESSION)
    const { wrapper, router } = await mountAt(SignupView, '/signup?redirect=/analyses/new')

    await fill(wrapper, VALID)
    await chooseLevel(wrapper, 'JUNIOR')
    await submit(wrapper)

    expect(authApi.signup).toHaveBeenCalledWith({ ...VALID, level: 'JUNIOR' })
    expect(authApi.login).toHaveBeenCalledWith({ email: VALID.email, password: VALID.password })
    expect(useAuthStore().isLoggedIn).toBe(true)
    await vi.waitFor(() => expect(router.currentRoute.value.fullPath).toBe('/analyses/new'))
  })

  it('AC7 서버의 USER_409 details를 해당 필드 아래에 보여 준다', async () => {
    authApi.signup.mockRejectedValue(
      apiError(409, {
        code: 'USER_409',
        message: '이미 사용 중인 정보가 있습니다.',
        details: { email: '이미 사용 중인 이메일입니다.' },
      }),
    )
    const { wrapper, router } = await mountAt(SignupView, '/signup')

    await fill(wrapper, VALID)
    await chooseLevel(wrapper, 'BEGINNER')
    await submit(wrapper)

    expect(wrapper.find('[data-error="email"]').text()).toBe('이미 사용 중인 이메일입니다.')
    expect(authApi.login).not.toHaveBeenCalled()
    expect(router.currentRoute.value.path).toBe('/signup')
  })

  it('AC7 details가 없는 에러는 폼 위에 메시지로 보여 준다', async () => {
    authApi.signup.mockRejectedValue(new Error('Network Error'))
    const { wrapper } = await mountAt(SignupView, '/signup')

    await fill(wrapper, VALID)
    await chooseLevel(wrapper, 'SENIOR')
    await submit(wrapper)

    expect(wrapper.find('[role="alert"]').text()).toBe(
      '서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.',
    )
  })

  it('AC7 로그인 화면 링크가 있다', async () => {
    const { wrapper } = await mountAt(SignupView, '/signup')
    expect(wrapper.find('a[href="/login"]').exists()).toBe(true)
  })
})
