import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ReviewForm from '@/components/concept/ReviewForm.vue'

const submitButton = (wrapper) => wrapper.find('[data-action="submit"]')

describe('ReviewForm (F-11 AC15·AC16)', () => {
  it('AC15 별을 고르지 않았거나 내용이 공백뿐이면 등록이 비활성이다', async () => {
    const wrapper = mount(ReviewForm)
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()

    await wrapper.find('textarea').setValue('좋아요')
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()

    await wrapper.find('[aria-label="별점 4점"]').trigger('click')
    expect(submitButton(wrapper).attributes('disabled')).toBeUndefined()

    await wrapper.find('textarea').setValue('   ')
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
  })

  it('AC15 입력창은 200자 제한이고 n/200을 보여 준다', async () => {
    const wrapper = mount(ReviewForm)
    const textarea = wrapper.find('textarea')

    expect(textarea.attributes('maxlength')).toBe('200')
    expect(wrapper.find('[data-role="count"]').text()).toBe('0/200')
    await textarea.setValue('가나다')
    expect(wrapper.find('[data-role="count"]').text()).toBe('3/200')
  })

  it('AC15 제출하면 별점과 앞뒤 공백을 뺀 내용을 올린다', async () => {
    const wrapper = mount(ReviewForm)
    await wrapper.find('[aria-label="별점 5점"]').trigger('click')
    await wrapper.find('textarea').setValue('  예제가 명확했어요 ')

    await wrapper.find('form').trigger('submit')

    expect(wrapper.emitted('submit')).toEqual([[{ rating: 5, content: '예제가 명확했어요' }]])
  })

  it('AC15 서버 details를 필드 아래에, message를 폼 아래에 보여 준다', () => {
    const wrapper = mount(ReviewForm, {
      props: {
        errors: { rating: '별점을 선택해 주세요.', content: '리뷰 내용을 입력해 주세요.' },
        error: '이미 이 카드에 리뷰를 남겼습니다.',
      },
    })

    expect(wrapper.find('[data-error="rating"]').text()).toBe('별점을 선택해 주세요.')
    expect(wrapper.find('[data-error="content"]').text()).toBe('리뷰 내용을 입력해 주세요.')
    expect(wrapper.find('[role="alert"]').text()).toBe('이미 이 카드에 리뷰를 남겼습니다.')
  })

  it('AC16 수정이면 기존 값이 채워지고 취소를 올린다', async () => {
    const wrapper = mount(ReviewForm, {
      props: {
        initial: { rating: 3, content: '처음 생각' },
        submitLabel: '저장',
        cancellable: true,
      },
    })

    expect(wrapper.find('textarea').element.value).toBe('처음 생각')
    expect(wrapper.find('[aria-label="별점 3점"]').attributes('aria-pressed')).toBe('true')
    expect(submitButton(wrapper).text()).toBe('저장')

    await wrapper.find('[data-action="cancel"]').trigger('click')
    expect(wrapper.emitted('cancel')).toHaveLength(1)
  })
})
