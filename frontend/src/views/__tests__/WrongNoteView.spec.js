import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import WrongNoteView from '@/views/WrongNoteView.vue'
import * as quizzesApi from '@/api/quizzes'
import { apiError, deferred, mountAt } from './viewTestUtils'

vi.mock('@/api/quizzes')

const CONCEPT = {
  id: 57,
  name: 'JWT',
  category: 'security',
  difficulty: 'JUNIOR',
  summary: '서명된 토큰',
  wishCount: 4,
}

function item(questionId, partial) {
  return {
    questionId,
    quizId: 3,
    analysisId: 7,
    analysisTitle: 'JWT 기반 인증',
    question: `질문 ${questionId}`,
    choices: ['a', 'b', 'c', 'd'],
    selectedIndex: 3,
    answerIndex: 1,
    explanation: `해설 ${questionId}`,
    wrongCount: 2,
    lastAnsweredAt: '2026-11-30T21:14:03+09:00',
    ...partial,
  }
}

function note() {
  return {
    totalCount: 3,
    groups: [
      { concept: CONCEPT, items: [item(22), item(23, { wrongCount: 1 })] },
      {
        concept: null,
        items: [item(40, { analysisId: 12, analysisTitle: '제목 없는 분석', selectedIndex: 2 })],
      },
    ],
  }
}

async function mountView() {
  const mounted = await mountAt(WrongNoteView, '/wrong-notes')
  await flushPromises()
  return mounted
}

describe('WrongNoteView (L-1b SC-27)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('AC12 문항 수와 개념 묶음(카드 링크·문항 수), 항목의 질문·내 답·정답·해설·틀린 횟수·분석 제목을 보여 준다', async () => {
    quizzesApi.fetchWrongNotes.mockResolvedValue(note())

    const { wrapper } = await mountView()

    expect(wrapper.find('[data-message="total"]').text()).toBe('다시 볼 문항 3개')
    expect(wrapper.find('[data-message="truncated"]').exists()).toBe(false)
    const groups = wrapper.findAll('[data-group]')
    expect(groups.map((g) => g.attributes('data-group'))).toEqual(['57', 'none'])
    expect(groups[0].find('[data-part="concept"]').text()).toBe('JWT')
    expect(groups[0].find('[data-part="concept"]').attributes('href')).toBe('/concepts/57')
    expect(groups[0].find('[data-part="count"]').text()).toBe('2문항')
    expect(groups[1].find('[data-part="concept"]').text()).toBe('관련 카드 없음')
    expect(groups[1].find('a[data-part="concept"]').exists()).toBe(false)

    const first = groups[0].find('[data-wrong-note="22"]')
    expect(first.find('[data-part="question"]').text()).toBe('질문 22')
    expect(first.find('[data-selected]').text()).toContain('d')
    expect(first.find('[data-selected]').text()).toContain('내 답 (오답)')
    expect(first.find('[data-answer]').text()).toContain('b')
    expect(first.find('[data-answer]').text()).toContain('정답')
    expect(first.find('[data-part="explanation"]').text()).toBe('해설 22')
    expect(first.find('[data-part="wrong-count"]').text()).toBe('2번 틀림')
    expect(first.find('[data-part="analysis"]').text()).toBe('JWT 기반 인증')
    expect(groups[1].find('[data-part="analysis"]').text()).toBe('제목 없는 분석')
  })

  it('AC12 "다시 풀기"는 그 분석의 퀴즈로 ?from=wrong-notes를 붙여 보낸다', async () => {
    quizzesApi.fetchWrongNotes.mockResolvedValue(note())

    const { wrapper, router } = await mountView()

    const retry = wrapper.find('[data-wrong-note="40"] [data-action="retry-quiz"]')
    expect(retry.attributes('href')).toBe('/analyses/12/quiz?from=wrong-notes')
    expect(router.resolve(retry.attributes('href')).name).toBe('analysis-quiz')
  })

  it('AC12 200개를 넘으면 최근 200개를 보여 준다고 안내한다', async () => {
    const items = Array.from({ length: 200 }, (_, i) => item(i + 1))
    quizzesApi.fetchWrongNotes.mockResolvedValue({
      totalCount: 230,
      groups: [{ concept: null, items }],
    })

    const { wrapper } = await mountView()

    expect(wrapper.find('[data-message="total"]').text()).toBe('다시 볼 문항 230개')
    expect(wrapper.find('[data-message="truncated"]').text()).toBe('최근 200개를 보여 줘요.')
  })

  it('AC12 로딩 중 문구, 빈 상태와 내 분석 링크를 보여 준다', async () => {
    const pending = deferred()
    quizzesApi.fetchWrongNotes.mockReturnValue(pending.promise)
    const { wrapper } = await mountAt(WrongNoteView, '/wrong-notes')
    expect(wrapper.find('[role="status"]').exists()).toBe(true)

    pending.resolve({ totalCount: 0, groups: [] })
    await flushPromises()
    expect(wrapper.text()).toContain('틀린 문항이 없어요.')
    expect(wrapper.find('[data-action="analyses"]').attributes('href')).toBe('/analyses')
    expect(wrapper.find('[data-message="total"]').exists()).toBe(false)
  })

  it('AC12 조회가 실패하면 서버 메시지와 "다시 시도"를 보이고 다시 부른다', async () => {
    quizzesApi.fetchWrongNotes.mockRejectedValueOnce(
      apiError(500, { code: 'COMMON_500', message: '잠시 후 다시 시도해 주세요.' }),
    )
    const { wrapper } = await mountView()
    expect(wrapper.find('[role="alert"]').text()).toContain('잠시 후 다시 시도해 주세요.')

    quizzesApi.fetchWrongNotes.mockResolvedValueOnce(note())
    await wrapper.find('[role="alert"] button').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('[data-wrong-note]')).toHaveLength(3)
  })

  it('AC14 화면을 떠난 뒤 온 응답은 반영하지 않는다', async () => {
    const pending = deferred()
    quizzesApi.fetchWrongNotes.mockReturnValue(pending.promise)
    const { wrapper } = await mountAt(WrongNoteView, '/wrong-notes')
    const setup = wrapper.vm.$.setupState

    wrapper.unmount()
    pending.resolve(note())
    await flushPromises()

    expect(setup.note).toBeNull()
    expect(setup.loading).toBe(true)
  })
})
