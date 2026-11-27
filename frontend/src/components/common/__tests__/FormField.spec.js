import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import FormField from '@/components/common/FormField.vue'

// 기본 슬롯이 받은 id·aria 속성을 입력에 단다
const slot = `<template #default="{ id, describedBy, invalid }">
  <input :id="id" :aria-describedby="describedBy" :aria-invalid="invalid" />
</template>`

const mountField = (props) => mount(FormField, { props, slots: { default: slot } })

// F-29a AC8: 라벨·에러·힌트가 붙는 입력 칸
describe('FormField (F-29a AC8)', () => {
  it('AC8 label과 입력이 for/id로 연결된다', () => {
    const wrapper = mountField({ label: '이메일' })
    const label = wrapper.find('label')
    const input = wrapper.find('input')
    expect(label.text()).toBe('이메일')
    expect(input.attributes('id')).toBeTruthy()
    expect(label.attributes('for')).toBe(input.attributes('id'))
  })

  it('AC8 id를 주면 그 id를 쓴다', () => {
    const wrapper = mountField({ label: '닉네임', id: 'nickname' })
    expect(wrapper.find('label').attributes('for')).toBe('nickname')
    expect(wrapper.find('input').attributes('id')).toBe('nickname')
  })

  it('AC8 error가 있으면 빨간 문구와 aria-invalid·aria-describedby가 붙고 hint는 숨는다', () => {
    const wrapper = mountField({
      label: '이메일',
      error: '이메일 형식이 아닙니다.',
      hint: '회사 메일',
    })
    const input = wrapper.find('input')
    const message = wrapper.find('[data-error]')
    expect(message.text()).toBe('이메일 형식이 아닙니다.')
    expect(message.classes()).toContain('text-danger')
    expect(input.attributes('aria-invalid')).toBe('true')
    expect(input.attributes('aria-describedby')).toBe(message.attributes('id'))
    expect(wrapper.text()).not.toContain('회사 메일')
  })

  it('AC8 error가 없으면 hint를 보이고 aria-invalid가 없다', () => {
    const wrapper = mountField({ label: '이메일', hint: '로그인에 씁니다.' })
    const input = wrapper.find('input')
    const hint = wrapper.find('[data-hint]')
    expect(hint.text()).toBe('로그인에 씁니다.')
    expect(input.attributes('aria-invalid')).toBeUndefined()
    expect(input.attributes('aria-describedby')).toBe(hint.attributes('id'))
  })

  it('AC8 required면 라벨에 필수 표시가 붙는다', () => {
    const wrapper = mountField({ label: '비밀번호', required: true })
    expect(wrapper.find('label [data-required]').exists()).toBe(true)
  })
})
