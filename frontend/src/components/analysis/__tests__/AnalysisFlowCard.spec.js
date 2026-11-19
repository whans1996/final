import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import AnalysisFlowCard from '@/components/analysis/AnalysisFlowCard.vue'

function flow(partial) {
  return {
    id: 7,
    method: 'POST',
    path: '/api/todos',
    status: 'DONE',
    errorCode: null,
    errorMessage: null,
    summary: '새 할 일을 등록하면 컨트롤러가 입력을 검사합니다.',
    steps: [
      {
        no: 1,
        layer: 'controller',
        fileId: 101,
        path: 'src/main/java/todo/TodoController.java',
        symbol: 'TodoController.create',
        title: '요청 받기',
        description: '@RequestBody로 본문을 받습니다.',
      },
      {
        no: 2,
        layer: null,
        fileId: null,
        path: 'src/Unknown.java',
        symbol: null,
        title: '알 수 없는 단계',
        description: '설명',
      },
    ],
    createdAt: '2026-12-09T10:00:00+09:00',
    ...partial,
  }
}

describe('AnalysisFlowCard (F-07)', () => {
  it('AC15 DONE이면 요약과 단계(번호, 레이어, title, symbol, 설명)를 보여 준다', () => {
    const wrapper = mount(AnalysisFlowCard, { props: { flow: flow() } })

    expect(wrapper.find('.method').text()).toBe('POST')
    expect(wrapper.text()).toContain('/api/todos')
    expect(wrapper.text()).toContain('완료')
    expect(wrapper.text()).toContain('새 할 일을 등록하면 컨트롤러가 입력을 검사합니다.')
    const steps = wrapper.findAll('[data-step]')
    expect(steps).toHaveLength(2)
    expect(steps[0].text()).toContain('1')
    expect(steps[0].text()).toContain('컨트롤러')
    expect(steps[0].text()).toContain('요청 받기')
    expect(steps[0].text()).toContain('TodoController.create')
    expect(steps[0].text()).toContain('@RequestBody로 본문을 받습니다.')
  })

  it('AC15 fileId가 있는 단계 경로를 누르면 파일을 고르고 없으면 링크가 아니다', async () => {
    const wrapper = mount(AnalysisFlowCard, { props: { flow: flow() } })

    const links = wrapper.findAll('[data-action="open-file"]')
    expect(links).toHaveLength(1)
    await links[0].trigger('click')

    expect(wrapper.emitted('select-file')).toEqual([[101]])
    expect(wrapper.findAll('[data-step]')[1].text()).toContain('src/Unknown.java')
  })

  it('AC15 PENDING이면 생성 중 문구를 보여 준다', () => {
    const wrapper = mount(AnalysisFlowCard, {
      props: { flow: flow({ status: 'PENDING', summary: null, steps: null }) },
    })

    expect(wrapper.text()).toContain('생성 중')
    expect(wrapper.text()).toContain('실행 흐름을 만드는 중입니다…')
    expect(wrapper.find('[data-step]').exists()).toBe(false)
  })

  it('AC16 FAILED면 errorMessage와 다시 만들기를 보여 주고 누르면 retry를 보낸다', async () => {
    const wrapper = mount(AnalysisFlowCard, {
      props: {
        flow: flow({
          status: 'FAILED',
          errorCode: 'LLM_502',
          errorMessage: 'AI 응답 형식이 올바르지 않습니다.',
          summary: null,
          steps: null,
        }),
      },
    })

    expect(wrapper.text()).toContain('실패')
    expect(wrapper.text()).toContain('AI 응답 형식이 올바르지 않습니다.')
    await wrapper.find('[data-action="retry-flow"]').trigger('click')
    expect(wrapper.emitted('retry')).toHaveLength(1)
  })
})
