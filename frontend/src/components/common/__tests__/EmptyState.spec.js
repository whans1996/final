import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import EmptyState from '@/components/common/EmptyState.vue'

// F-00b AC11: 목록 화면 공용 빈 상태
describe('EmptyState (F-00b AC11)', () => {
  it('AC11 문구와 action 슬롯을 보인다', () => {
    const wrapper = mount(EmptyState, {
      props: { message: '아직 찜한 카드가 없습니다.' },
      slots: { action: '<a href="/concepts" data-action="go">카드 둘러보기</a>' },
    })

    expect(wrapper.text()).toContain('아직 찜한 카드가 없습니다.')
    expect(wrapper.find('a[data-action="go"]').text()).toBe('카드 둘러보기')
  })

  it('AC11 action 슬롯 없이도 문구만 보인다', () => {
    const wrapper = mount(EmptyState, { props: { message: '결과가 없습니다.' } })

    expect(wrapper.text()).toBe('결과가 없습니다.')
  })
})
