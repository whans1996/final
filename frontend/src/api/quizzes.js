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

// 풀이 기록·오답 노트 (L-1b, docs/specs/L-1b-wrong-notes.md 3절). 선택지·위치는 저장된 순서 기준이다.

/**
 * 그 퀴즈의 내 풀이 최신순 최대 20개. 없으면 []. 열 수 없는 퀴즈면 404 QUIZ_404.
 * @returns {Promise<{ attemptId: number, score: number, total: number, createdAt: string }[]>}
 */
export function fetchQuizAttempts(quizId) {
  return http.get(`/quizzes/${quizId}/attempts`).then((res) => res.data)
}

/**
 * 내 지난 풀이 하나의 문항별 결과 (results는 submitQuizAttempt의 results와 같은 모양, reviewAdded 없음).
 * 404 QUIZ_404(퀴즈를 열 수 없음), QUIZ_001(남의·다른 퀴즈의·없는 풀이).
 * @returns {Promise<{ attemptId: number, score: number, total: number, createdAt: string, results: { questionId: number, no: number, question: string, choices: string[], selectedIndex: number, answerIndex: number, correct: boolean, explanation: string, concept: object | null }[] }>}
 */
export function fetchQuizAttempt(quizId, attemptId) {
  return http.get(`/quizzes/${quizId}/attempts/${attemptId}`).then((res) => res.data)
}

/**
 * 내 오답 노트: 지금 열 수 있는 분석에서 마지막 답이 틀린 문항을 개념별로 묶는다 (최근 200개, totalCount는 전체 수).
 * concept가 null인 묶음은 개념이 없거나 공개되지 않은 문항이고 맨 뒤에 온다.
 * @returns {Promise<{ totalCount: number, groups: { concept: object | null, items: { questionId: number, quizId: number, analysisId: number, analysisTitle: string, question: string, choices: string[], selectedIndex: number, answerIndex: number, explanation: string, wrongCount: number, lastAnsweredAt: string }[] }[] }>}
 */
export function fetchWrongNotes() {
  return http.get('/users/me/wrong-notes').then((res) => res.data)
}
