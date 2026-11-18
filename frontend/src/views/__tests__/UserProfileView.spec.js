import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import App from '@/App.vue'
import UserProfileView from '@/views/UserProfileView.vue'
import * as usersApi from '@/api/users'
import { useAuthStore } from '@/stores/auth'
import { apiError, mountAt } from './viewTestUtils'

vi.mock('@/api/users')

const PROFILE = {
  id: 3,
  nickname: '코딩초보',
  bio: '스프링 공부 중',
  level: 'JUNIOR',
  createdAt: '2026-11-18T10:30:00',
}

function login(id) {
  useAuthStore().setSession({
    accessToken: 'access-1',
    user: { id, nickname: '코딩초보', level: 'JUNIOR', role: 'USER' },
  })
}

describe('UserProfileView (F-03 AC9)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('AC9 프로필 정보와 학습 통계 3개를 보여 준다', async () => {
    usersApi.fetchUser.mockResolvedValue(PROFILE)
    usersApi.fetchUserStats.mockResolvedValue({
      analysisCount: 3,
      learnedConceptCount: 12,
      quizAccuracy: 75,
    })

    const { wrapper } = await mountAt(UserProfileView, '/users/3')
    await flushPromises()

    expect(usersApi.fetchUser).toHaveBeenCalledWith(3)
    expect(usersApi.fetchUserStats).toHaveBeenCalledWith(3)
    expect(wrapper.find('h1').text()).toBe('코딩초보')
    expect(wrapper.text()).toContain('주니어')
    expect(wrapper.text()).toContain('스프링 공부 중')
    expect(wrapper.text()).toContain('2026.11.18')
    expect(wrapper.find('[data-stat="analysisCount"]').text()).toContain('3')
    expect(wrapper.find('[data-stat="learnedConceptCount"]').text()).toContain('12')
    expect(wrapper.find('[data-stat="quizAccuracy"]').text()).toContain('75%')
  })

  it('AC9 퀴즈 기록이 없으면 정답률을 "-"로 보여 준다', async () => {
    usersApi.fetchUser.mockResolvedValue({ ...PROFILE, bio: null })
    usersApi.fetchUserStats.mockResolvedValue({
      analysisCount: 0,
      learnedConceptCount: 0,
      quizAccuracy: null,
    })

    const { wrapper } = await mountAt(UserProfileView, '/users/3')
    await flushPromises()

    expect(wrapper.find('[data-stat="quizAccuracy"]').text()).toContain('-')
    expect(wrapper.find('[data-stat="quizAccuracy"]').text()).not.toContain('%')
  })

  it('AC9 내 프로필이면 정보 수정 링크가 보이고, 다른 사람이면 보이지 않는다', async () => {
    usersApi.fetchUser.mockResolvedValue(PROFILE)
    usersApi.fetchUserStats.mockResolvedValue({
      analysisCount: 0,
      learnedConceptCount: 0,
      quizAccuracy: null,
    })

    const mine = await mountAt(UserProfileView, '/users/3')
    login(3)
    await flushPromises()
    expect(mine.wrapper.find('a[href="/me/settings"]').exists()).toBe(true)

    const others = await mountAt(UserProfileView, '/users/3')
    login(4)
    await flushPromises()
    expect(others.wrapper.find('a[href="/me/settings"]').exists()).toBe(false)
  })

  it('AC9 404면 탈퇴했거나 없는 사용자라고 알려 준다', async () => {
    const notFound = apiError(404, {
      code: 'USER_404',
      message: '존재하지 않거나 탈퇴한 사용자입니다.',
    })
    usersApi.fetchUser.mockRejectedValue(notFound)
    usersApi.fetchUserStats.mockRejectedValue(notFound)

    const { wrapper } = await mountAt(UserProfileView, '/users/99')
    await flushPromises()

    expect(wrapper.text()).toContain('탈퇴했거나 없는 사용자입니다.')
    expect(wrapper.find('[data-stat="analysisCount"]').exists()).toBe(false)
  })

  it('AC9 그 밖의 에러는 서버 메시지를 보여 준다', async () => {
    const serverError = apiError(500, {
      code: 'COMMON_500',
      message: '서버에서 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.',
    })
    usersApi.fetchUser.mockRejectedValue(serverError)
    usersApi.fetchUserStats.mockRejectedValue(serverError)

    const { wrapper } = await mountAt(UserProfileView, '/users/3')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toBe(
      '서버에서 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.',
    )
  })

  it('AC9 로그인하면 헤더 닉네임이 내 프로필 링크가 된다', async () => {
    const { wrapper } = await mountAt(App, '/')
    login(3)
    await flushPromises()

    const link = wrapper.find('header a.nickname')
    expect(link.attributes('href')).toBe('/users/3')
    expect(link.text()).toBe('코딩초보')
  })
})
