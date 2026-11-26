import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import ChallengeProgressNotice from '@/components/challenge/ChallengeProgressNotice.vue'
import * as challengesApi from '@/api/challenges'
import { routes } from '@/router'
import { apiError } from '@/views/__tests__/viewTestUtils'
import { myChallenge, page } from './fixtures'

vi.mock('@/api/challenges', async (importOriginal) => ({
  ...(await importOriginal()),
  fetchMyChallenges: vi.fn(),
}))

async function mountNotice(props) {
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push('/')
  await router.isReady()
  const wrapper = mount(ChallengeProgressNotice, { props, global: { plugins: [router] } })
  await flushPromises()
  return wrapper
}

describe('ChallengeProgressNotice (#211 완료 화면의 챌린지 진행)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('#211 진행 중인 내 챌린지 중 같은 목표 유형만 달성도와 상세 링크로 보여 준다', async () => {
    challengesApi.fetchMyChallenges.mockResolvedValue(
      page([
        myChallenge(1, { title: '퀴즈 10번', progress: 4 }),
        myChallenge(2, { goalType: 'REVIEW_COUNT', title: '복습 5번' }),
        myChallenge(3, {
          title: '퀴즈 3번',
          goalValue: 3,
          progress: 3,
          completedAt: '2026-11-26T10:00:00+09:00',
        }),
      ]),
    )

    const wrapper = await mountNotice({ goalType: 'QUIZ_COUNT' })

    expect(challengesApi.fetchMyChallenges).toHaveBeenCalledWith({ status: 'ONGOING', size: 50 })
    const items = wrapper.findAll('[data-challenge-id]')
    expect(items.map((i) => i.attributes('data-challenge-id'))).toEqual(['1', '3'])
    expect(items[0].find('a').attributes('href')).toBe('/challenges/1')
    expect(items[0].text()).toContain('퀴즈 10번')
    expect(items[0].text()).toContain('4 / 10회')
    expect(items[0].find('[data-badge="completed"]').exists()).toBe(false)
    expect(items[1].find('[data-badge="completed"]').text()).toBe('달성 완료')
  })

  it('#211 같은 유형의 챌린지가 없거나 조회가 실패하면 아무것도 보이지 않는다', async () => {
    challengesApi.fetchMyChallenges.mockResolvedValue(
      page([myChallenge(2, { goalType: 'REVIEW_COUNT' })]),
    )
    const empty = await mountNotice({ goalType: 'ANALYSIS_COUNT' })
    expect(empty.find('[data-section="challenge-progress"]').exists()).toBe(false)

    challengesApi.fetchMyChallenges.mockRejectedValue(apiError(500, { message: '서버 오류' }))
    const failed = await mountNotice({ goalType: 'QUIZ_COUNT' })
    expect(failed.find('[data-section="challenge-progress"]').exists()).toBe(false)
    expect(failed.find('[role="alert"]').exists()).toBe(false)
  })

  it('#211 refreshKey가 바뀌면 다시 불러온다', async () => {
    challengesApi.fetchMyChallenges.mockResolvedValue(page([myChallenge(1, { progress: 4 })]))
    const wrapper = await mountNotice({ goalType: 'QUIZ_COUNT', refreshKey: 1 })

    challengesApi.fetchMyChallenges.mockResolvedValue(page([myChallenge(1, { progress: 5 })]))
    await wrapper.setProps({ refreshKey: 2 })
    await flushPromises()

    expect(challengesApi.fetchMyChallenges).toHaveBeenCalledTimes(2)
    expect(wrapper.find('[data-challenge-id="1"]').text()).toContain('5 / 10회')
  })
})
