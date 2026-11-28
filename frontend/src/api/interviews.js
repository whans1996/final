import http from './http'

// 면접 예상 질문 (I-1a, docs/specs/I-1a-interview-prep.md 3절). 생성은 비동기라 PENDING이면 다시 불러 확인한다.
// 둘 다 내 분석만: 없거나 남의·삭제된 분석이면 404 ANALYSIS_404.

/**
 * 분석 하나의 면접 질문. 요청한 적이 없으면 status NONE, DONE이 아니면 questions가 null.
 * @param {number} analysisId
 * @param {{ signal?: AbortSignal }} [options]
 * @returns {Promise<{ analysisId: number, status: 'NONE'|'PENDING'|'DONE'|'FAILED', errorCode: string|null, errorMessage: string|null, regenerationsLeft: number, generatedAt: string|null, questions: object[]|null }>}
 */
export function fetchInterview(analysisId, { signal } = {}) {
  return http.get(`/analyses/${analysisId}/interview`, { signal }).then((res) => res.data)
}

/**
 * 처음 만들기·FAILED 다시 시도·DONE 다시 만들기를 요청한다. 202로 PENDING 응답을 준다.
 * 409 INTERVIEW_409(분석 미완료·이미 만드는 중), 429 INTERVIEW_429(다시 만들기 횟수 소진).
 * @param {number} analysisId
 */
export function requestInterview(analysisId) {
  return http.post(`/analyses/${analysisId}/interview`).then((res) => res.data)
}
