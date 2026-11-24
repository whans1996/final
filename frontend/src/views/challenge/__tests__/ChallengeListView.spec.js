import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import ChallengeListView from '@/views/challenge/ChallengeListView.vue'
import ChallengeCard from '@/components/challenge/ChallengeCard.vue'
import * as challengesApi from '@/api/challenges'
import { useAuthStore } from '@/stores/auth'
import { apiError, mountAt } from '@/views/__tests__/viewTestUtils'
import { challengeSummary, myChallenge, page } from '@/components/challenge/__tests__/fixtures'

vi.mock('@/api/challenges')

const login = () =>
  useAuthStore().setSession({
    accessToken: 'access-1',
    user: { id: 1, nickname: '코딩초보', level: 'JUNIOR', role: 'USER' },
  })

async function mountList(path, { loggedIn = false } = {}) {
  const mounted = await mountAt(ChallengeListView, path, {
    beforeNavigate: loggedIn ? login : undefined,
  })
  await flushPromises()
  return mounted
}

const tabs = (wrapper) => wrapper.findAll('[data-status-tab]').map((t) => t.text())
const tab = (wrapper, value) => wrapper.find(`[data-status-tab="${value}"]`)

describe('ChallengeListView (F-25 AC12 SC-12)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    challengesApi.fetchChallenges.mockResolvedValue(
      page([challengeSummary(3), challengeSummary(2, { joined: true })]),
    )
    challengesApi.fetchMyChallenges.mockResolvedValue(page([myChallenge(2)]))
  })

  it('AC12 비로그인은 진행 중 탭으로 시작하고 상태 탭 세 개와 카드 격자를 본다', async () => {
    const { wrapper } = await mountList('/challenges')

    expect(tabs(wrapper)).toEqual(['진행 중', '예정', '종료'])
    expect(tab(wrapper, 'ONGOING').attributes('aria-selected')).toBe('true')
    expect(challengesApi.fetchChallenges).toHaveBeenCalledWith({
      status: 'ONGOING',
      page: 1,
      size: 12,
    })
    const cards = wrapper.findAllComponents(ChallengeCard)
    expect(cards.map((c) => c.props('challenge').id)).toEqual([3, 2])
  })

  it('AC12 탭을 누르면 쿼리에 반영하고 그 상태로 다시 불러온다', async () => {
    const { wrapper, router } = await mountList('/challenges?page=2')

    await tab(wrapper, 'UPCOMING').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.query).toEqual({ status: 'UPCOMING' })
    expect(challengesApi.fetchChallenges).toHaveBeenLastCalledWith({
      status: 'UPCOMING',
      page: 1,
      size: 12,
    })
  })

  it('AC12 로그인하면 "내 챌린지" 탭이 생기고, 누르면 /users/me/challenges로 달성도를 보여 준다', async () => {
    const { wrapper, router } = await mountList('/challenges', { loggedIn: true })
    expect(tabs(wrapper)).toEqual(['진행 중', '예정', '종료', '내 챌린지'])

    await tab(wrapper, 'MY').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.query).toEqual({ status: 'MY' })
    expect(challengesApi.fetchMyChallenges).toHaveBeenCalledWith({ page: 1, size: 12 })
    expect(wrapper.find('[role="progressbar"]').exists()).toBe(true)
  })

  it('AC12 비로그인이 ?status=MY로 오면 진행 중 탭을 보여 준다', async () => {
    const { wrapper } = await mountList('/challenges?status=MY')

    expect(challengesApi.fetchMyChallenges).not.toHaveBeenCalled()
    expect(tab(wrapper, 'ONGOING').attributes('aria-selected')).toBe('true')
  })

  it('AC12 빈 목록은 탭별 문구를 보여 준다', async () => {
    challengesApi.fetchChallenges.mockResolvedValue(page([]))
    challengesApi.fetchMyChallenges.mockResolvedValue(page([]))

    const upcoming = await mountList('/challenges?status=UPCOMING')
    expect(upcoming.wrapper.text()).toContain('예정된 챌린지가 없습니다.')

    const mine = await mountList('/challenges?status=MY', { loggedIn: true })
    expect(mine.wrapper.text()).toContain('참여한 챌린지가 없습니다.')
  })

  it('AC12 불러오기에 실패하면 서버 메시지와 "다시 시도"를 보여 준다', async () => {
    challengesApi.fetchChallenges.mockRejectedValueOnce(
      apiError(500, { code: 'COMMON_500', message: '서버 오류가 발생했습니다.' }),
    )
    const { wrapper } = await mountList('/challenges')

    expect(wrapper.find('[role="alert"]').text()).toContain('서버 오류가 발생했습니다.')
    await wrapper.find('[data-action="retry"]').trigger('click')
    await flushPromises()
    expect(wrapper.findAllComponents(ChallengeCard)).toHaveLength(2)
  })

  it('AC12 페이지를 넘기면 page 쿼리로 다시 불러온다', async () => {
    challengesApi.fetchChallenges.mockResolvedValue(
      page([challengeSummary(3)], { page: 1, size: 12, total: 30 }),
    )
    const { wrapper, router } = await mountList('/challenges?status=ENDED')

    await wrapper.find('[data-action="next"]').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.query).toEqual({ status: 'ENDED', page: '2' })
    expect(challengesApi.fetchChallenges).toHaveBeenLastCalledWith({
      status: 'ENDED',
      page: 2,
      size: 12,
    })
  })
})
