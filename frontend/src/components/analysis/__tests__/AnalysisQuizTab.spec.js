import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import AnalysisQuizTab from '@/components/analysis/AnalysisQuizTab.vue'
import * as quizzesApi from '@/api/quizzes'
import { apiError } from '@/views/__tests__/viewTestUtils'

vi.mock('@/api/quizzes')

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
