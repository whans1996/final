import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import FeedView from '@/views/community/FeedView.vue'
import * as feedApi from '@/api/feed'
import * as followsApi from '@/api/follows'
import { authGuard, routes } from '@/router'
import { useAuthStore } from '@/stores/auth'
import { apiError, mountAt } from '@/views/__tests__/viewTestUtils'

vi.mock('@/api/feed', async (importOriginal) => ({
  ...(await importOriginal()),
  fetchFeed: vi.fn(),
}))
vi.mock('@/api/follows')

const ME = 1

function login() {
  useAuthStore().setSession({
    accessToken: 'access-1',
    user: { id: ME, nickname: '나', level: 'JUNIOR', role: 'USER' },
  })
}

const activity = (id, type = 'POST') => ({
  id,
  type,
  createdAt: '2026-11-20T10:00:00+09:00',
  actor: { id: 7, nickname: 'java러버' },
  target: { id: id + 1000, title: `글 ${id}`, rating: null },
})

function page(items, { page = 1, total = items.length } = {}) {
  return { items, page, size: 20, total }
}

async function open() {
  const result = await mountAt(FeedView, '/feed', { beforeNavigate: login })
  await flushPromises()
  return result
}

const ids = (wrapper) =>
  wrapper.findAll('[data-activity]').map((li) => Number(li.attributes('data-activity')))

describe('FeedView (F-04 AC11)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    followsApi.fetchFollowStatus.mockResolvedValue({
      following: false,
      followerCount: 0,
      followingCount: 2,
    })
  })

  it('AC11 /feed는 로그인 필요이고 회원 nav "피드"로 들어온다', () => {
    const feed = routes.find((r) => r.path === '/feed')
    expect(feed.meta.requiresAuth).toBe(true)
    expect(feed.meta.nav).toMatchObject({ label: '피드', show: 'user' })
    expect(authGuard({ meta: feed.meta, fullPath: '/feed' }, { isLoggedIn: false })).toEqual({
      name: 'login',
      query: { redirect: '/feed' },
    })
  })

  it('AC11 팔로우한 사람의 활동을 순서대로 보여 주고 모르는 type은 그리지 않는다', async () => {
    feedApi.fetchFeed.mockResolvedValue(
      page([activity(3), activity(2, 'FOLLOW'), activity(1, 'REVIEW')]),
    )

    const { wrapper } = await open()

    expect(feedApi.fetchFeed).toHaveBeenCalledWith(1)
    expect(ids(wrapper)).toEqual([3, 1])
  })

  it('AC11 "더 보기"는 다음 쪽을 붙이고 이미 보인 활동 id는 거른다', async () => {
    const first = Array.from({ length: 20 }, (_, i) => activity(100 - i))
    feedApi.fetchFeed
      .mockResolvedValueOnce(page(first, { total: 23 }))
      .mockResolvedValueOnce(page([activity(81), activity(80)], { page: 2, total: 24 }))

    const { wrapper } = await open()
    await wrapper.find('[data-action="more"]').trigger('click')
    await flushPromises()

    expect(feedApi.fetchFeed).toHaveBeenLastCalledWith(2)
    expect(ids(wrapper)).toHaveLength(21)
    expect(ids(wrapper).at(-1)).toBe(80)
    expect(wrapper.find('[data-action="more"]').exists()).toBe(false)
  })

  it('AC11 팔로우한 사람이 없으면 안내와 커뮤니티 둘러보기 링크를 보여 준다', async () => {
    feedApi.fetchFeed.mockResolvedValue(page([]))
    followsApi.fetchFollowStatus.mockResolvedValue({
      following: false,
      followerCount: 0,
      followingCount: 0,
    })

    const { wrapper } = await open()

    expect(followsApi.fetchFollowStatus).toHaveBeenCalledWith(ME)
    expect(wrapper.text()).toContain('아직 팔로우한 사람이 없어요')
    expect(wrapper.find('[data-action="browse-community"]').attributes('href')).toBe('/community')
  })

  it('AC11 빈 피드는 팔로잉 수를 확인할 때까지 빈 안내 대신 "불러오는 중…"이다', async () => {
    feedApi.fetchFeed.mockResolvedValue(page([]))
    let resolve
    followsApi.fetchFollowStatus.mockReturnValue(new Promise((r) => (resolve = r)))

    const { wrapper } = await open()

    expect(wrapper.text()).toContain('불러오는 중…')
    expect(wrapper.text()).not.toContain('새 활동이 없습니다')
    expect(wrapper.text()).not.toContain('아직 팔로우한 사람이 없어요')

    resolve({ following: false, followerCount: 0, followingCount: 0 })
    await flushPromises()

    expect(wrapper.text()).not.toContain('불러오는 중…')
    expect(wrapper.text()).toContain('아직 팔로우한 사람이 없어요')
  })

  it('AC11 팔로우는 했지만 활동이 없으면 "새 활동이 없습니다"', async () => {
    feedApi.fetchFeed.mockResolvedValue(page([]))

    const { wrapper } = await open()

    expect(wrapper.text()).toContain('새 활동이 없습니다')
    expect(wrapper.find('[data-action="browse-community"]').exists()).toBe(false)
  })

  it('AC11 에러는 서버 message를 보여 준다', async () => {
    feedApi.fetchFeed.mockRejectedValue(
      apiError(500, { code: 'COMMON_500', message: '서버에서 오류가 발생했습니다.' }),
    )

    const { wrapper } = await open()

    expect(wrapper.find('[role="alert"]').text()).toBe('서버에서 오류가 발생했습니다.')
  })
})
