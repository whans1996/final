import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import RecommendationCard from '@/components/recommendation/RecommendationCard.vue'
import { recommendation } from './fixtures'

function mountCard(item) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/:p(.*)*', component: { template: '<div />' } }],
  })
  return mount(RecommendationCard, { props: { item }, global: { plugins: [router] } })
}

describe('RecommendationCard (F-16)', () => {
  it('AC8 카드 요약, 이유 한 줄, 신호 배지, AI 표시를 보여 준다', () => {
    const wrapper = mountCard(recommendation(12))

    expect(wrapper.find('[data-concept-id="12"]').exists()).toBe(true)
    expect(wrapper.find('[data-field="reason"]').text()).toBe('카드12를 볼 이유')
    expect(wrapper.findAll('[data-signal]').map((b) => b.text())).toEqual([
      '오답 2회',
      '내 코드',
      '찜한 분야',
      '수준 맞음',
    ])
    expect(wrapper.find('[data-field="ai"]').text()).toBe('AI')
  })

  it('AC8 해당하지 않는 신호는 배지를 빼고 RULE이면 AI 표시가 없다', () => {
    const wrapper = mountCard(
      recommendation(3, {
        signals: { wrongCount: 0, inMyCode: false, wishedCategory: false, levelFit: 0 },
        reasonSource: 'RULE',
      }),
    )

    expect(wrapper.findAll('[data-signal]')).toHaveLength(0)
    expect(wrapper.find('[data-field="ai"]').exists()).toBe(false)
    expect(wrapper.find('[data-field="reason"]').text()).toBe('카드3를 볼 이유')
  })
})
