import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import AdminConceptForm from '@/components/concept/admin/AdminConceptForm.vue'

const VALUES = {
  name: '낙관적 락',
  category: 'spring',
  difficulty: 'SENIOR',
  summary: '버전으로 충돌을 확인한다.',
  body: '## 핵심\n- 버전 컬럼',
  exampleCode: 'UPDATE t SET v = v + 1',
  referenceUrl: 'https://docs.spring.io',
}

function mountForm(props = {}) {
  return mount(AdminConceptForm, { props: { modelValue: { ...VALUES }, ...props } })
}

const field = (wrapper, name) => wrapper.find(`[name="${name}"]`)

describe('AdminConceptForm (F-13 AC14·AC15)', () => {
  it('AC14 필드 7개에 값을 채우고, 입력하면 바뀐 값 전체를 update:modelValue로 올린다', async () => {
    const wrapper = mountForm()

    expect(field(wrapper, 'name').element.value).toBe('낙관적 락')
    expect(field(wrapper, 'category').element.value).toBe('spring')
    expect(field(wrapper, 'difficulty').element.value).toBe('SENIOR')
    expect(field(wrapper, 'summary').element.value).toBe('버전으로 충돌을 확인한다.')
    expect(field(wrapper, 'body').element.value).toBe('## 핵심\n- 버전 컬럼')
    expect(field(wrapper, 'exampleCode').element.value).toBe('UPDATE t SET v = v + 1')
    expect(field(wrapper, 'referenceUrl').element.value).toBe('https://docs.spring.io')

    await field(wrapper, 'summary').setValue('새 요약')
    expect(wrapper.emitted('update:modelValue').at(-1)[0]).toEqual({
      ...VALUES,
      summary: '새 요약',
    })
  })

  it('AC14 카테고리·난이도 선택지는 6종·3종이고 선택 전에는 빈 값이다', () => {
    const wrapper = mountForm({ modelValue: { ...VALUES, category: '', difficulty: '' } })

    const values = (name) => wrapper.findAll(`[name="${name}"] option`).map((o) => o.element.value)
    expect(values('category')).toEqual(['', 'java', 'spring', 'web', 'security', 'mybatis', 'vue'])
    expect(values('difficulty')).toEqual(['', 'BEGINNER', 'JUNIOR', 'SENIOR'])
  })

  it('AC14 본문을 MarkdownBody로 미리 보여 준다', () => {
    const wrapper = mountForm()

    const preview = wrapper.find('[data-testid="body-preview"]')
    expect(preview.find('h3').text()).toBe('핵심')
    expect(preview.find('li').text()).toBe('버전 컬럼')
  })

  it('AC14 편집/미리보기 탭으로 보이는 칸을 바꾼다 (좁은 화면용)', async () => {
    const wrapper = mountForm()

    const editor = wrapper.find('.editor')
    expect(editor.attributes('data-pane')).toBe('edit')
    await wrapper.find('[data-pane-tab="preview"]').trigger('click')
    expect(editor.attributes('data-pane')).toBe('preview')
    expect(wrapper.find('[data-pane-tab="preview"]').attributes('aria-selected')).toBe('true')
  })

  it('AC14 nameReadonly면 이름 칸을 읽기 전용으로 둔다', () => {
    expect(field(mountForm(), 'name').attributes('readonly')).toBeUndefined()
    expect(field(mountForm({ nameReadonly: true }), 'name').attributes('readonly')).toBeDefined()
  })

  it('AC15 errors의 필드별 사유를 해당 필드 아래에 보여 준다', () => {
    const wrapper = mountForm({
      errors: { name: '같은 이름의 개념 카드가 이미 있습니다.', referenceUrl: 'http(s) 주소만' },
    })

    expect(wrapper.find('[data-error-for="name"]').text()).toBe(
      '같은 이름의 개념 카드가 이미 있습니다.',
    )
    expect(wrapper.find('[data-error-for="referenceUrl"]').text()).toBe('http(s) 주소만')
    expect(wrapper.find('[data-error-for="summary"]').exists()).toBe(false)
    expect(field(wrapper, 'name').attributes('aria-invalid')).toBe('true')
  })
})
