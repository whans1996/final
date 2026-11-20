import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import http from '@/api/http'
import {
  addReviewPlan,
  deleteReviewPlan,
  fetchReviewPlans,
  updateReviewPlanStatus,
} from '@/api/reviewPlans'

// API 계약(docs/specs/F-15-review-plan.md 3절)과 URL·메서드·필드 이름이 같은지 확인한다
function capture(data = {}, status = 200) {
  const calls = []
  http.defaults.adapter = async (config) => {
    calls.push(config)
    return { data, status, statusText: 'OK', headers: {}, config }
  }
  return calls
}

describe('api/reviewPlans (F-15)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('AC1 fetchReviewPlans는 기간이 없으면 쿼리 없이 GET /api/review-plans', async () => {
    const body = {
      today: '2026-11-20',
      from: '2026-11-20',
      to: '2026-11-26',
      overdue: [],
      items: [],
    }
    const calls = capture(body)

    await expect(fetchReviewPlans()).resolves.toEqual(body)

    expect(calls[0].method).toBe('get')
    expect(calls[0].url).toBe('/review-plans')
    expect(calls[0].params).toBeUndefined()
  })

  it('AC1 fetchReviewPlans는 from·to를 쿼리로 보낸다', async () => {
    const calls = capture({})

    await fetchReviewPlans({ from: '2026-11-27', to: '2026-12-03' })

    expect(calls[0].params).toEqual({ from: '2026-11-27', to: '2026-12-03' })
  })

  it('AC4 addReviewPlan은 POST /api/review-plans에 conceptId(와 planDate)를 보낸다', async () => {
    const item = { id: 40, planDate: '2026-11-21', status: 'TODO', reason: 'MANUAL' }
    const calls = capture(item, 201)

    await expect(addReviewPlan({ conceptId: 12 })).resolves.toEqual(item)
    await addReviewPlan({ conceptId: 12, planDate: '2026-11-25' })

    expect(calls[0].method).toBe('post')
    expect(calls[0].url).toBe('/review-plans')
    expect(JSON.parse(calls[0].data)).toEqual({ conceptId: 12 })
    expect(JSON.parse(calls[1].data)).toEqual({ conceptId: 12, planDate: '2026-11-25' })
  })

  it('AC6 updateReviewPlanStatus는 PATCH /api/review-plans/{id}에 status를 보낸다', async () => {
    const calls = capture({ id: 31, status: 'DONE' })

    await expect(updateReviewPlanStatus(31, 'DONE')).resolves.toEqual({ id: 31, status: 'DONE' })

    expect(calls[0].method).toBe('patch')
    expect(calls[0].url).toBe('/review-plans/31')
    expect(JSON.parse(calls[0].data)).toEqual({ status: 'DONE' })
  })

  it('AC7 deleteReviewPlan은 DELETE /api/review-plans/{id}', async () => {
    const calls = capture('', 204)

    await deleteReviewPlan(31)

    expect(calls[0].method).toBe('delete')
    expect(calls[0].url).toBe('/review-plans/31')
  })
})
