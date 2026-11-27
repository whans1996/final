import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import AsyncState from '@/components/common/AsyncState.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const mountState = (props, slots = {}) =>
  mount(AsyncState, { props, slots: { default: '<p data-content>본문</p>', ...slots } })

// F-29a AC9: 로딩·에러·빈 상태를 한 컴포넌트로 (우선순위 loading > error > empty)
describe('AsyncState (F-29a AC9)', () => {
  it('AC9 loading이면 #skeleton 슬롯을 보인다', () => {
    const wrapper = mountState({ loading: true }, { skeleton: '<div data-skeleton>뼈대</div>' })
    expect(wrapper.find('[data-skeleton]').exists()).toBe(true)
    expect(wrapper.find('[data-content]').exists()).toBe(false)
  })

  it('AC9 skeleton 슬롯이 없으면 스피너와 "불러오는 중…"을 보인다', () => {
    const wrapper = mountState({ loading: true })
    expect(wrapper.find('[data-spinner]').exists()).toBe(true)
    expect(wrapper.text()).toContain('불러오는 중…')
    expect(wrapper.find('[role="status"]').exists()).toBe(true)
  })

  it('AC9 error면 메시지와 다시 시도 버튼을 보이고 누르면 retry를 내보낸다', async () => {
    const wrapper = mountState({ error: '목록을 불러오지 못했습니다.' })
    expect(wrapper.find('[role="alert"]').text()).toContain('목록을 불러오지 못했습니다.')
    const retry = wrapper.findAll('button').find((b) => b.text() === '다시 시도')
    await retry.trigger('click')
    expect(wrapper.emitted('retry')).toHaveLength(1)
    expect(wrapper.find('[data-content]').exists()).toBe(false)
  })

  it('AC9 empty면 EmptyState와 같은 문구와 #empty-action 슬롯을 보인다', () => {
    const wrapper = mountState(
      { empty: true, emptyMessage: '아직 찜한 카드가 없습니다.' },
      { 'empty-action': '<a href="/concepts" data-action="go">카드 둘러보기</a>' },
    )
    const empty = wrapper.findComponent(EmptyState)
    expect(empty.exists()).toBe(true)
    expect(empty.props('message')).toBe('아직 찜한 카드가 없습니다.')
    expect(wrapper.find('a[data-action="go"]').exists()).toBe(true)
    expect(wrapper.find('[data-content]').exists()).toBe(false)
  })

  it('AC9 아무 상태도 아니면 기본 슬롯을 보인다', () => {
    const wrapper = mountState({})
    expect(wrapper.find('[data-content]').exists()).toBe(true)
  })

  it('AC9 우선순위는 loading > error > empty이다', () => {
    const all = mountState({ loading: true, error: '실패', empty: true })
    expect(all.text()).toContain('불러오는 중…')
    expect(all.find('[role="alert"]').exists()).toBe(false)

    const errorAndEmpty = mountState({ error: '실패', empty: true })
    expect(errorAndEmpty.find('[role="alert"]').exists()).toBe(true)
    expect(errorAndEmpty.findComponent(EmptyState).exists()).toBe(false)
  })
})
