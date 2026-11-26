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

    expect(challengesApi.fetchMyChallenges).toHaveBeenCalledWith({
      status: 'ONGOING',
      size: 50,
      page: 1,
    })
    const items = wrapper.findAll('[data-challenge-id]')
    expect(items.map((i) => i.attributes('data-challenge-id'))).toEqual(['1', '3'])
    expect(items[0].find('a').attributes('href')).toBe('/challenges/1')
    expect(items[0].text()).toContain('퀴즈 10번')
    expect(items[0].text()).toContain('4 / 10회')
    expect(items[0].find('[data-badge="completed"]').exists()).toBe(false)
    expect(items[1].find('[data-badge="completed"]').text()).toBe('달성 완료')
    const section = wrapper.find('[data-section="challenge-progress"]')
    expect(section.attributes('role')).toBe('status')
    expect(section.attributes('aria-labelledby')).toBe(section.find('h2').attributes('id'))
    expect(section.find('h2 [aria-hidden="true"]').text()).toBe('🏁')
  })

  it('#225 진행 중 참여 챌린지가 한 페이지를 넘으면 다음 페이지까지 이어 받아 거른다', async () => {
    const first = Array.from({ length: 50 }, (_, i) =>
      myChallenge(i + 1, { goalType: 'REVIEW_COUNT' }),
    )
    challengesApi.fetchMyChallenges
      .mockResolvedValueOnce(page(first, { size: 50, total: 52 }))
      .mockResolvedValueOnce(
        page([myChallenge(51, { goalType: 'REVIEW_COUNT' }), myChallenge(52)], {
          page: 2,
          size: 50,
          total: 52,
        }),
      )

    const wrapper = await mountNotice({ goalType: 'QUIZ_COUNT' })

    expect(challengesApi.fetchMyChallenges).toHaveBeenNthCalledWith(1, {
      status: 'ONGOING',
      size: 50,
      page: 1,
    })
    expect(challengesApi.fetchMyChallenges).toHaveBeenNthCalledWith(2, {
      status: 'ONGOING',
      size: 50,
      page: 2,
    })
    expect(challengesApi.fetchMyChallenges).toHaveBeenCalledTimes(2)
    expect(
      wrapper.findAll('[data-challenge-id]').map((i) => i.attributes('data-challenge-id')),
    ).toEqual(['52'])
  })

  it('#225 total이 줄어 빈 페이지가 오면 더 부르지 않는다', async () => {
    challengesApi.fetchMyChallenges
      .mockResolvedValueOnce(page([myChallenge(1)], { size: 50, total: 120 }))
      .mockResolvedValueOnce(page([], { page: 2, size: 50, total: 1 }))

    const wrapper = await mountNotice({ goalType: 'QUIZ_COUNT' })

    expect(challengesApi.fetchMyChallenges).toHaveBeenCalledTimes(2)
    expect(wrapper.findAll('[data-challenge-id]')).toHaveLength(1)
  })

  it('#225 받는 사이 항목이 밀려 겹치면 한 번만 보인다', async () => {
    const first = Array.from({ length: 50 }, (_, i) => myChallenge(i + 1))
    challengesApi.fetchMyChallenges
      .mockResolvedValueOnce(page(first, { size: 50, total: 51 }))
      .mockResolvedValueOnce(page([myChallenge(50)], { page: 2, size: 50, total: 51 }))

    const wrapper = await mountNotice({ goalType: 'QUIZ_COUNT' })

    expect(wrapper.findAll('[data-challenge-id]')).toHaveLength(50)
  })

  it('#225 페이지는 20번까지만 이어 받는다', async () => {
    challengesApi.fetchMyChallenges.mockImplementation(({ page: p }) =>
      Promise.resolve(page([myChallenge(p)], { page: p, size: 50, total: 5000 })),
    )

    const wrapper = await mountNotice({ goalType: 'QUIZ_COUNT' })

    expect(challengesApi.fetchMyChallenges).toHaveBeenCalledTimes(20)
    expect(wrapper.findAll('[data-challenge-id]')).toHaveLength(20)
  })

  it('#225 중간 페이지가 실패하면 아무것도 보이지 않는다', async () => {
    challengesApi.fetchMyChallenges
      .mockResolvedValueOnce(page([myChallenge(1)], { size: 50, total: 60 }))
      .mockRejectedValueOnce(apiError(500, { message: '서버 오류' }))

    const wrapper = await mountNotice({ goalType: 'QUIZ_COUNT' })

    expect(wrapper.find('[data-section="challenge-progress"]').exists()).toBe(false)
  })

  it('#225 여러 페이지를 받는 중에 refreshKey가 바뀌면 옛 요청 결과는 버린다', async () => {
    let resolveOldSecond
    challengesApi.fetchMyChallenges
      .mockResolvedValueOnce(page([myChallenge(1, { progress: 1 })], { size: 50, total: 2 }))
      .mockReturnValueOnce(
        new Promise((resolve) => {
          resolveOldSecond = resolve
        }),
      )
      .mockResolvedValueOnce(page([myChallenge(1, { progress: 7 })], { size: 50, total: 1 }))

    const wrapper = await mountNotice({ goalType: 'QUIZ_COUNT', refreshKey: 1 })
    await wrapper.setProps({ refreshKey: 2 })
    await flushPromises()
    resolveOldSecond(page([myChallenge(2)], { page: 2, size: 50, total: 2 }))
    await flushPromises()

    expect(challengesApi.fetchMyChallenges).toHaveBeenCalledTimes(3)
    const items = wrapper.findAll('[data-challenge-id]')
    expect(items.map((i) => i.attributes('data-challenge-id'))).toEqual(['1'])
    expect(items[0].text()).toContain('7 / 10회')
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
