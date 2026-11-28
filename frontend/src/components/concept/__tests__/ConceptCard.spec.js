import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import ConceptCard from '@/components/concept/ConceptCard.vue'

const concept = {
  id: 12,
  name: 'JWT 인증',
  category: 'security',
  difficulty: 'JUNIOR',
  summary: '서버가 서명한 토큰으로 요청한 사람을 확인하는 방식이다.',
  avgRating: 4,
  wishCount: 12,
}

function mountCard(props) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }],
  })
  return mount(ConceptCard, { props, global: { plugins: [router] } })
}

// F-00b AC10: 카드 목록(F-09)·찜(F-12)·추천(F-16)이 함께 쓰는 카드 요약
describe('ConceptCard (F-00b AC10)', () => {
  it('AC10 이름, 카테고리, 난이도 라벨, 요약, 평점(소수 1자리), 찜 수를 보인다', () => {
    const wrapper = mountCard({ concept })

    expect(wrapper.find('.name').text()).toBe('JWT 인증')
    expect(wrapper.find('.category').text()).toBe('Security') // K-1 AC12: 원문 대신 라벨
    expect(wrapper.find('.difficulty').text()).toBe('주니어')
    expect(wrapper.find('.summary').text()).toBe(concept.summary)
    expect(wrapper.find('.rating').text()).toContain('4.0')
    expect(wrapper.find('.wish').text()).toContain('12')
  })

  it('AC10 카드 전체가 /concepts/{id} 링크다', () => {
    const wrapper = mountCard({ concept })

    expect(wrapper.element.tagName).toBe('A')
    expect(wrapper.attributes('href')).toBe('/concepts/12')
  })

  it('AC10 다른 난이도도 LEVELS 라벨로 보인다', () => {
    expect(
      mountCard({ concept: { ...concept, difficulty: 'BEGINNER' } })
        .find('.difficulty')
        .text(),
    ).toBe('입문')
    expect(
      mountCard({ concept: { ...concept, difficulty: 'SENIOR' } })
        .find('.difficulty')
        .text(),
    ).toBe('숙련')
  })
})
