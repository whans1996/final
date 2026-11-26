import http from '@/api/http'

// 코드 이해 퀴즈 (F-14, docs/specs/F-14-code-quiz.md 3절). 로그인 필요 · 본인 분석만.

/**
 * 분석의 퀴즈 (정답·해설·개념은 담지 않는다). 남의·없는 분석이면 404 ANALYSIS_404, 퀴즈가 없으면 404 QUIZ_404.
 * @returns {Promise<{ quizId: number, analysisId: number, questions: { id: number, no: number, question: string, choices: string[] }[], lastAttempt: { attemptId: number, score: number, total: number, createdAt: string } | null }>}
 */
export function fetchAnalysisQuiz(analysisId) {
  return http.get(`/analyses/${analysisId}/quiz`).then((res) => res.data)
}

/**
 * 풀이 제출·채점 (201). 틀린 문항의 공개 개념은 내일 복습 계획에 들어간다 (reviewAdded).
 * @param {{ questionId: number, selectedIndex: number }[]} answers 문항마다 정확히 한 번
 * @returns {Promise<{ attemptId: number, score: number, total: number, createdAt: string, results: { questionId: number, no: number, question: string, choices: string[], selectedIndex: number, answerIndex: number, correct: boolean, explanation: string, concept: object | null }[], reviewAdded: { conceptId: number, planDate: string }[] }>}
 */
export function submitQuizAttempt(quizId, answers) {
  return http.post(`/quizzes/${quizId}/attempts`, { answers }).then((res) => res.data)
}

/**
 * 생성에 실패한(quizStatus FAILED) 내 분석의 퀴즈를 다시 만든다 (202, L-1a). 결과는 fetchAnalysisStatus의 quizStatus로 본다.
 * 404 ANALYSIS_404, 409 QUIZ_409(DONE 아님·이미 있음·만드는 중), 429 QUIZ_429(횟수 소진).
 * @returns {Promise<{ analysisId: number, quizStatus: 'GENERATING', quizRegenerationsLeft: number }>}
 */
export function regenerateQuiz(analysisId) {
  return http.post(`/analyses/${analysisId}/quiz`).then((res) => res.data)
}
