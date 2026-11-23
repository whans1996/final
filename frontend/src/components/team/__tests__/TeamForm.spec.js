import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import TeamForm from '@/components/team/TeamForm.vue'

function mountForm(props = {}) {
  return mount(TeamForm, { props: { submitLabel: '만들기', ...props } })
}

describe('TeamForm (F-20a AC13·AC15)', () => {
  it('AC13 이름 30자·소개 200자에서 더 입력되지 않는다', () => {
    const wrapper = mountForm()

    expect(wrapper.find('input[name="name"]').attributes('maxlength')).toBe('30')
    expect(wrapper.find('textarea[name="description"]').attributes('maxlength')).toBe('200')
  })

  it('AC13 이름이 공백뿐이거나 요청 중이면 버튼이 비활성이다', async () => {
    const wrapper = mountForm()
    const submit = () => wrapper.find('button[type="submit"]')

    expect(submit().attributes('disabled')).toBeDefined()
    await wrapper.find('input[name="name"]').setValue('  ')
    expect(submit().attributes('disabled')).toBeDefined()
    await wrapper.find('input[name="name"]').setValue('팀')
    expect(submit().attributes('disabled')).toBeUndefined()
    await wrapper.setProps({ busy: true })
    expect(submit().attributes('disabled')).toBeDefined()
  })

  it('AC15 초기값을 채우고 제출하면 이름·소개를 내보낸다', async () => {
    const wrapper = mountForm({
      submitLabel: '저장',
      initial: { name: '스프링 스터디', description: null },
    })

    expect(wrapper.find('input[name="name"]').element.value).toBe('스프링 스터디')
    expect(wrapper.find('textarea[name="description"]').element.value).toBe('')
    await wrapper.find('textarea[name="description"]').setValue('소개')
    await wrapper.find('form').trigger('submit')

    expect(wrapper.emitted('submit')[0]).toEqual([{ name: '스프링 스터디', description: '소개' }])
  })

  it('AC13 필드 에러와 폼 에러를 각 위치에 보여 준다', () => {
    const wrapper = mountForm({
      fieldErrors: {
        name: '팀 이름을 입력해 주세요.',
        description: '소개는 200자 이하로 입력해 주세요.',
      },
      formError: '더 이상 팀에 속할 수 없습니다. (최대 5개)',
    })

    expect(wrapper.find('[data-error="name"]').text()).toBe('팀 이름을 입력해 주세요.')
    expect(wrapper.find('[data-error="description"]').text()).toBe(
      '소개는 200자 이하로 입력해 주세요.',
    )
    expect(wrapper.find('[data-error="form"]').text()).toBe(
      '더 이상 팀에 속할 수 없습니다. (최대 5개)',
    )
  })
})
