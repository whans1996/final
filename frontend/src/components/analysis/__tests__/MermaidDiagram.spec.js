import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import MermaidDiagram from '@/components/analysis/MermaidDiagram.vue'

const mermaid = vi.hoisted(() => ({ initialize: vi.fn(), render: vi.fn() }))
vi.mock('mermaid', () => ({ default: mermaid }))

const CODE = 'flowchart LR\n  f1["A.java"] --> f2["B.java"]'

async function mountDiagram(code = CODE) {
  const wrapper = mount(MermaidDiagram, { props: { code } })
  await flushPromises()
  return wrapper
}

function setClipboard(value) {
  Object.defineProperty(navigator, 'clipboard', { value, configurable: true })
}

describe('MermaidDiagram (F-21a)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mermaid.render.mockResolvedValue({ svg: '<svg data-test="graph"></svg>' })
  })

  afterEach(() => {
    setClipboard(undefined)
  })

  it('AC17 mermaid를 strict·startOnLoad false로 초기화해 원본을 그린다', async () => {
    const wrapper = await mountDiagram()

    expect(mermaid.initialize).toHaveBeenCalledWith(
      expect.objectContaining({ startOnLoad: false, securityLevel: 'strict' }),
    )
    expect(mermaid.render).toHaveBeenCalledTimes(1)
    expect(mermaid.render.mock.calls[0][1]).toBe(CODE)
    expect(wrapper.find('[data-role="diagram"] svg[data-test="graph"]').exists()).toBe(true)
    expect(wrapper.find('[data-message="render-failed"]').exists()).toBe(false)
  })

  it('AC17 원본이 바뀌면 다시 그린다', async () => {
    const wrapper = await mountDiagram()
    mermaid.render.mockResolvedValue({ svg: '<svg data-test="second"></svg>' })

    await wrapper.setProps({ code: 'flowchart LR\n  f3 --> f4' })
    await flushPromises()

    expect(mermaid.render).toHaveBeenCalledTimes(2)
    expect(mermaid.render.mock.calls[1][1]).toBe('flowchart LR\n  f3 --> f4')
    expect(wrapper.find('[data-role="diagram"] svg[data-test="second"]').exists()).toBe(true)
  })

  it('AC17 렌더링이 실패하면 그림 자리에 실패 문구를 보여 주고 원본 보기는 그대로 둔다', async () => {
    mermaid.render.mockRejectedValue(new Error('Parse error on line 2'))

    const wrapper = await mountDiagram()

    expect(wrapper.find('[data-message="render-failed"]').text()).toBe(
      '다이어그램을 그리지 못했습니다.',
    )
    expect(wrapper.find('[data-role="diagram"] svg').exists()).toBe(false)
    expect(wrapper.find('[data-role="source"]').text()).toBe(CODE)
  })

  it('AC16 "mermaid 코드 보기"에 원본을 pre로 보여 주고 복사하면 성공 문구를 보여 준다', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    setClipboard({ writeText })
    const wrapper = await mountDiagram()

    const source = wrapper.find('details[data-section="source"]')
    expect(source.find('summary').text()).toBe('mermaid 코드 보기')
    expect(source.find('pre[data-role="source"]').text()).toBe(CODE)

    await wrapper.find('[data-action="copy-source"]').trigger('click')
    await flushPromises()

    expect(writeText).toHaveBeenCalledWith(CODE)
    expect(wrapper.find('[data-role="copy-result"]').text()).toBe('복사했습니다.')
  })

  it('AC16 클립보드가 없거나 복사가 실패하면 직접 복사 안내를 보여 준다', async () => {
    const failed = '복사하지 못했습니다. 코드를 직접 선택해 복사해 주세요.'
    setClipboard(undefined)
    const wrapper = await mountDiagram()

    await wrapper.find('[data-action="copy-source"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-role="copy-result"]').text()).toBe(failed)

    setClipboard({ writeText: vi.fn().mockRejectedValue(new Error('denied')) })
    const other = await mountDiagram()
    await other.find('[data-action="copy-source"]').trigger('click')
    await flushPromises()
    expect(other.find('[data-role="copy-result"]').text()).toBe(failed)
  })
})
