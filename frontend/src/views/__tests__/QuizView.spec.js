import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import QuizView from '@/views/QuizView.vue'
import * as quizzesApi from '@/api/quizzes'
import * as challengesApi from '@/api/challenges'
import { myChallenge, page } from '@/components/challenge/__tests__/fixtures'
import { apiError, deferred, mountAt } from './viewTestUtils'

vi.mock('@/api/quizzes')
vi.mock('@/api/challenges')

const QUESTIONS = [
  { id: 21, no: 1, question: '첫 질문\n둘째 줄', choices: ['a1', 'b1', 'c1', 'd1'] },
  { id: 22, no: 2, question: '두 번째 질문', choices: ['a2', 'b2', 'c2', 'd2'] },
]

const CONCEPT = {
  id: 57,
  name: 'JWT 기반 인증',
  category: 'security',
  difficulty: 'JUNIOR',
  summary: '토큰으로 사용자를 확인합니다.',
  avgRating: 4,
  wishCount: 2,
}

function quiz() {
  return { quizId: 3, analysisId: 42, questions: QUESTIONS, lastAttempt: null }
}

function attempt(partial) {
  return {
    attemptId: 16,
    score: 1,
    total: 2,
    createdAt: '2026-11-19T17:42:05+09:00',
    results: [
      {
        questionId: 21,
        no: 1,
        question: QUESTIONS[0].question,
        choices: QUESTIONS[0].choices,
        selectedIndex: 0,
        answerIndex: 0,
        correct: true,
        explanation: '해설 1',
        concept: null,
      },
      {
        questionId: 22,
        no: 2,
        question: QUESTIONS[1].question,
        choices: QUESTIONS[1].choices,
        selectedIndex: 3,
        answerIndex: 1,
        correct: false,
        explanation: '해설 2',
        concept: CONCEPT,
      },
    ],
    reviewAdded: [{ conceptId: 57, planDate: '2026-11-20' }],
    ...partial,
  }
}

async function mountQuiz() {
  const mounted = await mountAt(QuizView, '/analyses/42/quiz')
  await flushPromises()
  return mounted
}

async function choose(wrapper, questionId, index) {
  await wrapper.find(`input[name="q-${questionId}"][value="${index}"]`).setValue(true)
}

function submitButton(wrapper) {
  return wrapper.find('[data-action="submit"]')
}

describe('QuizView (F-14 SC-08)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('AC10 문항마다 번호·질문과 4지선다를 보여 주고, 모두 답해야 제출 버튼이 켜진다', async () => {
    quizzesApi.fetchAnalysisQuiz.mockResolvedValue(quiz())

    const { wrapper } = await mountQuiz()

    expect(quizzesApi.fetchAnalysisQuiz).toHaveBeenCalledWith(42)
    const items = wrapper.findAll('[data-question]')
    expect(items).toHaveLength(2)
    expect(items[0].text()).toContain('1.')
    expect(items[0].find('[data-part="question"]').text()).toBe('첫 질문\n둘째 줄')
    expect(items[0].findAll('input[type="radio"]')).toHaveLength(4)
    expect(items[1].text()).toContain('b2')
    expect(wrapper.find('[data-message="progress"]').text()).toBe('0/2 답함')
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()

    await choose(wrapper, 21, 0)
    expect(wrapper.find('[data-message="progress"]').text()).toBe('1/2 답함')
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()

    await choose(wrapper, 22, 3)
    expect(wrapper.find('[data-message="progress"]').text()).toBe('2/2 답함')
    expect(submitButton(wrapper).attributes('disabled')).toBeUndefined()
  })

  it('AC10 제출 중에는 버튼을 끄고, 성공하면 점수·정오·해설·개념 카드와 복습 안내를 보여 준다', async () => {
    quizzesApi.fetchAnalysisQuiz.mockResolvedValue(quiz())
    let resolve
    quizzesApi.submitQuizAttempt.mockReturnValue(new Promise((r) => (resolve = r)))
    const { wrapper } = await mountQuiz()
    await choose(wrapper, 22, 3)
    await choose(wrapper, 21, 0)

    await submitButton(wrapper).trigger('click')

    expect(quizzesApi.submitQuizAttempt).toHaveBeenCalledWith(3, [
      { questionId: 21, selectedIndex: 0 },
      { questionId: 22, selectedIndex: 3 },
    ])
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()
    resolve(attempt())
    await flushPromises()

    expect(wrapper.find('[data-message="score"]').text()).toBe('1/2')
    const results = wrapper.findAll('[data-question]')
    expect(results[0].find('[data-result]').text()).toBe('정답')
    expect(results[1].find('[data-result]').text()).toBe('오답')
    expect(results[1].find('[data-selected]').text()).toContain('d2')
    expect(results[1].find('[data-answer]').text()).toContain('b2')
    expect(results[1].find('[data-part="explanation"]').text()).toBe('해설 2')
    expect(results[0].find('[data-concept-id]').exists()).toBe(false)
    expect(results[1].find('[data-concept-id]').attributes('href')).toBe('/concepts/57')
    expect(wrapper.find('[data-message="review-added"]').text()).toBe(
      '틀린 개념 1개를 11월 20일 복습 계획에 추가했습니다.',
    )
    // F-15 AC12: 문구 뒤에 복습 계획 링크
    expect(wrapper.find('[data-action="review-plans"]').attributes('href')).toBe('/review-plans')
    expect(wrapper.find('input[type="radio"]').exists()).toBe(false)
  })

  it('#211 제출하면 점수 아래에 진행 중인 퀴즈 챌린지 달성도를 보여 주고, 다시 제출하면 다시 부른다', async () => {
    quizzesApi.fetchAnalysisQuiz.mockResolvedValue(quiz())
    quizzesApi.submitQuizAttempt.mockResolvedValue(attempt())
    challengesApi.fetchMyChallenges.mockResolvedValue(page([myChallenge(1, { progress: 4 })]))
    const { wrapper } = await mountQuiz()
    expect(challengesApi.fetchMyChallenges).not.toHaveBeenCalled()

    await choose(wrapper, 21, 0)
    await choose(wrapper, 22, 1)
    await submitButton(wrapper).trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-challenge-id="1"]').text()).toContain('4 / 10회')

    quizzesApi.submitQuizAttempt.mockResolvedValue(attempt({ attemptId: 17 }))
    challengesApi.fetchMyChallenges.mockResolvedValue(page([myChallenge(1, { progress: 5 })]))
    await wrapper.find('[data-action="retry-quiz"]').trigger('click')
    expect(wrapper.find('[data-section="challenge-progress"]').exists()).toBe(false)
    await choose(wrapper, 21, 0)
    await choose(wrapper, 22, 1)
    await submitButton(wrapper).trigger('click')
    await flushPromises()

    expect(challengesApi.fetchMyChallenges).toHaveBeenCalledTimes(2)
    expect(wrapper.find('[data-challenge-id="1"]').text()).toContain('5 / 10회')
  })

  it('AC10 reviewAdded가 비면 복습 안내를 보여 주지 않는다', async () => {
    quizzesApi.fetchAnalysisQuiz.mockResolvedValue(quiz())
    quizzesApi.submitQuizAttempt.mockResolvedValue(attempt({ reviewAdded: [] }))
    const { wrapper } = await mountQuiz()
    await choose(wrapper, 21, 0)
    await choose(wrapper, 22, 3)

    await submitButton(wrapper).trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-message="score"]').exists()).toBe(true)
    expect(wrapper.find('[data-message="review-added"]').exists()).toBe(false)
    expect(wrapper.find('[data-action="review-plans"]').exists()).toBe(false)
  })

  it('AC10 "다시 풀기"는 답을 비우고 풀이 화면으로, "분석으로 돌아가기"는 퀴즈 탭으로 간다', async () => {
    quizzesApi.fetchAnalysisQuiz.mockResolvedValue(quiz())
    quizzesApi.submitQuizAttempt.mockResolvedValue(attempt())
    const { wrapper, router } = await mountQuiz()
    await choose(wrapper, 21, 0)
    await choose(wrapper, 22, 3)
    await submitButton(wrapper).trigger('click')
    await flushPromises()
    const scrollTo = vi.spyOn(window, 'scrollTo').mockImplementation(() => {})

    await wrapper.find('[data-action="retry-quiz"]').trigger('click')

    // 결과 화면 맨 아래의 버튼을 눌러도 풀이 화면 맨 위(1번 문항)부터 보이게 한다
    expect(scrollTo).toHaveBeenCalledWith({ top: 0 })
    scrollTo.mockRestore()
    expect(wrapper.find('[data-message="score"]').exists()).toBe(false)
    expect(wrapper.find('[data-message="progress"]').text()).toBe('0/2 답함')
    expect(wrapper.findAll('input[type="radio"]:checked')).toHaveLength(0)
    expect(submitButton(wrapper).attributes('disabled')).toBeDefined()

    // 상세 화면은 지연 로딩이라 클릭 이동 대신 링크 주소로 확인한다
    expect(wrapper.find('[data-action="back"]').attributes('href')).toBe('/analyses/42?tab=quiz')
    expect(router.resolve('/analyses/42?tab=quiz').name).toBe('analysis-detail')
  })

  it('AC10 제출이 실패하면 서버 message를 보여 주고 고른 답을 유지한다', async () => {
    quizzesApi.fetchAnalysisQuiz.mockResolvedValue(quiz())
    quizzesApi.submitQuizAttempt.mockRejectedValue(
      apiError(400, { code: 'QUIZ_400', message: '모든 문항에 한 번씩 답해 주세요.' }),
    )
    const { wrapper } = await mountQuiz()
    await choose(wrapper, 21, 2)
    await choose(wrapper, 22, 1)

    await submitButton(wrapper).trigger('click')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toBe('모든 문항에 한 번씩 답해 주세요.')
    expect(wrapper.find('input[name="q-21"][value="2"]').element.checked).toBe(true)
    expect(wrapper.find('input[name="q-22"][value="1"]').element.checked).toBe(true)
    expect(submitButton(wrapper).attributes('disabled')).toBeUndefined()
  })

  it('AC10 퀴즈가 없으면(QUIZ_404) 안내하고, 그 밖의 조회 실패는 다시 시도할 수 있다', async () => {
    quizzesApi.fetchAnalysisQuiz.mockRejectedValueOnce(
      apiError(404, { code: 'QUIZ_404', message: '퀴즈를 찾을 수 없습니다.' }),
    )
    const { wrapper: missing } = await mountQuiz()
    expect(missing.text()).toContain('이 분석은 퀴즈를 만들지 못했습니다.')

    quizzesApi.fetchAnalysisQuiz.mockRejectedValueOnce(
      apiError(500, { code: 'COMMON_500', message: '잠시 후 다시 시도해 주세요.' }),
    )
    const { wrapper } = await mountQuiz()
    expect(wrapper.find('[role="alert"]').text()).toContain('잠시 후 다시 시도해 주세요.')

    quizzesApi.fetchAnalysisQuiz.mockResolvedValueOnce(quiz())
    await wrapper.find('[role="alert"] button').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('[data-question]')).toHaveLength(2)
  })

  it('AC13(F-20b) ?team=T로 들어오면 "팀으로 돌아가기"를 보여 주고 다시 풀어도 쿼리가 남는다', async () => {
    quizzesApi.fetchAnalysisQuiz.mockResolvedValue(quiz())
    quizzesApi.submitQuizAttempt.mockResolvedValue(attempt())
    const { wrapper, router } = await mountAt(QuizView, '/analyses/42/quiz?team=12')
    await flushPromises()

    const back = () => wrapper.find('[data-action="back"]')
    expect(back().text()).toBe('← 팀으로 돌아가기')
    expect(back().attributes('href')).toBe('/teams/12')

    await choose(wrapper, 21, 0)
    await choose(wrapper, 22, 3)
    await submitButton(wrapper).trigger('click')
    await flushPromises()
    expect(back().text()).toBe('← 팀으로 돌아가기')
    vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
    await wrapper.find('[data-action="retry-quiz"]').trigger('click')
    expect(router.currentRoute.value.query.team).toBe('12')
    expect(back().attributes('href')).toBe('/teams/12')
  })

  it('AC13(F-20b) 조회가 404(ANALYSIS_404)여도 팀 링크를 보여 준다', async () => {
    quizzesApi.fetchAnalysisQuiz.mockRejectedValue(
      apiError(404, { code: 'ANALYSIS_404', message: '분석을 찾을 수 없습니다.' }),
    )
    const { wrapper } = await mountAt(QuizView, '/analyses/42/quiz?team=12')
    await flushPromises()

    expect(wrapper.text()).toContain('분석을 찾을 수 없습니다.')
    expect(wrapper.find('[data-action="back"]').attributes('href')).toBe('/teams/12')
  })

  it('AC13(F-20b) team이 숫자가 아니면 F-14 그대로 분석으로 돌아간다', async () => {
    quizzesApi.fetchAnalysisQuiz.mockResolvedValue(quiz())
    const { wrapper } = await mountAt(QuizView, '/analyses/42/quiz?team=abc')
    await flushPromises()

    const back = wrapper.find('[data-action="back"]')
    expect(back.text()).toBe('← 분석으로 돌아가기')
    expect(back.attributes('href')).toBe('/analyses/42?tab=quiz')
  })

  describe('L-1b', () => {
    const labels = (wrapper, questionId) =>
      wrapper.findAll(`[data-question="${questionId}"] [data-choice]`).map((c) => c.text())

    async function solve(wrapper, first, second) {
      await choose(wrapper, 21, first)
      await choose(wrapper, 22, second)
      await submitButton(wrapper).trigger('click')
      await flushPromises()
    }

    it('AC11 처음은 저장된 순서이고, 다시 풀면 문항마다 선지 순서가 직전과 다르게 섞인다', async () => {
      quizzesApi.fetchAnalysisQuiz.mockResolvedValue(quiz())
      quizzesApi.submitQuizAttempt.mockResolvedValue(attempt())
      vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
      const { wrapper } = await mountQuiz()
      expect(labels(wrapper, 21)).toEqual(['a1', 'b1', 'c1', 'd1'])
      expect(labels(wrapper, 22)).toEqual(['a2', 'b2', 'c2', 'd2'])
      await solve(wrapper, 0, 3)
      // 결과 화면도 직전 화면과 같은 순서다
      expect(labels(wrapper, 22)[3]).toContain('d2')

      const random = vi.spyOn(Math, 'random').mockReturnValue(0)
      await wrapper.find('[data-action="retry-quiz"]').trigger('click')
      // random 0 → Fisher–Yates가 [1, 2, 3, 0]을 만든다
      expect(labels(wrapper, 21)).toEqual(['b1', 'c1', 'd1', 'a1'])
      expect(labels(wrapper, 22)).toEqual(['b2', 'c2', 'd2', 'a2'])
      // 문항 순서는 그대로다
      expect(wrapper.findAll('[data-question]').map((q) => q.attributes('data-question'))).toEqual([
        '21',
        '22',
      ])
      expect(wrapper.findAll('input[type="radio"]:checked')).toHaveLength(0)

      await solve(wrapper, 0, 3)
      await wrapper.find('[data-action="retry-quiz"]').trigger('click')
      expect(labels(wrapper, 21)).not.toEqual(['b1', 'c1', 'd1', 'a1'])
      random.mockRestore()
    })

    it('AC11 섞인 화면에서 고른 선지는 저장된 위치로 제출하고, 결과 강조는 섞인 순서의 맞는 선지에 붙는다', async () => {
      quizzesApi.fetchAnalysisQuiz.mockResolvedValue(quiz())
      quizzesApi.submitQuizAttempt.mockResolvedValue(attempt())
      vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
      const { wrapper } = await mountQuiz()
      await solve(wrapper, 0, 3)
      vi.spyOn(Math, 'random').mockReturnValue(0)
      await wrapper.find('[data-action="retry-quiz"]').trigger('click')
      vi.mocked(Math.random).mockRestore()

      // 섞인 화면 [b, c, d, a]에서 22번은 셋째 자리(d2), 21번은 넷째 자리(a1)를 고른다
      const q22 = wrapper.findAll('[data-question="22"] input[type="radio"]')
      await q22[2].setValue(true)
      const q21 = wrapper.findAll('[data-question="21"] input[type="radio"]')
      await q21[3].setValue(true)
      await submitButton(wrapper).trigger('click')
      await flushPromises()

      expect(quizzesApi.submitQuizAttempt).toHaveBeenLastCalledWith(3, [
        { questionId: 21, selectedIndex: 0 },
        { questionId: 22, selectedIndex: 3 },
      ])
      const result = wrapper.find('[data-question="22"]')
      const shown = result.findAll('[data-choice]')
      expect(shown.map((c) => c.attributes('data-choice'))).toEqual(['1', '2', '3', '0'])
      // 정답(저장 위치 1 = b2)은 화면 첫 자리, 내 답(저장 위치 3 = d2)은 셋째 자리
      expect(shown[0].attributes('data-answer')).toBeDefined()
      expect(shown[0].text()).toContain('b2')
      expect(shown[2].attributes('data-selected')).toBeDefined()
      expect(shown[2].text()).toContain('d2')
    })

    it('AC12 ?from=wrong-notes로 들어오면 "오답 노트로 돌아가기"를 보여 준다', async () => {
      quizzesApi.fetchAnalysisQuiz.mockResolvedValue(quiz())
      const { wrapper } = await mountAt(QuizView, '/analyses/42/quiz?from=wrong-notes')
      await flushPromises()

      const back = wrapper.find('[data-action="back"]')
      expect(back.text()).toBe('← 오답 노트로 돌아가기')
      expect(back.attributes('href')).toBe('/wrong-notes')
    })

    it('AC14 조회 중에 분석이 바뀌면 늦게 온 이전 분석의 응답을 버린다', async () => {
      const first = deferred()
      const second = deferred()
      quizzesApi.fetchAnalysisQuiz
        .mockReturnValueOnce(first.promise)
        .mockReturnValueOnce(second.promise)
      const { wrapper, router } = await mountAt(QuizView, '/analyses/42/quiz')

      await router.push('/analyses/43/quiz')
      second.resolve({ ...quiz(), quizId: 4, analysisId: 43, questions: [QUESTIONS[1]] })
      await flushPromises()
      first.resolve(quiz())
      await flushPromises()

      expect(quizzesApi.fetchAnalysisQuiz).toHaveBeenLastCalledWith(43)
      expect(wrapper.findAll('[data-question]').map((q) => q.attributes('data-question'))).toEqual([
        '22',
      ])
    })
  })
})
