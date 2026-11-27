import { afterEach, describe, expect, it } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import BaseTabs from '@/components/common/BaseTabs.vue'

const items = [
  { value: 'easy', label: '쉬움' },
  { value: 'normal', label: '보통' },
  { value: 'hard', label: '어려움' },
]

let wrapper
function mountTabs(modelValue = 'easy') {
  wrapper = mount(BaseTabs, {
    attachTo: document.body,
    props: {
      items,
      modelValue,
      'onUpdate:modelValue': (v) => wrapper.setProps({ modelValue: v }),
    },
    slots: {
      easy: '<p>쉬운 설명</p>',
      normal: '<p>보통 설명</p>',
      hard: '<p>어려운 설명</p>',
    },
  })
  return wrapper
}

afterEach(() => wrapper?.unmount())

const tabs = () => wrapper.findAll('[role="tab"]')
const selected = () => tabs().map((t) => t.attributes('aria-selected'))

// F-29a AC11: Reka UI Tabs를 감싼 공통 탭
describe('BaseTabs (F-29a AC11)', () => {
  it('AC11 tablist·tab·tabpanel 역할과 aria-selected가 맞다', () => {
    mountTabs('normal')
    expect(wrapper.find('[role="tablist"]').exists()).toBe(true)
    expect(tabs().map((t) => t.text())).toEqual(['쉬움', '보통', '어려움'])
    expect(selected()).toEqual(['false', 'true', 'false'])
    const panel = wrapper.find('[role="tabpanel"][data-state="active"]')
    expect(panel.text()).toBe('보통 설명')
    expect(panel.attributes('aria-labelledby')).toBe(tabs()[1].attributes('id'))
  })

  it('AC11 탭을 누르면 선택이 바뀌고 update:modelValue가 나간다', async () => {
    mountTabs('easy')
    await tabs()[2].trigger('mousedown', { button: 0 })
    expect(wrapper.emitted('update:modelValue')).toEqual([['hard']])
    expect(selected()).toEqual(['false', 'false', 'true'])
    expect(wrapper.find('[role="tabpanel"][data-state="active"]').text()).toBe('어려운 설명')
  })

  it('AC11 →/← 키로 옆 탭으로 옮겨 선택한다', async () => {
    mountTabs('easy')
    await flushPromises() // Reka가 마운트 뒤에 탭 항목을 등록한다
    tabs()[0].element.focus()
    await tabs()[0].trigger('keydown', { key: 'ArrowRight' })
    await flushPromises()
    expect(selected()).toEqual(['false', 'true', 'false'])
    await tabs()[1].trigger('keydown', { key: 'ArrowLeft' })
    await flushPromises()
    expect(selected()).toEqual(['true', 'false', 'false'])
    expect(wrapper.emitted('update:modelValue')).toEqual([['normal'], ['easy']])
  })
})
