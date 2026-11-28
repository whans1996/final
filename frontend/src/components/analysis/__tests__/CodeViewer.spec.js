import { afterEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import CodeViewer from '@/components/analysis/CodeViewer.vue'

const CONTENT = Array.from({ length: 40 }, (_, i) => `line ${i + 1}`).join('\n') + '\n'

function marked(wrapper) {
  return wrapper.findAll('[data-highlighted="true"]').map((l) => l.attributes('data-line'))
}

describe('CodeViewer (F-06 6절, I-1a AC15)', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('줄 번호를 붙이고 파일 끝 빈 줄은 보이지 않는다', () => {
    const wrapper = mount(CodeViewer, { props: { content: 'a\nb\n' } })

    expect(wrapper.findAll('[data-line]').map((l) => l.attributes('data-line'))).toEqual(['1', '2'])
  })

  it('AC15 범위를 강조하고 첫 줄을 화면 가운데로 스크롤한다', async () => {
    const scroll = vi.fn()
    Element.prototype.scrollIntoView = scroll

    const wrapper = mount(CodeViewer, {
      props: { content: CONTENT, highlight: { start: 12, end: 14 } },
      attachTo: document.body,
    })
    await flushPromises()

    expect(marked(wrapper)).toEqual(['12', '13', '14'])
    expect(scroll).toHaveBeenCalledWith({ block: 'center' })
    expect(scroll.mock.contexts.at(-1).getAttribute('data-line')).toBe('12')
    wrapper.unmount()
  })

  it('AC15 한 줄이면 그 줄만 강조한다', () => {
    const wrapper = mount(CodeViewer, {
      props: { content: CONTENT, highlight: { start: 7, end: 7 } },
    })

    expect(marked(wrapper)).toEqual(['7'])
  })

  it('AC15 파일 줄 수를 넘거나 역순·0이면 강조하지 않는다', () => {
    for (const highlight of [
      { start: 39, end: 41 },
      { start: 30, end: 12 },
      { start: 0, end: 2 },
      null,
    ]) {
      const wrapper = mount(CodeViewer, { props: { content: CONTENT, highlight } })
      expect(marked(wrapper)).toEqual([])
      expect(wrapper.findAll('[data-line]')).toHaveLength(40)
    }
  })

  it('AC15 범위가 바뀌면 새 범위를 강조한다', async () => {
    const wrapper = mount(CodeViewer, {
      props: { content: CONTENT, highlight: { start: 1, end: 2 } },
    })

    await wrapper.setProps({ highlight: { start: 5, end: 5 } })

    expect(marked(wrapper)).toEqual(['5'])
  })
})
