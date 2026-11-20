import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import CommentForm from '@/components/community/CommentForm.vue'

const mountForm = (props) =>
  mount(CommentForm, { props: { modelValue: '', submitLabel: '등록', ...props } })

describe('CommentForm (F-18 AC12·AC13)', () => {
  it('AC12 공백뿐이면 제출 버튼이 비활성이고, 내용이 있으면 활성이다', async () => {
    const wrapper = mountForm({ modelValue: '  \n ' })
    expect(wrapper.find('[data-action="submit"]').attributes('disabled')).toBeDefined()

    await wrapper.setProps({ modelValue: '답' })
    expect(wrapper.find('[data-action="submit"]').attributes('disabled')).toBeUndefined()
    expect(wrapper.find('[data-action="submit"]').text()).toBe('등록')
  })

  it('AC12 입력은 500자까지이고 아래에 n/500을 보여 준다', async () => {
    const wrapper = mountForm({ modelValue: '가나다' })

    expect(wrapper.find('textarea').attributes('maxlength')).toBe('500')
    expect(wrapper.find('[data-role="counter"]').text()).toBe('3/500')

    await wrapper.find('textarea').setValue('abcd')
    expect(wrapper.emitted('update:modelValue').at(-1)).toEqual(['abcd'])
  })

  it('AC12 저장 중이면 버튼이 비활성이고 제출해도 submit을 보내지 않는다', async () => {
    const wrapper = mountForm({ modelValue: '답', saving: true })

    expect(wrapper.find('[data-action="submit"]').attributes('disabled')).toBeDefined()
    await wrapper.find('form').trigger('submit')
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('AC12 제출하면 submit을 보내고, 필드 오류는 입력창 아래에 보여 준다', async () => {
    const wrapper = mountForm({
      modelValue: '답',
      fieldError: '댓글은 500자 이하로 입력해 주세요.',
    })

    await wrapper.find('form').trigger('submit')
    expect(wrapper.emitted('submit')).toHaveLength(1)
    expect(wrapper.find('[data-role="field-error"]').text()).toBe(
      '댓글은 500자 이하로 입력해 주세요.',
    )
  })

  it('AC13 cancellable이면 "취소"가 보이고 누르면 cancel을 보낸다. 아니면 취소가 없다', async () => {
    const plain = mountForm({ modelValue: '답' })
    expect(plain.find('[data-action="cancel"]').exists()).toBe(false)

    const editing = mountForm({ modelValue: '답', submitLabel: '저장', cancellable: true })
    await editing.find('[data-action="cancel"]').trigger('click')
    expect(editing.emitted('cancel')).toHaveLength(1)
    expect(editing.emitted('submit')).toBeUndefined()
  })
})
