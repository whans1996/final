import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import AttachmentPicker from '@/components/community/AttachmentPicker.vue'
import * as analysesApi from '@/api/analyses'
import * as conceptsApi from '@/api/concepts'
import { apiError } from '@/views/__tests__/viewTestUtils'

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
    isPublic: true,
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

  it('AC17 #206 비공개 분석은 "(비공개)"를 붙여 고를 수 없게 두고 공개로 바꾸라고 안내한다', async () => {
    analysesApi.fetchAnalyses.mockResolvedValue({
      items: [analysis(), analysis({ id: 18, title: '비밀 과제', isPublic: false })],
      page: 1,
      size: 100,
      total: 2,
    })
    const wrapper = mountPicker()

    await chooseMode(wrapper, 'analysis')

    const option = wrapper.find('select[name="analysisId"] option[value="18"]')
    expect(option.text()).toBe('비밀 과제 (비공개)')
    expect(option.element.disabled).toBe(true)
    expect(wrapper.find('[data-role="private-hint"]').text()).toBe(
      '비공개 분석은 첨부할 수 없습니다. 분석 화면에서 공개로 바꾸면 첨부할 수 있습니다.',
    )
    await wrapper.find('select[name="analysisId"]').setValue('18')
    expect(emitted(wrapper)).toBeUndefined()
  })

  it('AC17 #206 비공개 분석이 없으면 안내를 보여 주지 않는다', async () => {
    analysesApi.fetchAnalyses.mockResolvedValue({
      items: [analysis()],
      page: 1,
      size: 100,
      total: 1,
    })
    const wrapper = mountPicker()

    await chooseMode(wrapper, 'analysis')

    expect(wrapper.find('[data-role="private-hint"]').exists()).toBe(false)
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

  it('AC17 #172 Enter로 연달아 찾으면 늦게 온 이전 결과가 마지막 검색 결과를 덮지 않는다', async () => {
    const pending = []
    conceptsApi.fetchConcepts.mockImplementation(
      () => new Promise((resolve, reject) => pending.push({ resolve, reject })),
    )
    const wrapper = mountPicker()
    await chooseMode(wrapper, 'concept')
    const input = wrapper.find('input[name="conceptKeyword"]')

    await input.setValue('jwt')
    await input.trigger('keydown', { key: 'Enter' })
    await input.setValue('spring')
    await input.trigger('keydown', { key: 'Enter' })
    expect(pending).toHaveLength(2)

    pending[1].resolve({ items: [concept({ id: 20, name: 'Spring Bean' })], total: 1 })
    await flushPromises()
    pending[0].resolve({ items: [concept()], total: 1 })
    await flushPromises()

    const names = wrapper.findAll('[data-concept-option]').map((o) => o.text())
    expect(names).toHaveLength(1)
    expect(names[0]).toContain('Spring Bean')
    expect(wrapper.find('[data-action="search-concepts"]').element.disabled).toBe(false)
  })

  it('AC17 #172 늦게 실패한 이전 검색은 마지막 검색 결과와 검색 중 상태를 바꾸지 않는다', async () => {
    const pending = []
    conceptsApi.fetchConcepts.mockImplementation(
      () => new Promise((resolve, reject) => pending.push({ resolve, reject })),
    )
    const wrapper = mountPicker()
    await chooseMode(wrapper, 'concept')
    const input = wrapper.find('input[name="conceptKeyword"]')

    await input.setValue('jwt')
    await input.trigger('keydown', { key: 'Enter' })
    await input.setValue('spring')
    await input.trigger('keydown', { key: 'Enter' })

    pending[0].reject(new Error('network'))
    await flushPromises()
    expect(wrapper.find('[data-action="search-concepts"]').element.disabled).toBe(true)

    pending[1].resolve({ items: [concept({ id: 20, name: 'Spring Bean' })], total: 1 })
    await flushPromises()
    expect(wrapper.find('.error').exists()).toBe(false)
    expect(wrapper.findAll('[data-concept-option]')).toHaveLength(1)
    expect(wrapper.find('[data-action="search-concepts"]').element.disabled).toBe(false)
  })

  it('AC17 #172 마지막 검색이 실패하면 늦게 성공한 이전 결과가 실패 안내를 덮지 않는다', async () => {
    const pending = []
    conceptsApi.fetchConcepts.mockImplementation(
      () => new Promise((resolve, reject) => pending.push({ resolve, reject })),
    )
    const wrapper = mountPicker()
    await chooseMode(wrapper, 'concept')
    const input = wrapper.find('input[name="conceptKeyword"]')

    await input.setValue('jwt')
    await input.trigger('keydown', { key: 'Enter' })
    await input.setValue('spring')
    await input.trigger('keydown', { key: 'Enter' })

    pending[1].reject(apiError(500, { code: 'COMMON_500', message: '잠시 후 다시 시도해 주세요.' }))
    await flushPromises()
    pending[0].resolve({ items: [concept()], total: 1 })
    await flushPromises()

    expect(wrapper.find('.error').text()).toBe('잠시 후 다시 시도해 주세요.')
    expect(wrapper.findAll('[data-concept-option]')).toHaveLength(0)
    expect(wrapper.find('[data-action="search-concepts"]').element.disabled).toBe(false)
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
