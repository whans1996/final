import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import http from '@/api/http'
import { fetchAnalysisQuiz, submitQuizAttempt } from '@/api/quizzes'

// API 계약(docs/specs/F-14-code-quiz.md 3절)과 URL·메서드·필드 이름이 같은지 확인한다
function capture(data = {}, status = 200) {
  const calls = []
  http.defaults.adapter = async (config) => {
    calls.push(config)
    return { data, status, statusText: 'OK', headers: {}, config }
  }
  return calls
}

describe('api/quizzes (F-14)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('AC5 fetchAnalysisQuiz는 GET /api/analyses/{id}/quiz 응답 본문을 돌려준다', async () => {
    const quiz = { quizId: 3, analysisId: 7, questions: [], lastAttempt: null }
    const calls = capture(quiz)

    await expect(fetchAnalysisQuiz(7)).resolves.toEqual(quiz)

    expect(calls).toHaveLength(1)
    expect(calls[0].method).toBe('get')
    expect(calls[0].url).toBe('/analyses/7/quiz')
  })

  it('AC6 submitQuizAttempt는 POST /api/quizzes/{quizId}/attempts에 answers를 보낸다', async () => {
    const result = { attemptId: 16, score: 1, total: 2, results: [], reviewAdded: [] }
    const calls = capture(result, 201)
    const answers = [
      { questionId: 21, selectedIndex: 0 },
      { questionId: 22, selectedIndex: 3 },
    ]

    await expect(submitQuizAttempt(3, answers)).resolves.toEqual(result)

    expect(calls[0].method).toBe('post')
    expect(calls[0].url).toBe('/quizzes/3/attempts')
    expect(JSON.parse(calls[0].data)).toEqual({ answers })
  })
})
