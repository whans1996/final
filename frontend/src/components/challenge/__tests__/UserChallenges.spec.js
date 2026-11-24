import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import UserChallenges from '@/components/challenge/UserChallenges.vue'
import * as challengesApi from '@/api/challenges'
import { routes } from '@/router'
import { apiError } from '@/views/__tests__/viewTestUtils'
import { myChallenge, page } from './fixtures'

vi.mock('@/api/challenges')

async function mountSection(props) {
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push('/')
  await router.isReady()
  const wrapper = mount(UserChallenges, { props, global: { plugins: [router] } })
  await flushPromises()
  return wrapper
}

const rows = (wrapper) => wrapper.findAll('[data-challenge-id]')

describe('UserChallenges 프로필 "참여 챌린지" 절 (F-25 AC15)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('AC15 참여 시각 최신순 6개와 개수, 달성 배지를 보여 준다', async () => {
    challengesApi.fetchUserChallenges.mockResolvedValue(
      page(
        [
          myChallenge(9, { completedAt: '2026-11-24T10:15:00+09:00', progress: 10 }),
          myChallenge(8),
        ],
        { size: 6, total: 2 },
      ),
    )
    const wrapper = await mountSection({ userId: 3 })

    expect(challengesApi.fetchUserChallenges).toHaveBeenCalledWith(3, { page: 1, size: 6 })
    expect(wrapper.find('h2').text()).toBe('참여 챌린지 2')
    expect(rows(wrapper).map((r) => r.attributes('href'))).toEqual([
      '/challenges/9',
      '/challenges/8',
    ])
    expect(rows(wrapper)[0].find('[data-badge="completed"]').text()).toBe('달성')
    expect(wrapper.find('[data-action="more-challenges"]').exists()).toBe(false)
  })

  it('AC15 더 있으면 "더 보기"로 다음 6개를 아래에 붙인다', async () => {
    const first = Array.from({ length: 6 }, (_, i) => myChallenge(20 - i))
    challengesApi.fetchUserChallenges
      .mockResolvedValueOnce(page(first, { size: 6, total: 7 }))
      .mockResolvedValueOnce(page([myChallenge(14)], { page: 2, size: 6, total: 7 }))
    const wrapper = await mountSection({ userId: 3 })

    await wrapper.find('[data-action="more-challenges"]').trigger('click')
    await flushPromises()

    expect(challengesApi.fetchUserChallenges).toHaveBeenLastCalledWith(3, { page: 2, size: 6 })
    expect(rows(wrapper)).toHaveLength(7)
    expect(wrapper.find('[data-action="more-challenges"]').exists()).toBe(false)
  })

  it('AC15 0건이면 안내 문구, 내 프로필이면 "챌린지 둘러보기" 링크', async () => {
    challengesApi.fetchUserChallenges.mockResolvedValue(page([], { size: 6, total: 0 }))

    const other = await mountSection({ userId: 3 })
    expect(other.text()).toContain('아직 참여한 챌린지가 없습니다.')
    expect(other.find('[data-action="browse-challenges"]').exists()).toBe(false)

    const mine = await mountSection({ userId: 3, isMe: true })
    expect(mine.find('[data-action="browse-challenges"]').attributes('href')).toBe('/challenges')
  })

  it('AC15 실패하면 절 안에 서버 메시지를 보여 준다', async () => {
    challengesApi.fetchUserChallenges.mockRejectedValue(
      apiError(500, { code: 'COMMON_500', message: '서버 오류가 발생했습니다.' }),
    )
    const wrapper = await mountSection({ userId: 3 })

    expect(wrapper.find('[role="alert"]').text()).toBe('서버 오류가 발생했습니다.')
  })
})
