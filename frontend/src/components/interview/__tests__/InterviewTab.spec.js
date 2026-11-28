import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import InterviewTab from '@/components/interview/InterviewTab.vue'
import * as interviewsApi from '@/api/interviews'
import { apiError } from '@/views/__tests__/viewTestUtils'
import { doneInterview, interview } from './fixtures'

vi.mock('@/api/interviews')

async function mountTab() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/analyses/:id', name: 'analysis-detail', component: { template: '<div />' } },
      { path: '/concepts/:id', name: 'concept-detail', component: { template: '<div />' } },
    ],
  })
  await router.push('/analyses/42?tab=interview')
  await router.isReady()
  const wrapper = mount(InterviewTab, {
    props: { analysisId: 42 },
    global: { plugins: [router] },
  })
  await flushPromises()
  return wrapper
}

async function advance(ms) {
  await vi.advanceTimersByTimeAsync(ms)
  await flushPromises()
}

describe('InterviewTab (I-1a AC12~AC14)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('AC12 NONE이면 안내 문구와 "면접 질문 만들기" 버튼을 보이고, 누르면 요청 뒤 3초마다 조회한다', async () => {
    interviewsApi.fetchInterview
      .mockResolvedValueOnce(interview())
      .mockResolvedValueOnce(interview({ status: 'PENDING' }))
      .mockResolvedValue(doneInterview())
    interviewsApi.requestInterview.mockResolvedValue(interview({ status: 'PENDING' }))

    const wrapper = await mountTab()

    expect(interviewsApi.fetchInterview).toHaveBeenCalledWith(42, {
      signal: expect.any(AbortSignal),
    })
    expect(wrapper.text()).toContain(
      '이 프로젝트 코드로 면접 예상 질문을 만들어요. 질문마다 근거 코드와 답의 핵심 포인트를 함께 보여 드려요.',
    )

    await wrapper.find('[data-action="create"]').trigger('click')
    await flushPromises()

    expect(interviewsApi.requestInterview).toHaveBeenCalledWith(42)
    expect(wrapper.find('[data-state="pending"]').text()).toContain('면접 질문을 만드는 중입니다…')
    expect(interviewsApi.fetchInterview).toHaveBeenCalledTimes(1)

    await advance(3000)
    expect(interviewsApi.fetchInterview).toHaveBeenCalledTimes(2)
    await advance(3000)
    expect(interviewsApi.fetchInterview).toHaveBeenCalledTimes(3)
    expect(wrapper.find('[data-state="done"]').exists()).toBe(true)

    await advance(9000)
    expect(interviewsApi.fetchInterview).toHaveBeenCalledTimes(3)
  })

  it('AC12 PENDING이 120회 이어지면 멈추고 "예상보다 오래 걸리고 있습니다."와 "상태 다시 확인"을 보인다', async () => {
    interviewsApi.fetchInterview.mockResolvedValue(interview({ status: 'PENDING' }))

    const wrapper = await mountTab()
    for (let i = 0; i < 125; i++) await advance(3000)

    expect(interviewsApi.fetchInterview).toHaveBeenCalledTimes(120)
    expect(wrapper.find('[data-message="timeout"]').text()).toBe('예상보다 오래 걸리고 있습니다.')

    interviewsApi.fetchInterview.mockResolvedValue(doneInterview())
    await wrapper.find('[data-action="recheck"]').trigger('click')
    await flushPromises()

    expect(interviewsApi.fetchInterview).toHaveBeenCalledTimes(121)
    expect(wrapper.find('[data-state="done"]').exists()).toBe(true)
  })

  it('AC12 탭을 떠나면(unmount) 조회를 멈춘다', async () => {
    interviewsApi.fetchInterview.mockResolvedValue(interview({ status: 'PENDING' }))

    const wrapper = await mountTab()
    wrapper.unmount()
    await advance(9000)

    expect(interviewsApi.fetchInterview).toHaveBeenCalledTimes(1)
  })

  it('AC13 DONE이면 범주 순서로 묶어 보이고 다시 만들기 남은 횟수를 보인다', async () => {
    interviewsApi.fetchInterview.mockResolvedValue(doneInterview({ regenerationsLeft: 1 }))

    const wrapper = await mountTab()

    expect(wrapper.findAll('[data-category]').map((s) => s.attributes('data-category'))).toEqual([
      'STRUCTURE',
      'FLOW',
      'LIMITATION',
    ])
    expect(wrapper.find('[data-category="STRUCTURE"] h3').text()).toBe('구조')
    expect(wrapper.find('[data-category="FLOW"]').text()).toContain('흐름 질문')
    const regenerate = wrapper.find('[data-action="regenerate"]')
    expect(regenerate.text()).toBe('다시 만들기 (남은 1회)')
    expect(regenerate.attributes('disabled')).toBeUndefined()
    expect(wrapper.find('[data-message="no-regenerations"]').exists()).toBe(false)
  })

  it('AC13 다시 만들기를 0회 남기면 버튼이 비활성이고 안내를 보인다', async () => {
    interviewsApi.fetchInterview.mockResolvedValue(doneInterview({ regenerationsLeft: 0 }))

    const wrapper = await mountTab()

    expect(wrapper.find('[data-action="regenerate"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('[data-message="no-regenerations"]').text()).toBe(
      '다시 만들기 횟수를 모두 썼습니다.',
    )
  })

  it('AC13 다시 만들기를 누르면 요청하고 만드는 중으로 바뀐다', async () => {
    interviewsApi.fetchInterview.mockResolvedValue(doneInterview({ regenerationsLeft: 2 }))
    interviewsApi.requestInterview.mockResolvedValue(
      interview({ status: 'PENDING', regenerationsLeft: 1 }),
    )

    const wrapper = await mountTab()
    await wrapper.find('[data-action="regenerate"]').trigger('click')
    await flushPromises()

    expect(interviewsApi.requestInterview).toHaveBeenCalledWith(42)
    expect(wrapper.find('[data-state="pending"]').exists()).toBe(true)
  })

  it('AC14 FAILED면 errorMessage와 횟수 표시 없는 "다시 만들기"를 보인다', async () => {
    interviewsApi.fetchInterview.mockResolvedValue(
      interview({
        status: 'FAILED',
        errorCode: 'LLM_503',
        errorMessage: 'AI 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요.',
      }),
    )
    interviewsApi.requestInterview.mockResolvedValue(interview({ status: 'PENDING' }))

    const wrapper = await mountTab()

    const failed = wrapper.find('[data-state="failed"]')
    expect(failed.text()).toContain('AI 응답을 받지 못했습니다.')
    const retry = failed.find('[data-action="retry-failed"]')
    expect(retry.text()).toBe('다시 만들기')

    await retry.trigger('click')
    await flushPromises()
    expect(interviewsApi.requestInterview).toHaveBeenCalledWith(42)
  })

  it('AC14 POST가 409·429면 서버 message와 "다시 시도"(조회 다시)를 보인다', async () => {
    const message = '이 분석의 면접 질문 다시 만들기 횟수(2회)를 모두 썼습니다.'
    interviewsApi.fetchInterview.mockResolvedValue(doneInterview({ regenerationsLeft: 1 }))
    interviewsApi.requestInterview.mockRejectedValue(
      apiError(429, { code: 'INTERVIEW_429', message }),
    )

    const wrapper = await mountTab()
    await wrapper.find('[data-action="regenerate"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-error="request"]').text()).toContain(message)
    await wrapper.find('[data-error="request"] [data-action="retry-load"]').trigger('click')
    await flushPromises()
    expect(interviewsApi.fetchInterview).toHaveBeenCalledTimes(2)
    expect(wrapper.find('[data-error="request"]').exists()).toBe(false)
  })

  it('AC14 GET이 실패하면 서버 message와 "다시 시도"를 보이고 누르면 다시 조회한다', async () => {
    const message = '분석을 찾을 수 없습니다.'
    interviewsApi.fetchInterview
      .mockRejectedValueOnce(apiError(404, { code: 'ANALYSIS_404', message }))
      .mockResolvedValue(interview())

    const wrapper = await mountTab()

    expect(wrapper.find('[role="alert"]').text()).toContain(message)
    await wrapper.find('[role="alert"] button').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-state="none"]').exists()).toBe(true)
  })
})
