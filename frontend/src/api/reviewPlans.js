import http from '@/api/http'

// 복습 계획 (F-15, docs/specs/F-15-review-plan.md 3절). 로그인 필요 · 본인 계획만.

/**
 * @typedef {{ id: number, planDate: string, status: 'TODO' | 'DONE', reason: 'WRONG' | 'WISH' | 'MANUAL', doneAt: string | null, concept: object }} ReviewPlanItem
 */

/**
 * 기간 안의 계획과 밀린 복습. 기간을 생략하면 오늘부터 7일. 형식 오류·31일 초과는 400 COMMON_400.
 * @param {{ from?: string, to?: string }} [period] YYYY-MM-DD, 둘 다 주거나 둘 다 생략
 * @returns {Promise<{ today: string, from: string, to: string, overdue: ReviewPlanItem[], items: ReviewPlanItem[] }>}
 */
export function fetchReviewPlans({ from, to } = {}) {
  const params = from && to ? { from, to } : undefined
  return http.get('/review-plans', { params }).then((res) => res.data)
}

/**
 * 직접 추가 (201, reason MANUAL). planDate를 생략하면 내일. 이미 있으면 409 REVIEW_PLAN_409, 비공개·없는 카드는 404 CONCEPT_404.
 * @param {{ conceptId: number, planDate?: string }} body
 * @returns {Promise<ReviewPlanItem>}
 */
export function addReviewPlan({ conceptId, planDate }) {
  const body = planDate ? { conceptId, planDate } : { conceptId }
  return http.post('/review-plans', body).then((res) => res.data)
}

/**
 * 완료(DONE)·되돌림(TODO). 남의·없는 계획은 404 REVIEW_PLAN_404, 되돌릴 때 다른 TODO가 있으면 409.
 * @param {number} id
 * @param {'TODO' | 'DONE'} status
 * @returns {Promise<ReviewPlanItem>}
 */
export function updateReviewPlanStatus(id, status) {
  return http.patch(`/review-plans/${id}`, { status }).then((res) => res.data)
}

/** 삭제 (204). 남의·없는 계획은 404 REVIEW_PLAN_404. */
export function deleteReviewPlan(id) {
  return http.delete(`/review-plans/${id}`).then(() => undefined)
}
