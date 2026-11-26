import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import AnalysisQuizTab from '@/components/analysis/AnalysisQuizTab.vue'
import * as quizzesApi from '@/api/quizzes'
import * as analysesApi from '@/api/analyses'
import { apiError } from '@/views/__tests__/viewTestUtils'

vi.mock('@/api/quizzes')
vi.mock('@/api/analyses')

function quiz(partial) {
  return {
    quizId: 3,
    analysisId: 42,
    questions: [1, 2, 3, 4, 5, 6].map((n) => ({
      id: 20 + n,
      no: n,
      question: `질문 ${n}`,
      choices: ['a', 'b', 'c', 'd'],
    })),
    lastAttempt: null,
    ...partial,
  }
}

async function mountTab() {
  const wrapper = mount(AnalysisQuizTab, {
    props: { analysisId: 42 },
    global: { stubs: { RouterLink: RouterLinkStub } },
  })
  await flushPromises()
  return wrapper
}

describe('AnalysisQuizTab (F-14)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('AC9 퀴즈를 1회 불러와 문항 수와 "퀴즈 풀기" 버튼을 보여 준다', async () => {
    quizzesApi.fetchAnalysisQuiz.mockResolvedValue(quiz())

    const wrapper = await mountTab()

    expect(quizzesApi.fetchAnalysisQuiz).toHaveBeenCalledTimes(1)
    expect(quizzesApi.fetchAnalysisQuiz).toHaveBeenCalledWith(42)
    expect(wrapper.find('[data-message="count"]').text()).toBe('객관식 6문항')
    expect(wrapper.find('[data-message="last-attempt"]').exists()).toBe(false)
    const start = wrapper.findComponent(RouterLinkStub)
    expect(start.props('to')).toBe('/analyses/42/quiz')
    expect(start.text()).toBe('퀴즈 풀기')
  })

  it('AC9 지난 풀이가 있으면 점수·시각과 "다시 풀기"를 보여 준다', async () => {
    quizzesApi.fetchAnalysisQuiz.mockResolvedValue(
      quiz({
        lastAttempt: { attemptId: 15, score: 5, total: 6, createdAt: '2026-11-09T07:05:12+09:00' },
      }),
    )

    const wrapper = await mountTab()

    expect(wrapper.find('[data-message="last-attempt"]').text()).toBe('지난 풀이 5/6 · 11/9 07:05')
    expect(wrapper.findComponent(RouterLinkStub).text()).toBe('다시 풀기')
  })

  it('AC9 QUIZ_404면 퀴즈를 만들지 못했다고 안내한다', async () => {
    quizzesApi.fetchAnalysisQuiz.mockRejectedValue(
      apiError(404, { code: 'QUIZ_404', message: '퀴즈를 찾을 수 없습니다.' }),
    )

    const wrapper = await mountTab()

    expect(wrapper.text()).toContain('이 분석은 퀴즈를 만들지 못했습니다.')
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    expect(wrapper.findComponent(RouterLinkStub).exists()).toBe(false)
  })

  it('AC9 로딩 중 문구를 보여 주고, 그 밖의 실패는 서버 message와 다시 시도 버튼으로 재요청한다', async () => {
    let resolve
    quizzesApi.fetchAnalysisQuiz.mockReturnValueOnce(new Promise((r) => (resolve = r)))
    const wrapper = mount(AnalysisQuizTab, {
      props: { analysisId: 42 },
      global: { stubs: { RouterLink: RouterLinkStub } },
    })
    expect(wrapper.text()).toContain('퀴즈를 불러오는 중…')
    resolve(quiz())
    await flushPromises()
    expect(wrapper.text()).not.toContain('퀴즈를 불러오는 중…')

    quizzesApi.fetchAnalysisQuiz.mockRejectedValueOnce(
      apiError(500, { code: 'COMMON_500', message: '잠시 후 다시 시도해 주세요.' }),
    )
    const failed = await mountTab()
    expect(failed.find('[role="alert"]').text()).toBe('잠시 후 다시 시도해 주세요.')

    quizzesApi.fetchAnalysisQuiz.mockResolvedValueOnce(quiz())
    await failed.find('[data-action="retry-load"]').trigger('click')
    await flushPromises()
    expect(failed.find('[role="alert"]').exists()).toBe(false)
    expect(failed.find('[data-message="count"]').text()).toBe('객관식 6문항')
  })
})

describe('AnalysisQuizTab 퀴즈 다시 만들기 (L-1a AC10)', () => {
  const missing = () => apiError(404, { code: 'QUIZ_404', message: '퀴즈를 찾을 수 없습니다.' })
  const quizStatus = (value) => ({
    analysisId: 42,
    status: 'DONE',
    progress: 100,
    errorCode: null,
    errorMessage: null,
    quizStatus: value,
  })

  async function mountWith(props) {
    const wrapper = mount(AnalysisQuizTab, {
      props: { analysisId: 42, ...props },
      global: { stubs: { RouterLink: RouterLinkStub } },
    })
    await flushPromises()
    return wrapper
  }

  async function advance(ms) {
    await vi.advanceTimersByTimeAsync(ms)
    await flushPromises()
  }

  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('AC10 생성에 실패했고 남은 횟수가 있으면 "퀴즈 다시 만들기 (남은 n회)" 버튼을 보여 준다', async () => {
    quizzesApi.fetchAnalysisQuiz.mockRejectedValue(missing())

    const wrapper = await mountWith({ quizStatus: 'FAILED', regenerationsLeft: 2 })

    expect(wrapper.text()).toContain('퀴즈를 만들지 못했습니다.')
    expect(wrapper.find('[data-action="regenerate"]').text()).toBe('퀴즈 다시 만들기 (남은 2회)')
  })

  it('AC10 남은 횟수가 0이면 버튼 없이 횟수를 다 썼다고 안내한다', async () => {
    quizzesApi.fetchAnalysisQuiz.mockRejectedValue(missing())

    const wrapper = await mountWith({ quizStatus: 'FAILED', regenerationsLeft: 0 })

    expect(wrapper.text()).toContain('퀴즈를 만들지 못했습니다.')
    expect(wrapper.text()).toContain('다시 만들기 횟수를 모두 썼습니다.')
    expect(wrapper.find('[data-action="regenerate"]').exists()).toBe(false)
  })

  it('AC10 버튼을 누르면 만드는 중을 보이고 2초마다 상태를 조회해 READY면 퀴즈를 불러온다', async () => {
    quizzesApi.fetchAnalysisQuiz.mockRejectedValueOnce(missing()).mockResolvedValue(quiz())
    quizzesApi.regenerateQuiz.mockResolvedValue({
      analysisId: 42,
      quizStatus: 'GENERATING',
      quizRegenerationsLeft: 1,
    })
    analysesApi.fetchAnalysisStatus
      .mockResolvedValueOnce(quizStatus('GENERATING'))
      .mockResolvedValue(quizStatus('READY'))

    const wrapper = await mountWith({ quizStatus: 'FAILED', regenerationsLeft: 2 })
    await wrapper.find('[data-action="regenerate"]').trigger('click')
    await flushPromises()

    expect(quizzesApi.regenerateQuiz).toHaveBeenCalledWith(42)
    expect(wrapper.find('[data-message="generating"]').text()).toBe('퀴즈를 만드는 중입니다…')
    expect(analysesApi.fetchAnalysisStatus).not.toHaveBeenCalled()

    await advance(2000)
    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledTimes(1)
    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledWith(42)
    expect(wrapper.find('[data-message="generating"]').exists()).toBe(true)

    await advance(2000)
    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledTimes(2)
    expect(quizzesApi.fetchAnalysisQuiz).toHaveBeenCalledTimes(2)
    expect(wrapper.find('[data-message="count"]').text()).toBe('객관식 6문항')

    await advance(10000)
    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledTimes(2)
  })

  it('AC10 처음부터 GENERATING이면 만드는 중을 보이고, FAILED가 되면 실패 문구와 갱신된 남은 횟수로 돌아간다', async () => {
    quizzesApi.fetchAnalysisQuiz.mockRejectedValue(missing())
    analysesApi.fetchAnalysisStatus.mockResolvedValue(quizStatus('FAILED'))

    const wrapper = await mountWith({ quizStatus: 'GENERATING', regenerationsLeft: 1 })

    expect(wrapper.find('[data-message="generating"]').exists()).toBe(true)
    await advance(2000)

    expect(wrapper.find('[data-message="generating"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('퀴즈를 만들지 못했습니다.')
    expect(wrapper.find('[data-action="regenerate"]').text()).toBe('퀴즈 다시 만들기 (남은 1회)')
  })

  it('AC10 재생성 뒤 FAILED가 되면 응답의 남은 횟수로 버튼을 갱신한다', async () => {
    quizzesApi.fetchAnalysisQuiz.mockRejectedValue(missing())
    quizzesApi.regenerateQuiz.mockResolvedValue({
      analysisId: 42,
      quizStatus: 'GENERATING',
      quizRegenerationsLeft: 0,
    })
    analysesApi.fetchAnalysisStatus.mockResolvedValue(quizStatus('FAILED'))

    const wrapper = await mountWith({ quizStatus: 'FAILED', regenerationsLeft: 1 })
    await wrapper.find('[data-action="regenerate"]').trigger('click')
    await advance(2000)

    expect(wrapper.text()).toContain('다시 만들기 횟수를 모두 썼습니다.')
    expect(wrapper.find('[data-action="regenerate"]').exists()).toBe(false)
  })

  it('AC10 탭을 떠나면 폴링을 멈춘다', async () => {
    quizzesApi.fetchAnalysisQuiz.mockRejectedValue(missing())
    analysesApi.fetchAnalysisStatus.mockResolvedValue(quizStatus('GENERATING'))

    const wrapper = await mountWith({ quizStatus: 'GENERATING', regenerationsLeft: 2 })
    await advance(2000)
    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledTimes(1)
    wrapper.unmount()
    await advance(10000)

    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledTimes(1)
  })

  it('AC10 429·409·404면 서버 메시지를 보인다', async () => {
    quizzesApi.fetchAnalysisQuiz.mockRejectedValue(missing())
    const message = '이 분석의 퀴즈 다시 만들기 횟수(3회)를 모두 썼습니다.'
    quizzesApi.regenerateQuiz.mockRejectedValue(apiError(429, { code: 'QUIZ_429', message }))

    const wrapper = await mountWith({ quizStatus: 'FAILED', regenerationsLeft: 1 })
    await wrapper.find('[data-action="regenerate"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-error="regenerate"]').text()).toBe(message)
    expect(wrapper.find('[data-message="generating"]').exists()).toBe(false)
    expect(analysesApi.fetchAnalysisStatus).not.toHaveBeenCalled()
  })
})
