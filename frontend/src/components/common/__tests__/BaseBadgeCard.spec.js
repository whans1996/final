import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import BaseBadge from '@/components/common/BaseBadge.vue'
import BaseCard from '@/components/common/BaseCard.vue'

// F-29a AC10: 배지 tone별 상태 색 쌍, 카드 슬롯·interactive
// 다크 쌍은 토큰(.dark의 --vn-*)이 바꾸므로 클래스는 같다. 대비는 tokens.spec.js AC13이 본다.
describe('BaseBadge (F-29a AC10)', () => {
  it.each([
    ['neutral', 'text-text-muted', 'bg-bg'],
    ['primary', 'text-primary', 'bg-primary-soft'],
    ['success', 'text-success', 'bg-success-soft'],
    ['warning', 'text-warning', 'bg-warning-soft'],
    ['info', 'text-info', 'bg-info-soft'],
    ['danger', 'text-danger', 'bg-danger-soft'],
    ['highlight', 'text-text', 'bg-highlight'],
  ])('AC10 tone %s이면 글자 %s·바탕 %s', (tone, text, bg) => {
    const badge = mount(BaseBadge, { props: { tone }, slots: { default: '새 카드' } })
    expect(badge.attributes('data-tone')).toBe(tone)
    expect(badge.classes()).toEqual(expect.arrayContaining([text, bg]))
    expect(badge.text()).toBe('새 카드')
  })

  it('AC10 tone 기본값은 neutral이다', () => {
    expect(mount(BaseBadge).attributes('data-tone')).toBe('neutral')
  })
})

describe('BaseCard (F-29a AC10)', () => {
  it('AC10 #header·기본·#footer 슬롯을 순서대로 그린다', () => {
    const card = mount(BaseCard, {
      slots: { header: '<h3>제목</h3>', default: '<p>본문</p>', footer: '<small>꼬리</small>' },
    })
    expect(card.element.tagName).toBe('DIV')
    expect(card.text()).toBe('제목본문꼬리')
    expect(card.classes()).toEqual(expect.arrayContaining(['bg-surface', 'border-border']))
  })

  it('AC10 as로 태그를 바꾼다', () => {
    expect(mount(BaseCard, { props: { as: 'article' } }).element.tagName).toBe('ARTICLE')
  })

  it('AC10 interactive면 hover·focus 링 클래스가 붙는다', () => {
    const plain = mount(BaseCard)
    const card = mount(BaseCard, { props: { interactive: true } })
    expect(plain.classes()).not.toContain('hover:border-primary')
    expect(card.classes()).toEqual(
      expect.arrayContaining([
        'hover:border-primary',
        'focus-within:ring-2',
        'focus-within:ring-primary',
      ]),
    )
  })
})
