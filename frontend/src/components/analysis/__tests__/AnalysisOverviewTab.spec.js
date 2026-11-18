import { describe, expect, it } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { routes } from '@/router'
import AnalysisOverviewTab from '@/components/analysis/AnalysisOverviewTab.vue'

const overview = {
  summary: '할 일을 등록하고 완료 표시하는 Todo 웹 서비스입니다.',
  techStack: ['Spring Boot', 'MyBatis', 'Vue 3'],
  layers: [
    { layer: 'vue', role: '목록·입력 화면을 그립니다.' },
    { layer: 'controller', role: 'HTTP 요청을 받습니다.' },
    { layer: 'mapper', role: 'SQL로 todos 테이블을 읽고 씁니다.' },
  ],
  readingOrder: [
    { fileId: 101, path: 'src/TodoController.java', reason: '어떤 API가 있는지 먼저 봅니다.' },
    { fileId: 102, path: 'src/TodoService.java', reason: '처리 흐름을 봅니다.' },
  ],
  apis: [
    { method: 'GET', path: '/api/todos', fileId: 101 },
    { method: 'POST', path: '/api/todos', fileId: 101 },
  ],
}

async function mountTab(props) {
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push('/analyses/42')
  await router.isReady()
  const wrapper = mount(AnalysisOverviewTab, { props, global: { plugins: [router] } })
  return { wrapper, router }
}

describe('AnalysisOverviewTab (F-06 AC13)', () => {
  it('AC13 요약과 기술 스택 칩을 보여 준다', async () => {
    const { wrapper } = await mountTab({ overview })

    expect(wrapper.find('[data-section="summary"]').text()).toContain('Todo 웹 서비스')
    const chips = wrapper.findAll('[data-section="tech-stack"] li').map((li) => li.text())
    expect(chips).toEqual(['Spring Boot', 'MyBatis', 'Vue 3'])
  })

  it('AC13 레이어별 역할을 한국어 라벨로, 선별 우선순위 순서로 보여 준다', async () => {
    const { wrapper } = await mountTab({ overview })

    const rows = wrapper.findAll('[data-section="layers"] li').map((li) => li.text())
    expect(rows).toHaveLength(3)
    expect(rows[0]).toContain('컨트롤러')
    expect(rows[0]).toContain('HTTP 요청을 받습니다.')
    expect(rows[1]).toContain('매퍼(DB)')
    expect(rows[2]).toContain('화면(Vue)')
  })

  it('AC13 읽는 순서를 누르면 파일 탭에서 그 파일을 선택한다', async () => {
    const { wrapper, router } = await mountTab({ overview })

    const links = wrapper.findAll('[data-section="reading-order"] a')
    expect(links).toHaveLength(2)
    expect(links[0].text()).toContain('src/TodoController.java')
    expect(wrapper.find('[data-section="reading-order"]').text()).toContain(
      '어떤 API가 있는지 먼저 봅니다.',
    )

    await links[1].trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual({ tab: 'files', file: '102' })
  })

  it('AC13 API 표에 메서드 배지·경로·파일 링크를 보여 준다', async () => {
    const { wrapper } = await mountTab({ overview })

    const rows = wrapper.findAll('[data-section="apis"] tbody tr')
    expect(rows).toHaveLength(2)
    expect(rows[0].find('[data-method]').text()).toBe('GET')
    expect(rows[0].text()).toContain('/api/todos')
    expect(rows[1].find('[data-method]').text()).toBe('POST')
    expect(rows[0].find('a').attributes('href')).toBe('/analyses/42?tab=files&file=101')
  })

  it('AC13 API가 비면 "탐지된 API가 없습니다"를 보여 준다', async () => {
    const { wrapper } = await mountTab({ overview: { ...overview, apis: [] } })

    expect(wrapper.find('[data-section="apis"] table').exists()).toBe(false)
    expect(wrapper.find('[data-section="apis"]').text()).toContain('탐지된 API가 없습니다')
  })

  it('AC13 개요가 없으면 안내 문구를 보여 준다', async () => {
    const { wrapper } = await mountTab({ overview: null })

    expect(wrapper.text()).toContain('개요가 아직 없습니다')
  })
})
