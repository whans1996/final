import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import App from '@/App.vue'
import UserProfileView from '@/views/UserProfileView.vue'
import * as usersApi from '@/api/users'
import * as conceptsApi from '@/api/concepts'
import * as followsApi from '@/api/follows'
import * as challengesApi from '@/api/challenges'
import { useAuthStore } from '@/stores/auth'
import { apiError, mountAt } from './viewTestUtils'

vi.mock('@/api/users')
vi.mock('@/api/concepts')
vi.mock('@/api/follows')
vi.mock('@/api/challenges')

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
    // F-04 팔로우 영역 기본값
    followsApi.fetchFollowStatus.mockResolvedValue({
      following: false,
      followerCount: 0,
      followingCount: 0,
    })
    // F-12 찜 절 기본값 (0건)
    conceptsApi.fetchUserWishes.mockResolvedValue({ items: [], page: 1, size: 12, total: 0 })
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

  it('AC9 A→B→A로 빠르게 이동하면 마지막 요청의 응답만 보여 준다', async () => {
    const pending = []
    usersApi.fetchUser.mockImplementation(() => new Promise((resolve) => pending.push(resolve)))
    usersApi.fetchUserStats.mockResolvedValue({
      analysisCount: 0,
      learnedConceptCount: 0,
      quizAccuracy: null,
    })
    const { wrapper, router } = await mountAt(UserProfileView, '/users/3')
    await router.push('/users/4')
    await router.push('/users/3')
    await flushPromises()
    expect(pending).toHaveLength(3)

    // 첫 요청(옛 3번)이 마지막 요청보다 늦게 끝나도 덮어쓰지 않는다
    pending[2]({ ...PROFILE, nickname: '최신' })
    await flushPromises()
    expect(wrapper.find('h1').text()).toBe('최신')
    pending[0]({ ...PROFILE, nickname: '옛날' })
    pending[1]({ ...PROFILE, id: 4, nickname: '사번' })
    await flushPromises()
    expect(wrapper.find('h1').text()).toBe('최신')
  })

  it('AC9 프로필이 아닌 주소로 나가면 id 없이 요청하지 않는다', async () => {
    usersApi.fetchUser.mockResolvedValue(PROFILE)
    usersApi.fetchUserStats.mockResolvedValue({
      analysisCount: 0,
      learnedConceptCount: 0,
      quizAccuracy: null,
    })
    const { router } = await mountAt(UserProfileView, '/users/3')
    await flushPromises()

    await router.push('/')
    await flushPromises()

    expect(usersApi.fetchUser).toHaveBeenCalledTimes(1)
    expect(usersApi.fetchUserStats).toHaveBeenCalledTimes(1)
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

describe('UserProfileView 찜한 카드 절 (F-12 AC15)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    // F-04 팔로우 영역 기본값
    followsApi.fetchFollowStatus.mockResolvedValue({
      following: false,
      followerCount: 0,
      followingCount: 0,
    })
    conceptsApi.fetchUserWishes.mockResolvedValue({ items: [], page: 1, size: 12, total: 4 })
    usersApi.fetchUserStats.mockResolvedValue({
      analysisCount: 0,
      learnedConceptCount: 0,
      quizAccuracy: null,
    })
  })

  it('AC15 학습 통계 아래에 찜한 카드 절이 보인다', async () => {
    usersApi.fetchUser.mockResolvedValue(PROFILE)

    const { wrapper } = await mountAt(UserProfileView, '/users/3')
    await flushPromises()

    expect(conceptsApi.fetchUserWishes).toHaveBeenCalledWith(3, { page: 1, size: 12 })
    expect(wrapper.find('[data-section="wishes"] h2').text()).toBe('찜한 카드 4')
  })

  it('AC15 프로필이 404면 찜한 카드 절을 그리지 않는다', async () => {
    usersApi.fetchUser.mockRejectedValue(
      apiError(404, { code: 'USER_404', message: '사용자를 찾을 수 없습니다.' }),
    )

    const { wrapper } = await mountAt(UserProfileView, '/users/3')
    await flushPromises()

    expect(wrapper.find('[data-section="wishes"]').exists()).toBe(false)
    expect(conceptsApi.fetchUserWishes).not.toHaveBeenCalled()
  })
})

describe('UserProfileView 참여 챌린지 절 (F-25 AC15)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    followsApi.fetchFollowStatus.mockResolvedValue({
      following: false,
      followerCount: 0,
      followingCount: 0,
    })
    conceptsApi.fetchUserWishes.mockResolvedValue({ items: [], page: 1, size: 12, total: 0 })
    usersApi.fetchUser.mockResolvedValue(PROFILE)
    usersApi.fetchUserStats.mockResolvedValue({
      analysisCount: 0,
      learnedConceptCount: 0,
      quizAccuracy: null,
    })
  })

  it('AC15 프로필에 참여 챌린지 절이 보인다', async () => {
    challengesApi.fetchUserChallenges.mockResolvedValue({
      items: [],
      page: 1,
      size: 6,
      total: 3,
    })

    const { wrapper } = await mountAt(UserProfileView, '/users/3')
    await flushPromises()

    expect(challengesApi.fetchUserChallenges).toHaveBeenCalledWith(3, { page: 1, size: 6 })
    expect(wrapper.find('[data-section="challenges"] h2').text()).toBe('참여 챌린지 3')
  })

  it('AC15 이 절만 실패하면 절 안에 메시지를 보이고 프로필은 그대로다', async () => {
    challengesApi.fetchUserChallenges.mockRejectedValue(
      apiError(500, { code: 'COMMON_500', message: '서버 오류가 발생했습니다.' }),
    )

    const { wrapper } = await mountAt(UserProfileView, '/users/3')
    await flushPromises()

    expect(wrapper.find('h1').text()).toBe('코딩초보')
    expect(wrapper.find('[data-section="challenges"] [role="alert"]').text()).toBe(
      '서버 오류가 발생했습니다.',
    )
  })
})

describe('UserProfileView 팔로우 영역 (F-04 AC9)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    conceptsApi.fetchUserWishes.mockResolvedValue({ items: [], page: 1, size: 12, total: 0 })
    usersApi.fetchUser.mockResolvedValue(PROFILE)
    usersApi.fetchUserStats.mockResolvedValue({
      analysisCount: 0,
      learnedConceptCount: 0,
      quizAccuracy: null,
    })
    followsApi.fetchFollowStatus.mockResolvedValue({
      following: false,
      followerCount: 12,
      followingCount: 3,
    })
  })

  it('AC9 남의 프로필이면 닉네임 아래에 수와 팔로우 버튼이 보인다', async () => {
    const { wrapper } = await mountAt(UserProfileView, '/users/3', {
      beforeNavigate: () => login(9),
    })
    await flushPromises()

    expect(followsApi.fetchFollowStatus).toHaveBeenCalledWith(3)
    const follow = wrapper.find('.profile-head [data-section="follow"]')
    expect(follow.find('[data-link="followers"]').text()).toBe('팔로워 12')
    expect(follow.find('[data-link="followings"]').text()).toBe('팔로잉 3')
    expect(follow.find('[data-action="follow"]').text()).toBe('팔로우')
  })

  it('AC9 내 프로필이면 수만 보이고 팔로우 버튼은 없다', async () => {
    const { wrapper } = await mountAt(UserProfileView, '/users/3', {
      beforeNavigate: () => login(3),
    })
    await flushPromises()

    expect(wrapper.find('[data-link="followers"]').exists()).toBe(true)
    expect(wrapper.find('[data-action="follow"]').exists()).toBe(false)
  })

  it('AC9 팔로우 영역만 실패하면 프로필은 그대로 보인다', async () => {
    followsApi.fetchFollowStatus.mockRejectedValue(
      apiError(500, { code: 'COMMON_500', message: '서버에서 오류가 발생했습니다.' }),
    )

    const { wrapper } = await mountAt(UserProfileView, '/users/3')
    await flushPromises()

    expect(wrapper.find('h1').text()).toBe('코딩초보')
    expect(wrapper.find('[data-section="follow"] [role="alert"]').text()).toBe(
      '서버에서 오류가 발생했습니다.',
    )
  })
})
