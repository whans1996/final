import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import RecommendationList from '@/components/recommendation/RecommendationList.vue'
import * as recommendationsApi from '@/api/recommendations'
import { recommendation } from './fixtures'

vi.mock('@/api/recommendations')

function apiError(status, data) {
  return Object.assign(new Error(`status ${status}`), { response: { status, data } })
}

function mountList(props = {}) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/:p(.*)*', component: { template: '<div />' } }],
  })
  return mount(RecommendationList, { props, global: { plugins: [router] } })
}

describe('RecommendationList (F-16)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('AC8 불러오는 동안 안내 문구를 보이고, 받은 카드를 순서대로 보여 준다', async () => {
    let resolve
    recommendationsApi.fetchRecommendations.mockReturnValue(new Promise((r) => (resolve = r)))
    const wrapper = mountList({ title: '맞춤 추천' })

    expect(wrapper.find('h2').text()).toBe('맞춤 추천')
    expect(wrapper.text()).toContain('추천을 만드는 중…')
    expect(recommendationsApi.fetchRecommendations).toHaveBeenCalledWith({ size: 5 })

    resolve({ items: [recommendation(1), recommendation(2)] })
    await flushPromises()

    expect(wrapper.text()).not.toContain('추천을 만드는 중…')
    expect(
      wrapper.findAll('[data-concept-id]').map((c) => c.attributes('data-concept-id')),
    ).toEqual(['1', '2'])
  })

  it('AC8 빈 결과면 안내 문구를 보여 준다', async () => {
    recommendationsApi.fetchRecommendations.mockResolvedValue({ items: [] })
    const wrapper = mountList({ title: '맞춤 추천' })
    await flushPromises()

    expect(wrapper.text()).toContain('추천할 카드가 없습니다.')
  })

  it('AC8 실패하면 서버 메시지와 다시 시도를 보여 주고, 다시 시도하면 다시 부른다', async () => {
    recommendationsApi.fetchRecommendations
      .mockRejectedValueOnce(apiError(500, { code: 'COMMON_500', message: '서버 오류입니다.' }))
      .mockResolvedValueOnce({ items: [recommendation(7)] })
    const wrapper = mountList({ title: '맞춤 추천', size: 3 })
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toBe('서버 오류입니다.')
    await wrapper.find('[data-action="retry-recommendations"]').trigger('click')
    await flushPromises()

    expect(recommendationsApi.fetchRecommendations).toHaveBeenCalledTimes(2)
    expect(recommendationsApi.fetchRecommendations).toHaveBeenLastCalledWith({ size: 3 })
    expect(wrapper.find('[data-concept-id="7"]').exists()).toBe(true)
  })

  it('AC10 hideOnEmptyOrError면 빈 결과·실패·로딩 중에 아무것도 보이지 않는다', async () => {
    recommendationsApi.fetchRecommendations.mockResolvedValue({ items: [] })
    const empty = mountList({ title: '이런 카드부터 시작해 보세요', hideOnEmptyOrError: true })
    expect(empty.find('h2').exists()).toBe(false)
    await flushPromises()
    expect(empty.find('h2').exists()).toBe(false)
    expect(empty.text()).toBe('')

    recommendationsApi.fetchRecommendations.mockRejectedValue(apiError(500, { message: '오류' }))
    const failed = mountList({ title: '이런 카드부터 시작해 보세요', hideOnEmptyOrError: true })
    await flushPromises()
    expect(failed.text()).toBe('')
  })
})
