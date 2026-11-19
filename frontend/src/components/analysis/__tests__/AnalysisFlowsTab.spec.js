import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import AnalysisFlowsTab from '@/components/analysis/AnalysisFlowsTab.vue'
import * as analysesApi from '@/api/analyses'
import { apiError } from '@/views/__tests__/viewTestUtils'

vi.mock('@/api/analyses')

const apis = [
  { method: 'GET', path: '/api/todos', fileId: 101 },
  { method: 'POST', path: '/api/todos', fileId: 101 },
  { method: 'PATCH', path: '/api/todos/{id}', fileId: 101 },
  { method: 'DELETE', path: '/api/todos/{id}', fileId: 101 },
]

function flow(partial) {
  return {
    id: 7,
    method: 'POST',
    path: '/api/todos',
    status: 'PENDING',
    errorCode: null,
    errorMessage: null,
    summary: null,
    steps: null,
    createdAt: '2026-12-09T10:00:00+09:00',
    ...partial,
  }
}

async function mountTab(props = {}) {
  const wrapper = mount(AnalysisFlowsTab, { props: { analysisId: 42, apis, ...props } })
  await flushPromises()
  return wrapper
}

function checkbox(wrapper, api) {
  return wrapper.find(`input[data-api="${api}"]`)
}

function submitButton(wrapper) {
  return wrapper.find('[data-action="request-flows"]')
}

async function advance(ms) {
  await vi.advanceTimersByTimeAsync(ms)
  await flushPromises()
}

describe('AnalysisFlowsTab (F-07)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('AC14 흐름 목록을 불러와 API를 체크박스로 보여 주고 이미 있는 API는 상태 배지와 함께 막는다', async () => {
    analysesApi.fetchAnalysisFlows.mockResolvedValue([
      flow({
        id: 7,
        method: 'GET',
        path: '/api/todos',
        status: 'DONE',
        summary: '요약',
        steps: [],
      }),
      flow({ id: 8, method: 'PATCH', path: '/api/todos/{id}', status: 'FAILED' }),
    ])

    const wrapper = await mountTab()

    expect(analysesApi.fetchAnalysisFlows).toHaveBeenCalledWith(42)
    expect(wrapper.findAll('input[type="checkbox"]')).toHaveLength(4)
    expect(checkbox(wrapper, 'GET /api/todos').attributes('disabled')).toBeDefined()
    expect(checkbox(wrapper, 'PATCH /api/todos/{id}').attributes('disabled')).toBeDefined()
    expect(checkbox(wrapper, 'POST /api/todos').attributes('disabled')).toBeUndefined()
    const done = wrapper.find('[data-api-row="GET /api/todos"]')
    expect(done.find('.method').text()).toBe('GET')
    expect(done.text()).toContain('완료')
    expect(wrapper.find('[data-api-row="PATCH /api/todos/{id}"]').text()).toContain('실패')
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
  })

  it('AC14 남은 개수까지만 체크되고 버튼에 고른 개수를 보여 준다', async () => {
    analysesApi.fetchAnalysisFlows.mockResolvedValue([
      flow({
        id: 7,
        method: 'GET',
        path: '/api/todos',
        status: 'DONE',
        summary: '요약',
        steps: [],
      }),
    ])
    const wrapper = await mountTab()

    expect(wrapper.text()).toContain('2개 더 만들 수 있습니다')
    await checkbox(wrapper, 'POST /api/todos').setValue(true)
    await checkbox(wrapper, 'PATCH /api/todos/{id}').setValue(true)

    expect(checkbox(wrapper, 'DELETE /api/todos/{id}').attributes('disabled')).toBeDefined()
    expect(submitButton(wrapper).attributes('disabled')).toBeUndefined()
    expect(submitButton(wrapper).text()).toBe('실행 흐름 만들기 (2개)')
  })

  it('AC14 apis가 비면 안내 문구를 보여 준다', async () => {
    analysesApi.fetchAnalysisFlows.mockResolvedValue([])

    const wrapper = await mountTab({ apis: [] })

    expect(wrapper.text()).toContain('탐지된 API가 없어 실행 흐름을 만들 수 없습니다.')
    expect(wrapper.find('input[type="checkbox"]').exists()).toBe(false)
  })

  it('AC14 FAILED가 아닌 흐름이 3개면 한도 안내를 보여 주고 체크할 수 없다', async () => {
    analysesApi.fetchAnalysisFlows.mockResolvedValue([
      flow({ id: 1, method: 'GET', path: '/api/todos', status: 'DONE', summary: 's', steps: [] }),
      flow({ id: 2, method: 'POST', path: '/api/todos', status: 'DONE', summary: 's', steps: [] }),
      flow({
        id: 3,
        method: 'PATCH',
        path: '/api/todos/{id}',
        status: 'DONE',
        summary: 's',
        steps: [],
      }),
    ])

    const wrapper = await mountTab()

    expect(wrapper.text()).toContain('실행 흐름은 분석 하나에 3개까지 만들 수 있습니다.')
    expect(checkbox(wrapper, 'DELETE /api/todos/{id}').attributes('disabled')).toBeDefined()
  })

  it('AC15 요청 뒤 PENDING이 있으면 3초마다 다시 불러오고 모두 끝나면 멈춘다', async () => {
    analysesApi.fetchAnalysisFlows.mockResolvedValueOnce([])
    analysesApi.requestAnalysisFlows.mockResolvedValue([flow({ id: 7 })])
    const wrapper = await mountTab()

    await checkbox(wrapper, 'POST /api/todos').setValue(true)
    analysesApi.fetchAnalysisFlows.mockResolvedValueOnce([flow({ id: 7 })])
    analysesApi.fetchAnalysisFlows.mockResolvedValueOnce([
      flow({ id: 7, status: 'DONE', summary: '요약', steps: [] }),
    ])
    await submitButton(wrapper).trigger('click')
    await flushPromises()

    expect(analysesApi.requestAnalysisFlows).toHaveBeenCalledWith(42, [
      { method: 'POST', path: '/api/todos' },
    ])
    expect(wrapper.text()).toContain('실행 흐름을 만드는 중입니다…')
    const callsAfterRequest = analysesApi.fetchAnalysisFlows.mock.calls.length

    await advance(3000)
    expect(analysesApi.fetchAnalysisFlows.mock.calls.length).toBe(callsAfterRequest + 1)
    await advance(3000)
    expect(analysesApi.fetchAnalysisFlows.mock.calls.length).toBe(callsAfterRequest + 2)
    expect(wrapper.text()).toContain('요약')

    await advance(9000)
    expect(analysesApi.fetchAnalysisFlows.mock.calls.length).toBe(callsAfterRequest + 2)
  })

  it('AC15 처음부터 PENDING이 있으면 폴링하고 화면을 떠나면 멈춘다', async () => {
    analysesApi.fetchAnalysisFlows.mockResolvedValue([flow({ id: 7 })])
    const wrapper = await mountTab()

    await advance(3000)
    expect(analysesApi.fetchAnalysisFlows).toHaveBeenCalledTimes(2)

    wrapper.unmount()
    await advance(9000)
    expect(analysesApi.fetchAnalysisFlows).toHaveBeenCalledTimes(2)
  })

  it('AC15 요청 응답 전에 화면을 떠나면 응답이 와도 폴링을 시작하지 않는다', async () => {
    analysesApi.fetchAnalysisFlows.mockResolvedValue([])
    let resolveRequest
    analysesApi.requestAnalysisFlows.mockReturnValue(
      new Promise((resolve) => {
        resolveRequest = resolve
      }),
    )
    const wrapper = await mountTab()

    await checkbox(wrapper, 'POST /api/todos').setValue(true)
    await submitButton(wrapper).trigger('click')
    wrapper.unmount()
    resolveRequest([flow({ id: 7 })])
    await flushPromises()

    await advance(9000)
    expect(analysesApi.fetchAnalysisFlows).toHaveBeenCalledTimes(1)
  })

  it('AC16 FAILED 카드의 다시 만들기는 그 API 하나만 POST한다', async () => {
    analysesApi.fetchAnalysisFlows.mockResolvedValue([
      flow({
        id: 8,
        method: 'PATCH',
        path: '/api/todos/{id}',
        status: 'FAILED',
        errorCode: 'LLM_503',
        errorMessage: 'AI 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요.',
      }),
    ])
    analysesApi.requestAnalysisFlows.mockResolvedValue([
      flow({ id: 8, method: 'PATCH', path: '/api/todos/{id}' }),
    ])
    const wrapper = await mountTab()

    expect(wrapper.text()).toContain('AI 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요.')
    await wrapper.find('[data-action="retry-flow"]').trigger('click')
    await flushPromises()

    expect(analysesApi.requestAnalysisFlows).toHaveBeenCalledWith(42, [
      { method: 'PATCH', path: '/api/todos/{id}' },
    ])
  })

  it('AC16 POST가 실패하면 서버 메시지와 다시 시도를 보여 주고 선택은 유지한다', async () => {
    analysesApi.fetchAnalysisFlows.mockResolvedValue([])
    analysesApi.requestAnalysisFlows.mockRejectedValueOnce(
      apiError(400, {
        code: 'ANALYSIS_003',
        message: '실행 흐름은 분석 하나에 3개까지 만들 수 있습니다.',
      }),
    )
    const wrapper = await mountTab()
    await checkbox(wrapper, 'POST /api/todos').setValue(true)

    await submitButton(wrapper).trigger('click')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toBe(
      '실행 흐름은 분석 하나에 3개까지 만들 수 있습니다.',
    )
    expect(checkbox(wrapper, 'POST /api/todos').element.checked).toBe(true)

    analysesApi.requestAnalysisFlows.mockResolvedValueOnce([flow({ id: 7 })])
    await wrapper.find('[data-action="retry-request"]').trigger('click')
    await flushPromises()
    expect(analysesApi.requestAnalysisFlows).toHaveBeenCalledTimes(2)
    expect(analysesApi.requestAnalysisFlows).toHaveBeenLastCalledWith(42, [
      { method: 'POST', path: '/api/todos' },
    ])
  })

  it('AC16 GET이 실패하면 서버 메시지와 다시 시도를 보여 준다', async () => {
    analysesApi.fetchAnalysisFlows.mockRejectedValueOnce(
      apiError(500, { code: 'COMMON_500', message: '서버에서 오류가 발생했습니다.' }),
    )
    const wrapper = await mountTab()

    expect(wrapper.find('[role="alert"]').text()).toBe('서버에서 오류가 발생했습니다.')
    analysesApi.fetchAnalysisFlows.mockResolvedValueOnce([])
    await wrapper.find('[data-action="retry-load"]').trigger('click')
    await flushPromises()

    expect(analysesApi.fetchAnalysisFlows).toHaveBeenCalledTimes(2)
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    expect(wrapper.findAll('input[type="checkbox"]')).toHaveLength(4)
  })
})
