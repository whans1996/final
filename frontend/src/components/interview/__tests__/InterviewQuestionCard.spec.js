import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import InterviewQuestionCard from '@/components/interview/InterviewQuestionCard.vue'
import { question } from './fixtures'

async function mountCard(q = question()) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/analyses/:id', name: 'analysis-detail', component: { template: '<div />' } },
      { path: '/concepts/:id', name: 'concept-detail', component: { template: '<div />' } },
    ],
  })
  await router.push('/analyses/42?tab=interview')
  await router.isReady()
  const wrapper = mount(InterviewQuestionCard, {
    props: { question: q },
    global: { plugins: [router] },
  })
  return { wrapper, router }
}

describe('InterviewQuestionCard (I-1a AC13)', () => {
  it('AC13 번호와 질문을 보이고 핵심 포인트는 눌러야 펼쳐진다', async () => {
    const { wrapper } = await mountCard()

    expect(wrapper.text()).toContain('Q1.')
    expect(wrapper.text()).toContain('controller·service·mapper로 나눈 이유')
    expect(wrapper.find('[data-points]').exists()).toBe(false)
    const toggle = wrapper.find('[data-action="toggle-points"]')
    expect(toggle.attributes('aria-expanded')).toBe('false')
    expect(toggle.text()).toBe('핵심 포인트 보기')

    await toggle.trigger('click')

    expect(toggle.attributes('aria-expanded')).toBe('true')
    expect(wrapper.findAll('[data-points] li').map((l) => l.text())).toEqual([
      '컨트롤러는 요청 검증만 한다',
      '서비스가 트랜잭션을 맡는다',
    ])
    await toggle.trigger('click')
    expect(wrapper.find('[data-points]').exists()).toBe(false)
  })

  it('AC13 근거 칩은 path:L12-L30이고 파일 탭의 그 줄들로 가는 링크다', async () => {
    const { wrapper } = await mountCard()

    const chips = wrapper.findAll('[data-evidence]')
    expect(chips.map((c) => c.text())).toEqual([
      'src/main/java/todo/TodoController.java:L12-L30',
      'src/main/java/todo/TodoService.java:L7',
    ])
    expect(chips[0].attributes('href')).toBe('/analyses/42?tab=files&file=101&lines=12-30')
    expect(chips[1].attributes('href')).toBe('/analyses/42?tab=files&file=102&lines=7')
  })

  it('AC13 관련 카드는 카드 상세로 가고, 없으면 영역이 없다', async () => {
    const { wrapper } = await mountCard()

    const concept = wrapper.find('[data-concept="12"]')
    expect(concept.text()).toBe('계층형 아키텍처')
    expect(concept.attributes('href')).toBe('/concepts/12')

    const { wrapper: empty } = await mountCard(question({ concepts: [] }))
    expect(empty.text()).not.toContain('관련 카드')
  })
})
