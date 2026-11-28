import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import QuizAttemptHistory from '@/components/quiz/QuizAttemptHistory.vue'
import * as quizzesApi from '@/api/quizzes'
import { apiError, deferred } from '@/views/__tests__/viewTestUtils'

vi.mock('@/api/quizzes')

const ATTEMPTS = [
  { attemptId: 31, score: 1, total: 2, createdAt: '2026-11-30T21:14:03+09:00' },
  { attemptId: 16, score: 0, total: 2, createdAt: '2026-11-29T07:42:05+09:00' },
]

const CONCEPT = {
  id: 57,
  name: 'JWT',
  category: 'security',
  difficulty: 'JUNIOR',
  summary: '서명된 토큰',
  wishCount: 4,
}

function detail(attemptId) {
  return {
    attemptId,
    score: 1,
    total: 2,
    createdAt: '2026-11-30T21:14:03+09:00',
    results: [
      {
        questionId: 21,
        no: 1,
        question: '질문 1',
        choices: ['a1', 'b1', 'c1', 'd1'],
        selectedIndex: 0,
        answerIndex: 0,
        correct: true,
        explanation: '해설 1',
        concept: null,
      },
      {
        questionId: 22,
        no: 2,
        question: '질문 2',
        choices: ['a2', 'b2', 'c2', 'd2'],
        selectedIndex: 3,
        answerIndex: 1,
        correct: false,
        explanation: '해설 2',
        concept: CONCEPT,
      },
    ],
  }
}

async function mountHistory(props = { quizId: 3 }) {
  const wrapper = mount(QuizAttemptHistory, {
    props,
    global: { stubs: { RouterLink: RouterLinkStub } },
  })
  await flushPromises()
  return wrapper
}

const rows = (wrapper) => wrapper.findAll('[data-attempt]')
const toggle = (wrapper, i) =>
  rows(wrapper)[i].find('[data-action="toggle-attempt"]').trigger('click')

describe('QuizAttemptHistory (L-1b SC-07)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('AC10 회차 목록을 최신순으로 날짜·시각과 점수로 보여 준다', async () => {
    quizzesApi.fetchQuizAttempts.mockResolvedValue(ATTEMPTS)

    const wrapper = await mountHistory()

    expect(quizzesApi.fetchQuizAttempts).toHaveBeenCalledWith(3)
    expect(wrapper.find('h3').text()).toBe('풀이 기록')
    expect(rows(wrapper).map((r) => r.attributes('data-attempt'))).toEqual(['31', '16'])
    expect(rows(wrapper)[0].find('[data-part="time"]').text()).toBe('11/30 21:14')
    expect(rows(wrapper)[0].find('[data-part="score"]').text()).toBe('1/2')
    expect(rows(wrapper)[1].find('[data-part="time"]').text()).toBe('11/29 07:42')
    expect(wrapper.find('[data-part="detail"]').exists()).toBe(false)
  })

  it('AC10 회차를 누르면 문항별 정오·내 답·정답·해설·카드가 펼쳐지고, 다시 누르면 접히며 상세는 한 번만 부른다', async () => {
    quizzesApi.fetchQuizAttempts.mockResolvedValue(ATTEMPTS)
    quizzesApi.fetchQuizAttempt.mockResolvedValue(detail(31))
    const wrapper = await mountHistory()

    await toggle(wrapper, 0)
    await flushPromises()

    expect(quizzesApi.fetchQuizAttempt).toHaveBeenCalledWith(3, 31)
    const opened = rows(wrapper)[0]
    expect(opened.find('[data-action="toggle-attempt"]').attributes('aria-expanded')).toBe('true')
    const items = opened.findAll('[data-question]')
    expect(items).toHaveLength(2)
    expect(items[0].find('[data-result]').text()).toBe('정답')
    expect(items[1].find('[data-result]').text()).toBe('오답')
    expect(items[1].find('[data-selected]').text()).toContain('d2')
    expect(items[1].find('[data-answer]').text()).toContain('b2')
    expect(items[1].find('[data-part="explanation"]').text()).toBe('해설 2')
    expect(items[1].find('[data-concept-id]').exists()).toBe(true)
    expect(opened.find('input[type="radio"]').exists()).toBe(false)

    await toggle(wrapper, 0)
    expect(rows(wrapper)[0].find('[data-part="detail"]').exists()).toBe(false)
    await toggle(wrapper, 0)
    await flushPromises()
    expect(rows(wrapper)[0].findAll('[data-question]')).toHaveLength(2)
    expect(quizzesApi.fetchQuizAttempt).toHaveBeenCalledTimes(1)
  })

  it('AC10 다른 회차를 누르면 앞 회차는 접힌다', async () => {
    quizzesApi.fetchQuizAttempts.mockResolvedValue(ATTEMPTS)
    quizzesApi.fetchQuizAttempt.mockImplementation((_quizId, id) => Promise.resolve(detail(id)))
    const wrapper = await mountHistory()

    await toggle(wrapper, 0)
    await flushPromises()
    await toggle(wrapper, 1)
    await flushPromises()

    expect(rows(wrapper)[0].find('[data-part="detail"]').exists()).toBe(false)
    expect(rows(wrapper)[1].findAll('[data-question]')).toHaveLength(2)
    expect(quizzesApi.fetchQuizAttempt).toHaveBeenLastCalledWith(3, 16)
  })

  it('AC10 상세 조회가 실패하면 그 자리에 오류 문구와 "다시 시도"가 보인다', async () => {
    quizzesApi.fetchQuizAttempts.mockResolvedValue(ATTEMPTS)
    quizzesApi.fetchQuizAttempt.mockRejectedValueOnce(
      apiError(500, { code: 'COMMON_500', message: '잠시 후 다시 시도해 주세요.' }),
    )
    const wrapper = await mountHistory()

    await toggle(wrapper, 0)
    await flushPromises()
    const opened = rows(wrapper)[0].find('[data-part="detail"]')
    expect(opened.find('[role="alert"]').text()).toContain('잠시 후 다시 시도해 주세요.')

    quizzesApi.fetchQuizAttempt.mockResolvedValueOnce(detail(31))
    await opened.find('[role="alert"] button').trigger('click')
    await flushPromises()
    expect(rows(wrapper)[0].findAll('[data-question]')).toHaveLength(2)
  })

  it('AC10 목록 조회가 실패하면 오류 문구와 "다시 시도"가 보인다', async () => {
    quizzesApi.fetchQuizAttempts.mockRejectedValueOnce(
      apiError(500, { code: 'COMMON_500', message: '잠시 후 다시 시도해 주세요.' }),
    )
    const wrapper = await mountHistory()
    expect(wrapper.find('[role="alert"]').text()).toContain('잠시 후 다시 시도해 주세요.')

    quizzesApi.fetchQuizAttempts.mockResolvedValueOnce(ATTEMPTS)
    await wrapper.find('[role="alert"] button').trigger('click')
    await flushPromises()
    expect(rows(wrapper)).toHaveLength(2)
  })

  it('AC10 풀이가 없으면 "풀이 기록" 영역이 보이지 않는다', async () => {
    quizzesApi.fetchQuizAttempts.mockResolvedValue([])

    const wrapper = await mountHistory()

    expect(wrapper.find('[data-section="attempt-history"]').exists()).toBe(false)
  })

  it('AC14 조회 중에 퀴즈가 바뀌면 늦게 온 이전 퀴즈의 목록을 버린다', async () => {
    const first = deferred()
    quizzesApi.fetchQuizAttempts
      .mockReturnValueOnce(first.promise)
      .mockResolvedValueOnce([ATTEMPTS[1]])
    const wrapper = await mountHistory()

    await wrapper.setProps({ quizId: 4 })
    await flushPromises()
    first.resolve(ATTEMPTS)
    await flushPromises()

    expect(quizzesApi.fetchQuizAttempts).toHaveBeenLastCalledWith(4)
    expect(rows(wrapper).map((r) => r.attributes('data-attempt'))).toEqual(['16'])
  })
})
