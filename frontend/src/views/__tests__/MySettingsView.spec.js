import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import MySettingsView from '@/views/MySettingsView.vue'
import * as usersApi from '@/api/users'
import { useAuthStore } from '@/stores/auth'
import { authGuard, routes } from '@/router'
import { apiError, mountAt } from './viewTestUtils'

vi.mock('@/api/users')

const ME = {
  id: 1,
  email: 'user@example.com',
  nickname: '코딩초보',
  bio: '스프링 공부 중',
  level: 'BEGINNER',
  role: 'USER',
  createdAt: '2026-11-18T10:30:00',
}

const SERVER_ERROR = '서버에서 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.'

function login() {
  useAuthStore().setSession({
    accessToken: 'access-1',
    user: { id: 1, nickname: '코딩초보', level: 'BEGINNER', role: 'USER' },
  })
}

async function mountSettings() {
  usersApi.fetchMe.mockResolvedValue(ME)
  const mounted = await mountAt(MySettingsView, '/me/settings', { beforeNavigate: login })
  await flushPromises()
  return mounted
}

async function saveProfile(wrapper) {
  await wrapper.find('form[data-form="profile"]').trigger('submit')
  await flushPromises()
}

describe('MySettingsView (F-03 AC10)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('AC10 로그인하지 않았으면 인증 가드가 로그인 화면으로 보낸다', () => {
    // 테스트 라우터에는 가드가 붙지 않으므로 라우트 meta와 authGuard를 직접 확인한다
    const route = routes.find((r) => r.path === '/me/settings')
    const to = { meta: route.meta, fullPath: '/me/settings' }

    expect(authGuard(to, { isLoggedIn: false })).toEqual({
      name: 'login',
      query: { redirect: '/me/settings' },
    })
    expect(authGuard(to, { isLoggedIn: true })).toBe(true)
  })

  it('AC10 내 정보로 폼을 채운다', async () => {
    const { wrapper } = await mountSettings()

    expect(wrapper.find('input[name="email"]').element.value).toBe('user@example.com')
    expect(wrapper.find('input[name="email"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('input[name="nickname"]').element.value).toBe('코딩초보')
    expect(wrapper.find('textarea[name="bio"]').element.value).toBe('스프링 공부 중')
    expect(wrapper.find('input[name="level"][value="BEGINNER"]').element.checked).toBe(true)
  })

  it('AC10 저장하면 수정 API를 부르고 성공 문구를 보여 주며 store의 닉네임·수준을 갱신한다', async () => {
    const { wrapper } = await mountSettings()
    usersApi.updateMe.mockResolvedValue({
      ...ME,
      nickname: '주니어개발자',
      bio: null,
      level: 'JUNIOR',
    })

    await wrapper.find('input[name="nickname"]').setValue('주니어개발자')
    await wrapper.find('textarea[name="bio"]').setValue('')
    await wrapper.find('input[name="level"][value="JUNIOR"]').setValue(true)
    await saveProfile(wrapper)

    expect(usersApi.updateMe).toHaveBeenCalledWith({
      nickname: '주니어개발자',
      bio: '',
      level: 'JUNIOR',
    })
    expect(wrapper.find('[data-message="profile-saved"]').text()).toBe('저장했습니다.')
    expect(useAuthStore().user).toMatchObject({ nickname: '주니어개발자', level: 'JUNIOR' })
  })

  it('AC10 클라이언트 검증에 실패하면 요청하지 않고 필드 아래에 사유를 보여 준다', async () => {
    const { wrapper } = await mountSettings()

    await wrapper.find('input[name="nickname"]').setValue('!')
    await wrapper.find('textarea[name="bio"]').setValue('가'.repeat(301))
    await saveProfile(wrapper)

    expect(usersApi.updateMe).not.toHaveBeenCalled()
    expect(wrapper.find('[data-error="nickname"]').text()).toBe(
      '닉네임은 2~20자의 한글·영문·숫자·_만 사용할 수 있습니다.',
    )
    expect(wrapper.find('[data-error="bio"]').text()).toBe('소개는 300자 이하로 입력하세요.')
  })

  it('AC10 서버의 USER_409 details를 닉네임 아래에 보여 주고 store는 그대로 둔다', async () => {
    const { wrapper } = await mountSettings()
    usersApi.updateMe.mockRejectedValue(
      apiError(409, {
        code: 'USER_409',
        message: '이미 사용 중인 정보가 있습니다.',
        details: { nickname: '이미 사용 중인 닉네임입니다.' },
      }),
    )

    await wrapper.find('input[name="nickname"]').setValue('Coder')
    await saveProfile(wrapper)

    expect(wrapper.find('[data-error="nickname"]').text()).toBe('이미 사용 중인 닉네임입니다.')
    expect(useAuthStore().user.nickname).toBe('코딩초보')
  })

  it('AC10 탈퇴 버튼은 비밀번호와 확인 체크가 모두 있어야 활성화된다', async () => {
    const { wrapper } = await mountSettings()
    const button = () => wrapper.find('button[data-action="withdraw"]')

    expect(button().attributes('disabled')).toBeDefined()
    await wrapper.find('input[name="withdraw-password"]').setValue('vibe1234')
    expect(button().attributes('disabled')).toBeDefined()
    await wrapper.find('input[name="withdraw-confirm"]').setValue(true)
    expect(button().attributes('disabled')).toBeUndefined()
  })

  it('AC10 탈퇴에 성공하면 logout 호출 없이 세션만 비우고 홈으로 이동한다', async () => {
    const { wrapper, router } = await mountSettings()
    usersApi.withdraw.mockResolvedValue()
    // 서버가 이미 쿠키·토큰을 폐기했으므로 /auth/logout을 다시 부르지 않는다 (spec 5절)
    const logout = vi.spyOn(useAuthStore(), 'logout')

    await wrapper.find('input[name="withdraw-password"]').setValue('vibe1234')
    await wrapper.find('input[name="withdraw-confirm"]').setValue(true)
    await wrapper.find('form[data-form="withdraw"]').trigger('submit')
    await flushPromises()

    expect(usersApi.withdraw).toHaveBeenCalledWith('vibe1234')
    expect(logout).not.toHaveBeenCalled()
    expect(useAuthStore().isLoggedIn).toBe(false)
    await vi.waitFor(() => expect(router.currentRoute.value.path).toBe('/'))
  })

  it('AC10 내 정보를 불러오지 못하면 서버 메시지를 보여 주고 폼을 띄우지 않는다', async () => {
    usersApi.fetchMe.mockRejectedValue(apiError(500, { code: 'COMMON_500', message: SERVER_ERROR }))
    const { wrapper } = await mountAt(MySettingsView, '/me/settings', { beforeNavigate: login })
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toBe(SERVER_ERROR)
    expect(wrapper.find('form').exists()).toBe(false)
  })

  it('AC10 저장·탈퇴에서 details가 없는 에러는 각 폼 위에 서버 메시지로 보여 준다', async () => {
    const { wrapper } = await mountSettings()
    usersApi.updateMe.mockRejectedValue(
      apiError(500, { code: 'COMMON_500', message: SERVER_ERROR }),
    )
    usersApi.withdraw.mockRejectedValue(
      apiError(500, { code: 'COMMON_500', message: SERVER_ERROR }),
    )

    await saveProfile(wrapper)
    await wrapper.find('input[name="withdraw-password"]').setValue('vibe1234')
    await wrapper.find('input[name="withdraw-confirm"]').setValue(true)
    await wrapper.find('form[data-form="withdraw"]').trigger('submit')
    await flushPromises()

    expect(wrapper.find('form[data-form="profile"] [role="alert"]').text()).toBe(SERVER_ERROR)
    expect(wrapper.find('form[data-form="withdraw"] [role="alert"]').text()).toBe(SERVER_ERROR)
    expect(useAuthStore().isLoggedIn).toBe(true)
  })

  it('AC10 비밀번호가 틀리면(USER_001) 비밀번호 아래에 보여 주고 로그인을 유지한다', async () => {
    const { wrapper, router } = await mountSettings()
    usersApi.withdraw.mockRejectedValue(
      apiError(400, {
        code: 'USER_001',
        message: '비밀번호가 올바르지 않습니다.',
        details: { password: '비밀번호가 올바르지 않습니다.' },
      }),
    )

    await wrapper.find('input[name="withdraw-password"]').setValue('wrong1234')
    await wrapper.find('input[name="withdraw-confirm"]').setValue(true)
    await wrapper.find('form[data-form="withdraw"]').trigger('submit')
    await flushPromises()

    expect(wrapper.find('[data-error="withdraw-password"]').text()).toBe(
      '비밀번호가 올바르지 않습니다.',
    )
    expect(useAuthStore().isLoggedIn).toBe(true)
    expect(router.currentRoute.value.path).toBe('/me/settings')
  })
})
