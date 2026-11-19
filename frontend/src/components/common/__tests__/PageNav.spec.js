import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PageNav from '@/components/common/PageNav.vue'

const prev = (wrapper) => wrapper.find('button[data-action="prev"]')
const next = (wrapper) => wrapper.find('button[data-action="next"]')

// F-00b AC11: 목록 화면 공용 페이지 이동 (page는 1부터)
describe('PageNav (F-00b AC11)', () => {
  it('AC11 total이 size 이하면 숨는다', () => {
    expect(
      mount(PageNav, { props: { page: 1, size: 20, total: 20 } })
        .find('nav')
        .exists(),
    ).toBe(false)
    expect(
      mount(PageNav, { props: { page: 1, size: 20, total: 0 } })
        .find('nav')
        .exists(),
    ).toBe(false)
  })

  it('AC11 1페이지에서는 이전이, 마지막 페이지에서는 다음이 비활성이다', () => {
    const first = mount(PageNav, { props: { page: 1, size: 20, total: 41 } })
    expect(prev(first).attributes('disabled')).toBeDefined()
    expect(next(first).attributes('disabled')).toBeUndefined()
    expect(first.text()).toContain('1 / 3')

    const last = mount(PageNav, { props: { page: 3, size: 20, total: 41 } })
    expect(prev(last).attributes('disabled')).toBeUndefined()
    expect(next(last).attributes('disabled')).toBeDefined()
  })

  it('AC11 이전·다음을 누르면 change(page)를 emit한다', async () => {
    const wrapper = mount(PageNav, { props: { page: 2, size: 20, total: 41 } })

    await prev(wrapper).trigger('click')
    await next(wrapper).trigger('click')

    expect(wrapper.emitted('change')).toEqual([[1], [3]])
  })
})
