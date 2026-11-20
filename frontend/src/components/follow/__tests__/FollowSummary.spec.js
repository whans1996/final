import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { routes } from '@/router'
import FollowSummary from '@/components/follow/FollowSummary.vue'
import * as followsApi from '@/api/follows'
import { useAuthStore } from '@/stores/auth'
import { apiError } from '@/views/__tests__/viewTestUtils'

vi.mock('@/api/follows')

function login(id) {
  useAuthStore().setSession({
    accessToken: 'access-1',
    user: { id, nickname: '나', level: 'JUNIOR', role: 'USER' },
  })
}

async function mountSummary(props, { asUser } = {}) {
  const pinia = createPinia()
  setActivePinia(pinia)
  if (asUser) login(asUser)
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push(`/users/${props.userId}`)
  await router.isReady()
  const wrapper = mount(FollowSummary, { props, global: { plugins: [pinia, router] } })
  await flushPromises()
  return { wrapper, router }
}

const button = (wrapper) => wrapper.find('[data-action="follow"]')

describe('FollowSummary (F-04 AC9)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    followsApi.fetchFollowStatus.mockResolvedValue({
      following: false,
      followerCount: 12,
      followingCount: 3,
    })
  })

  it('AC9 팔로워·팔로잉 수를 팔로우 목록 화면 링크로 보여 준다', async () => {
    const { wrapper } = await mountSummary({ userId: 7, isMe: false })

    expect(followsApi.fetchFollowStatus).toHaveBeenCalledWith(7)
    const followers = wrapper.find('[data-link="followers"]')
    const followings = wrapper.find('[data-link="followings"]')
    expect(followers.text()).toBe('팔로워 12')
    expect(followers.attributes('href')).toBe('/users/7/follows?tab=followers')
    expect(followings.text()).toBe('팔로잉 3')
    expect(followings.attributes('href')).toBe('/users/7/follows?tab=followings')
  })

  it('AC9 내 프로필에는 팔로우 버튼이 없다', async () => {
    const { wrapper } = await mountSummary({ userId: 1, isMe: true }, { asUser: 1 })

    expect(wrapper.find('[data-link="followers"]').exists()).toBe(true)
    expect(button(wrapper).exists()).toBe(false)
  })

  it('AC9 팔로우를 누르면 요청 중 비활성 → 응답의 상태와 수로 바꾼다', async () => {
    let resolve
    followsApi.follow.mockReturnValue(new Promise((r) => (resolve = r)))
    const { wrapper } = await mountSummary({ userId: 7, isMe: false }, { asUser: 1 })

    expect(button(wrapper).text()).toBe('팔로우')
    await button(wrapper).trigger('click')
    expect(followsApi.follow).toHaveBeenCalledWith(7)
    expect(button(wrapper).attributes('disabled')).toBeDefined()

    resolve({ following: true, followerCount: 13, followingCount: 3 })
    await flushPromises()

    expect(button(wrapper).attributes('disabled')).toBeUndefined()
    expect(button(wrapper).text()).toBe('팔로잉')
    expect(wrapper.find('[data-link="followers"]').text()).toBe('팔로워 13')
  })

  it('AC9 마우스를 올린 채 팔로우하면 응답 뒤에는 "언팔로우"가 아니라 "팔로잉"이다', async () => {
    followsApi.follow.mockResolvedValue({ following: true, followerCount: 13, followingCount: 3 })
    const { wrapper } = await mountSummary({ userId: 7, isMe: false }, { asUser: 1 })

    await button(wrapper).trigger('mouseenter')
    await button(wrapper).trigger('click')
    await flushPromises()

    expect(button(wrapper).text()).toBe('팔로잉')
    await button(wrapper).trigger('mouseleave')
    await button(wrapper).trigger('mouseenter')
    expect(button(wrapper).text()).toBe('언팔로우')
  })

  it('AC9 팔로잉 버튼에 마우스를 올리면 "언팔로우"이고 누르면 언팔로우한다', async () => {
    followsApi.fetchFollowStatus.mockResolvedValue({
      following: true,
      followerCount: 13,
      followingCount: 3,
    })
    followsApi.unfollow.mockResolvedValue({
      following: false,
      followerCount: 12,
      followingCount: 3,
    })
    const { wrapper } = await mountSummary({ userId: 7, isMe: false }, { asUser: 1 })

    expect(button(wrapper).text()).toBe('팔로잉')
    await button(wrapper).trigger('mouseenter')
    expect(button(wrapper).text()).toBe('언팔로우')
    await button(wrapper).trigger('mouseleave')
    expect(button(wrapper).text()).toBe('팔로잉')

    await button(wrapper).trigger('click')
    await flushPromises()

    expect(followsApi.unfollow).toHaveBeenCalledWith(7)
    expect(button(wrapper).text()).toBe('팔로우')
    expect(wrapper.find('[data-link="followers"]').text()).toBe('팔로워 12')
  })

  it('AC9 비로그인이 누르면 로그인 화면으로 보낸다', async () => {
    const { wrapper, router } = await mountSummary({ userId: 7, isMe: false })

    await button(wrapper).trigger('click')
    await flushPromises()

    expect(followsApi.follow).not.toHaveBeenCalled()
    // 로그인 화면 컴포넌트를 지연 로드하므로 이동이 끝날 때까지 기다린다
    await vi.waitFor(() => expect(router.currentRoute.value.path).toBe('/login'))
    expect(router.currentRoute.value.query.redirect).toBe('/users/7')
  })

  it('AC9 요청이 실패하면 서버 message를 보이고 상태는 그대로다', async () => {
    followsApi.follow.mockRejectedValue(
      apiError(404, { code: 'USER_404', message: '존재하지 않거나 탈퇴한 사용자입니다.' }),
    )
    const { wrapper } = await mountSummary({ userId: 7, isMe: false }, { asUser: 1 })

    await button(wrapper).trigger('click')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toBe('존재하지 않거나 탈퇴한 사용자입니다.')
    expect(button(wrapper).text()).toBe('팔로우')
    expect(wrapper.find('[data-link="followers"]').text()).toBe('팔로워 12')
  })

  it('AC9 상태 조회가 실패하면 영역 안에 서버 message를 보인다', async () => {
    followsApi.fetchFollowStatus.mockRejectedValue(
      apiError(500, { code: 'COMMON_500', message: '서버에서 오류가 발생했습니다.' }),
    )
    const { wrapper } = await mountSummary({ userId: 7, isMe: false })

    expect(wrapper.find('[role="alert"]').text()).toBe('서버에서 오류가 발생했습니다.')
    expect(button(wrapper).exists()).toBe(false)
  })
})
