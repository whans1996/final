import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import UserFollowsView from '@/views/UserFollowsView.vue'
import * as usersApi from '@/api/users'
import * as followsApi from '@/api/follows'
import { apiError, deferred, mountAt } from './viewTestUtils'

vi.mock('@/api/users')
vi.mock('@/api/follows', async (importOriginal) => ({
  ...(await importOriginal()),
  fetchFollowStatus: vi.fn(),
  fetchFollowers: vi.fn(),
  fetchFollowings: vi.fn(),
}))

const person = (id, nickname = `회원${id}`, level = 'BEGINNER') => ({
  id,
  nickname,
  level,
  followedAt: '2026-11-20T10:00:00+09:00',
})

function page(items, { page = 1, total = items.length } = {}) {
  return { items, page, size: 20, total }
}

async function open(path) {
  const result = await mountAt(UserFollowsView, path)
  await flushPromises()
  return result
}

const ids = (wrapper) =>
  wrapper.findAll('[data-follow-user]').map((li) => Number(li.attributes('data-follow-user')))

describe('UserFollowsView (F-04 AC10)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    usersApi.fetchUser.mockResolvedValue({ id: 3, nickname: '코딩초보', level: 'JUNIOR' })
    followsApi.fetchFollowStatus.mockResolvedValue({
      following: false,
      followerCount: 2,
      followingCount: 1,
    })
    followsApi.fetchFollowers.mockResolvedValue(page([person(7, 'java러버', 'JUNIOR'), person(8)]))
    followsApi.fetchFollowings.mockResolvedValue(page([person(9)]))
  })

  it('AC10 제목·탭 수·팔로워 항목(이니셜·닉네임 링크·수준 배지)을 보여 준다', async () => {
    const { wrapper } = await open('/users/3/follows?tab=followers')

    expect(usersApi.fetchUser).toHaveBeenCalledWith(3)
    expect(followsApi.fetchFollowers).toHaveBeenCalledWith(3, 1)
    expect(wrapper.find('h1').text()).toBe('코딩초보님의 팔로우')
    expect(wrapper.find('[data-tab="followers"]').text()).toBe('팔로워 2')
    expect(wrapper.find('[data-tab="followings"]').text()).toBe('팔로잉 1')
    expect(wrapper.find('[data-tab="followers"]').attributes('aria-selected')).toBe('true')
    expect(ids(wrapper)).toEqual([7, 8])
    const first = wrapper.find('[data-follow-user="7"]')
    expect(first.find('.avatar').text()).toBe('J')
    expect(first.find('a').attributes('href')).toBe('/users/7')
    expect(first.find('a').text()).toContain('java러버')
    expect(first.find('.level-badge').text()).toBe('주니어')
    expect(wrapper.find('[data-link="profile"]').attributes('href')).toBe('/users/3')
  })

  it('AC10 탭을 바꾸면 ?tab=과 맞추고 팔로잉 목록을 부른다', async () => {
    const { wrapper, router } = await open('/users/3/follows?tab=followers')

    await wrapper.find('[data-tab="followings"]').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.query.tab).toBe('followings')
    expect(followsApi.fetchFollowings).toHaveBeenCalledWith(3, 1)
    expect(ids(wrapper)).toEqual([9])
  })

  it('AC10 잘못된 tab 값이면 팔로워 탭이다', async () => {
    const { wrapper } = await open('/users/3/follows?tab=nope')

    expect(followsApi.fetchFollowers).toHaveBeenCalledWith(3, 1)
    expect(followsApi.fetchFollowings).not.toHaveBeenCalled()
    expect(wrapper.find('[data-tab="followers"]').attributes('aria-selected')).toBe('true')
  })

  it('AC10 "더 보기"는 다음 쪽을 붙이고 이미 보인 id는 거른다', async () => {
    const first = Array.from({ length: 20 }, (_, i) => person(100 - i))
    followsApi.fetchFollowers
      .mockResolvedValueOnce(page(first, { total: 22 }))
      .mockResolvedValueOnce(page([person(81), person(80), person(79)], { page: 2, total: 23 }))
    const { wrapper } = await open('/users/3/follows')

    expect(ids(wrapper)).toHaveLength(20)
    await wrapper.find('[data-action="more"]').trigger('click')
    await flushPromises()

    expect(followsApi.fetchFollowers).toHaveBeenLastCalledWith(3, 2)
    expect(ids(wrapper)).toHaveLength(22)
    expect(ids(wrapper).slice(-2)).toEqual([80, 79])
    // 다음 쪽이 20개보다 적으면 끝이다
    expect(wrapper.find('[data-action="more"]').exists()).toBe(false)
  })

  it('AC10 0명이면 탭별 안내 문구를 보여 준다', async () => {
    followsApi.fetchFollowers.mockResolvedValue(page([]))
    followsApi.fetchFollowings.mockResolvedValue(page([]))

    const { wrapper } = await open('/users/3/follows?tab=followers')
    expect(wrapper.text()).toContain('아직 팔로워가 없습니다.')

    await wrapper.find('[data-tab="followings"]').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('아직 팔로우한 사람이 없습니다.')
  })

  it('AC10 USER_404면 탈퇴·없는 사용자 안내를 보여 준다', async () => {
    usersApi.fetchUser.mockRejectedValue(
      apiError(404, { code: 'USER_404', message: '존재하지 않거나 탈퇴한 사용자입니다.' }),
    )
    followsApi.fetchFollowStatus.mockRejectedValue(
      apiError(404, { code: 'USER_404', message: '존재하지 않거나 탈퇴한 사용자입니다.' }),
    )
    followsApi.fetchFollowers.mockRejectedValue(
      apiError(404, { code: 'USER_404', message: '존재하지 않거나 탈퇴한 사용자입니다.' }),
    )

    const { wrapper } = await open('/users/3/follows')

    expect(wrapper.text()).toContain('탈퇴했거나 없는 사용자입니다.')
    expect(wrapper.find('[data-follow-user]').exists()).toBe(false)
  })

  it('AC10 그 밖의 에러는 서버 message를 보여 준다', async () => {
    followsApi.fetchFollowers.mockRejectedValue(
      apiError(500, { code: 'COMMON_500', message: '서버에서 오류가 발생했습니다.' }),
    )

    const { wrapper } = await open('/users/3/follows')

    expect(wrapper.find('[role="alert"]').text()).toBe('서버에서 오류가 발생했습니다.')
  })
})

describe('UserFollowsView 머리 늦은 응답 (V-4 AC6·AC7)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    followsApi.fetchFollowStatus.mockResolvedValue({
      following: false,
      followerCount: 2,
      followingCount: 1,
    })
    followsApi.fetchFollowers.mockResolvedValue(page([]))
    followsApi.fetchFollowings.mockResolvedValue(page([]))
  })

  it('AC6·AC7 이전 회원의 늦은 실패와 화면을 떠난 뒤 온 응답은 머리를 바꾸지 않는다', async () => {
    const first = deferred()
    const second = deferred()
    usersApi.fetchUser.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise)
    const { wrapper, router } = await open('/users/3/follows')

    await router.push('/users/4/follows')
    first.reject(apiError(500, { code: 'COMMON_500', message: '옛 회원 실패' }))
    await flushPromises()
    const vm = wrapper.vm
    expect(vm.headError).toBeNull()

    wrapper.unmount()
    second.resolve({ id: 4, nickname: '늦은 닉네임', level: 'JUNIOR' })
    await flushPromises()
    expect(vm.nickname).toBe('')
    expect(vm.status).toBeNull()
  })
})
