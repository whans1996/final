import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import AttachmentPicker from '@/components/community/AttachmentPicker.vue'
import * as analysesApi from '@/api/analyses'
import * as conceptsApi from '@/api/concepts'

vi.mock('@/api/analyses')
vi.mock('@/api/concepts')

function analysis(partial) {
  return {
    id: 15,
    title: '쇼핑몰 과제',
    projectName: 'shop-backend',
    status: 'DONE',
    level: 'JUNIOR',
    progress: 100,
    isPublic: false,
    sourceType: 'ZIP',
    createdAt: '2026-11-20T10:00:00+09:00',
    ...partial,
  }
}

function concept(partial) {
  return {
    id: 12,
    name: 'JWT',
    category: 'security',
    difficulty: 'JUNIOR',
    summary: '서버가 서명한 토큰',
    wishCount: 0,
    avgRating: 0,
    ...partial,
  }
}

const mountPicker = (modelValue = null) => mount(AttachmentPicker, { props: { modelValue } })
const emitted = (wrapper) => wrapper.emitted('update:modelValue')?.at(-1)?.[0]
const chooseMode = async (wrapper, mode) => {
  await wrapper.find(`input[name="attachmentMode"][value="${mode}"]`).setValue(true)
  await flushPromises()
}

describe('AttachmentPicker (F-17 AC17)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('AC17 없음 / 내 분석 / 개념 카드 중 하나를 고르고 기본은 없음이다', () => {
    const wrapper = mountPicker()
    const modes = wrapper.findAll('input[name="attachmentMode"]')

    expect(modes.map((m) => m.element.value)).toEqual(['none', 'analysis', 'concept'])
    expect(modes[0].element.checked).toBe(true)
    expect(wrapper.text()).toContain('없음')
    expect(wrapper.text()).toContain('내 분석')
    expect(wrapper.text()).toContain('개념 카드')
  })

  it('AC17 내 분석은 최근 100개 중 DONE인 것만 선택 상자로 보여 주고 고르면 첨부로 알린다', async () => {
    analysesApi.fetchAnalyses.mockResolvedValue({
      items: [
        analysis(),
        analysis({ id: 16, status: 'EXPLAINING' }),
        analysis({ id: 17, title: null }),
      ],
      page: 1,
      size: 100,
      total: 3,
    })
    const wrapper = mountPicker()

    await chooseMode(wrapper, 'analysis')

    expect(analysesApi.fetchAnalyses).toHaveBeenCalledWith({ page: 1, size: 100 })
    const options = wrapper
      .findAll('select[name="analysisId"] option')
      .filter((o) => o.element.value)
    expect(options.map((o) => o.element.value)).toEqual(['15', '17'])
    await wrapper.find('select[name="analysisId"]').setValue('17')
    expect(emitted(wrapper)).toEqual({ type: 'ANALYSIS', id: 17, label: 'shop-backend' })
  })

  it('AC17 개념 카드는 검색어로 10개를 찾아 고른다', async () => {
    conceptsApi.fetchConcepts.mockResolvedValue({
      items: [concept(), concept({ id: 13, name: 'JWT 서명' })],
      page: 1,
      size: 10,
      total: 2,
    })
    const wrapper = mountPicker()

    await chooseMode(wrapper, 'concept')
    await wrapper.find('input[name="conceptKeyword"]').setValue('jwt')
    await wrapper.find('[data-action="search-concepts"]').trigger('click')
    await flushPromises()

    expect(conceptsApi.fetchConcepts).toHaveBeenCalledWith({ keyword: 'jwt', size: 10 })
    const options = wrapper.findAll('[data-concept-option]')
    expect(options).toHaveLength(2)
    await options[1].trigger('click')
    expect(emitted(wrapper)).toEqual({ type: 'CONCEPT', id: 13, label: 'JWT 서명' })
  })

  it('AC17 고른 첨부는 미리보기 칩으로 보이고 ✕를 누르면 해제한다', async () => {
    const wrapper = mountPicker({ type: 'CONCEPT', id: 12, label: 'JWT' })

    const chip = wrapper.find('[data-role="attachment-chip"]')
    expect(chip.text()).toContain('🃏')
    expect(chip.text()).toContain('JWT')
    await wrapper.find('[data-action="clear-attachment"]').trigger('click')
    expect(emitted(wrapper)).toBeNull()

    await wrapper.setProps({ modelValue: { type: 'ANALYSIS', id: 15, label: '쇼핑몰 과제' } })
    expect(wrapper.find('[data-role="attachment-chip"]').text()).toContain('📎')
  })

  it('AC17 없음을 고르면 첨부를 해제한다', async () => {
    const wrapper = mountPicker({ type: 'CONCEPT', id: 12, label: 'JWT' })

    await chooseMode(wrapper, 'none')

    expect(emitted(wrapper)).toBeNull()
  })
})
