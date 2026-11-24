import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import AdminChallengeForm from '@/components/challenge/admin/AdminChallengeForm.vue'

const EMPTY = {
  title: '',
  description: '',
  startDate: '',
  endDate: '',
  goalType: '',
  goalValue: '',
  image: null,
  removeImage: false,
}

/** v-model을 흉내 내 update:modelValue를 받으면 props를 바꾼다 */
function mountForm(props = {}) {
  const wrapper = mount(AdminChallengeForm, {
    props: {
      modelValue: { ...EMPTY },
      'onUpdate:modelValue': (value) => wrapper.setProps({ modelValue: value }),
      ...props,
    },
  })
  return wrapper
}

async function choose(wrapper, file) {
  const input = wrapper.find('input[name="image"]')
  Object.defineProperty(input.element, 'files', { value: [file], configurable: true })
  await input.trigger('change')
}

const png = () => new File(['png'], 'cover.png', { type: 'image/png' })

describe('AdminChallengeForm (F-24 AC13)', () => {
  let created
  beforeEach(() => {
    created = 0
    URL.createObjectURL = vi.fn(() => `blob:preview-${++created}`)
    URL.revokeObjectURL = vi.fn()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('AC13 입력하면 바뀐 값 전체를 올린다', async () => {
    const wrapper = mountForm()

    await wrapper.find('[name="title"]').setValue('12월 챌린지')
    await wrapper.find('[name="goalType"]').setValue('QUIZ_COUNT')

    expect(wrapper.props('modelValue')).toMatchObject({
      title: '12월 챌린지',
      goalType: 'QUIZ_COUNT',
    })
  })

  it('AC13 기존 이미지를 보여 주고, 새 파일을 고르면 미리보기로 바꾸며 이전 미리보기 주소를 해제한다', async () => {
    const wrapper = mountForm({ currentImageUrl: '/uploads/challenges/old.png' })
    expect(wrapper.find('[data-testid="image-preview"]').attributes('src')).toBe(
      '/uploads/challenges/old.png',
    )

    await choose(wrapper, png())
    await nextTick()
    expect(wrapper.props('modelValue')).toMatchObject({ removeImage: false })
    expect(wrapper.props('modelValue').image.name).toBe('cover.png')
    expect(wrapper.find('[data-testid="image-preview"]').attributes('src')).toBe('blob:preview-1')

    await choose(wrapper, new File(['png2'], 'second.png', { type: 'image/png' }))
    await nextTick()
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:preview-1')
    expect(wrapper.find('[data-testid="image-preview"]').attributes('src')).toBe('blob:preview-2')

    wrapper.unmount()
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:preview-2')
  })

  it('AC13 이미지를 제거하면 기존 이미지가 있을 때 removeImage를 켜고 미리보기를 숨긴다', async () => {
    const wrapper = mountForm({ currentImageUrl: '/uploads/challenges/old.png' })

    await wrapper.find('[data-action="remove-image"]').trigger('click')

    expect(wrapper.props('modelValue')).toMatchObject({ image: null, removeImage: true })
    expect(wrapper.find('[data-testid="image-preview"]').exists()).toBe(false)
    expect(wrapper.find('[data-action="remove-image"]').exists()).toBe(false)
  })

  it('AC13 새 챌린지에서 고른 파일을 제거하면 이미지 없이 돌아간다', async () => {
    const wrapper = mountForm()
    await choose(wrapper, png())

    await wrapper.find('[data-action="remove-image"]').trigger('click')

    expect(wrapper.props('modelValue')).toMatchObject({ image: null, removeImage: false })
    expect(wrapper.find('[data-testid="image-preview"]').exists()).toBe(false)
  })

  it('AC13 잠금이면 목표 유형·목표값·시작일을 비활성으로 두고 안내를 보여 준다', () => {
    const wrapper = mountForm({ locked: true })

    expect(wrapper.find('[name="goalType"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('[name="goalValue"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('[name="startDate"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('[name="endDate"]').attributes('disabled')).toBeUndefined()
    expect(wrapper.find('[data-testid="lock-notice"]').text()).toBe(
      '이미 시작한 챌린지는 목표와 시작일을 바꿀 수 없습니다.',
    )
  })

  it('AC13 errors의 사유를 필드 아래에 보여 준다', () => {
    const wrapper = mountForm({ errors: { endDate: '종료일 사유', image: '이미지 사유' } })

    expect(wrapper.find('[data-error-for="endDate"]').text()).toBe('종료일 사유')
    expect(wrapper.find('[data-error-for="image"]').text()).toBe('이미지 사유')
    expect(wrapper.find('[name="endDate"]').attributes('aria-invalid')).toBe('true')
  })
})
