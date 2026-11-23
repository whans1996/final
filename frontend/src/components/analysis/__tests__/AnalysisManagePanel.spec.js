import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import AnalysisManagePanel from '@/components/analysis/AnalysisManagePanel.vue'
import * as analysesApi from '@/api/analyses'
import { apiError } from '@/views/__tests__/viewTestUtils'

vi.mock('@/api/analyses')

function detail(partial) {
  return {
    id: 42,
    title: 'todo-app',
    memo: '메모',
    mine: true,
    status: 'DONE',
    isPublic: false,
    ...partial,
  }
}

function mountPanel(partial) {
  return mount(AnalysisManagePanel, { props: { detail: detail(partial) } })
}

describe('AnalysisManagePanel (F-19 AC13)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('AC13 수정 폼은 현재 값으로 시작하고 글자 수 카운터를 보여 준다', async () => {
    const wrapper = mountPanel()
    await wrapper.find('[data-action="edit"]').trigger('click')

    expect(wrapper.find('[data-field="title"]').element.value).toBe('todo-app')
    expect(wrapper.find('[data-field="memo"]').element.value).toBe('메모')
    expect(wrapper.find('[data-counter="title"]').text()).toBe('8/100')
    await wrapper.find('[data-field="memo"]').setValue('가나다')
    expect(wrapper.find('[data-counter="memo"]').text()).toBe('3/1000')
  })

  it('AC13 저장하면 updateAnalysis를 부르고 저장 중에는 버튼을 막으며, 성공하면 updated를 보낸다', async () => {
    let resolve
    analysesApi.updateAnalysis.mockReturnValue(new Promise((r) => (resolve = r)))
    const wrapper = mountPanel()
    await wrapper.find('[data-action="edit"]').trigger('click')
    await wrapper.find('[data-field="title"]').setValue('새 제목')
    await wrapper.find('[data-form="edit"]').trigger('submit')

    expect(analysesApi.updateAnalysis).toHaveBeenCalledWith(42, { title: '새 제목', memo: '메모' })
    expect(wrapper.find('[data-action="save"]').attributes('disabled')).toBeDefined()

    resolve(detail({ title: '새 제목' }))
    await flushPromises()

    expect(wrapper.emitted('updated')[0][0].title).toBe('새 제목')
    expect(wrapper.find('[data-form="edit"]').exists()).toBe(false)
  })

  it('AC13 저장이 실패하면 서버 message를 보여 준다', async () => {
    const message = '제목은 100자 이하로 입력하세요.'
    analysesApi.updateAnalysis.mockRejectedValue(apiError(400, { code: 'COMMON_400', message }))
    const wrapper = mountPanel()
    await wrapper.find('[data-action="edit"]').trigger('click')
    await wrapper.find('[data-form="edit"]').trigger('submit')
    await flushPromises()

    expect(wrapper.find('[data-error="save"]').text()).toBe(message)
    expect(wrapper.emitted('updated')).toBeUndefined()
  })

  it('AC13 공개 토글은 DONE일 때만 켜지고 안내 문구를 보여 준다', () => {
    expect(mountPanel().find('[data-action="visibility"]').attributes('disabled')).toBeUndefined()
    expect(mountPanel().find('[data-message="visibility-hint"]').text()).toBe(
      '공개하면 팔로워 피드와 게시글 첨부에서 보입니다.',
    )
    expect(
      mountPanel({ status: 'FAILED' }).find('[data-action="visibility"]').attributes('disabled'),
    ).toBeDefined()
  })

  it('AC13 공개로 바꾸면 updateVisibility를 부르고 updated를 보낸다', async () => {
    analysesApi.updateVisibility.mockResolvedValue({ id: 42, isPublic: true })
    const wrapper = mountPanel()
    await wrapper.find('[data-action="visibility"]').setValue(true)
    await flushPromises()

    expect(analysesApi.updateVisibility).toHaveBeenCalledWith(42, true)
    expect(wrapper.emitted('updated')[0][0]).toEqual({ isPublic: true })
    expect(wrapper.find('[data-action="visibility"]').element.checked).toBe(true)
  })

  it('AC13 공개 변경이 실패하면 이전 값으로 되돌리고 서버 message를 보여 준다', async () => {
    const message = '완료된 분석만 공개할 수 있습니다.'
    analysesApi.updateVisibility.mockRejectedValue(apiError(409, { code: 'ANALYSIS_006', message }))
    const wrapper = mountPanel()
    await wrapper.find('[data-action="visibility"]').setValue(true)
    await flushPromises()

    expect(wrapper.find('[data-action="visibility"]').element.checked).toBe(false)
    expect(wrapper.find('[data-error="visibility"]').text()).toBe(message)
  })

  it('AC13 삭제는 DONE·FAILED에서만 보인다', () => {
    expect(mountPanel().find('[data-action="delete"]').exists()).toBe(true)
    expect(mountPanel({ status: 'FAILED' }).find('[data-action="delete"]').exists()).toBe(true)
    expect(mountPanel({ status: 'EXPLAINING' }).find('[data-action="delete"]').exists()).toBe(false)
  })

  it('AC13 삭제 확인을 취소하면 부르지 않고, 확인하면 deleted를 보낸다', async () => {
    const confirm = vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValue(true)
    analysesApi.deleteAnalysis.mockResolvedValue(undefined)
    const wrapper = mountPanel()

    await wrapper.find('[data-action="delete"]').trigger('click')
    expect(confirm).toHaveBeenCalledWith('이 분석을 삭제할까요? 퀴즈 풀이 기록은 남습니다.')
    expect(analysesApi.deleteAnalysis).not.toHaveBeenCalled()

    await wrapper.find('[data-action="delete"]').trigger('click')
    await flushPromises()
    expect(analysesApi.deleteAnalysis).toHaveBeenCalledWith(42)
    expect(wrapper.emitted('deleted')).toHaveLength(1)
  })

  it('AC13 진행 중이라 삭제가 409면 서버 문구를 보여 준다', async () => {
    const message = '진행 중인 분석은 삭제할 수 없습니다. 분석이 끝난 뒤 다시 시도해 주세요.'
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    analysesApi.deleteAnalysis.mockRejectedValue(apiError(409, { code: 'ANALYSIS_005', message }))
    const wrapper = mountPanel({ status: 'FAILED' })

    await wrapper.find('[data-action="delete"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-error="delete"]').text()).toBe(message)
    expect(wrapper.emitted('deleted')).toBeUndefined()
  })
})
