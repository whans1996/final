import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import ReviewPlanView from '@/views/reviewplan/ReviewPlanView.vue'
import * as reviewPlansApi from '@/api/reviewPlans'
import { apiError, mountAt } from './viewTestUtils'

vi.mock('@/api/reviewPlans')

function concept(id, name) {
  return {
    id,
    name,
    category: 'spring',
    difficulty: 'JUNIOR',
    summary: `${name} 요약`,
    avgRating: 4,
    wishCount: 1,
  }
}

function plan(id, planDate, partial = {}) {
  return {
    id,
    planDate,
    status: 'TODO',
    reason: 'WRONG',
    doneAt: null,
    concept: concept(100 + id, `카드${id}`),
    ...partial,
  }
}

function response(partial = {}) {
  return {
    today: '2026-11-20',
    from: '2026-11-20',
    to: '2026-11-26',
    overdue: [plan(1, '2026-11-18'), plan(2, '2026-11-19', { reason: 'WISH' })],
    items: [
      plan(3, '2026-11-20', {
        status: 'DONE',
        reason: 'MANUAL',
        doneAt: '2026-11-20T09:00:00+09:00',
      }),
      plan(4, '2026-11-20', { reason: 'WISH' }),
      plan(5, '2026-11-22'),
    ],
    ...partial,
  }
}

async function mountView() {
  const mounted = await mountAt(ReviewPlanView, '/review-plans')
  await flushPromises()
  return mounted
}

const item = (wrapper, id) => wrapper.find(`[data-plan-id="${id}"]`)

describe('ReviewPlanView (F-15 SC-11)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('AC9 제목·기간·이동 버튼, 밀린 복습 개수, 날짜 머리글(오늘)과 배지를 보여 준다', async () => {
    reviewPlansApi.fetchReviewPlans.mockResolvedValue(response())
    const { wrapper } = await mountView()

    expect(reviewPlansApi.fetchReviewPlans).toHaveBeenCalledWith(undefined)
    expect(wrapper.find('h1').text()).toBe('복습 계획')
    expect(wrapper.find('[data-message="period"]').text()).toBe('11월 20일 ~ 11월 26일')
    expect(wrapper.find('[data-action="prev"]').text()).toBe('◀ 이전 7일')
    expect(wrapper.find('[data-action="today"]').text()).toBe('오늘')
    expect(wrapper.find('[data-action="next"]').text()).toBe('다음 7일 ▶')

    const overdue = wrapper.find('[data-section="overdue"]')
    expect(overdue.find('h2').text()).toBe('밀린 복습 2개')
    expect(overdue.findAll('[data-plan-id]').map((w) => w.attributes('data-plan-id'))).toEqual([
      '1',
      '2',
    ])

    const days = wrapper.findAll('[data-date]')
    expect(days.map((d) => d.find('h2').text())).toEqual([
      '11월 20일 (금) · 오늘',
      '11월 22일 (일)',
    ])
    expect(days[0].findAll('[data-plan-id]')).toHaveLength(2)
    expect(item(wrapper, 3).find('[data-reason]').text()).toBe('직접')
    expect(item(wrapper, 4).find('[data-reason]').text()).toBe('찜')
    expect(item(wrapper, 5).find('[data-reason]').text()).toBe('오답')
    expect(item(wrapper, 5).find('[data-concept-id]').attributes('href')).toBe('/concepts/105')
    expect(item(wrapper, 5).find('[data-action="remove"]').text()).toBe('삭제')
  })

  it('AC9 DONE 항목은 흐리게(done) 보이고 체크되어 있다', async () => {
    reviewPlansApi.fetchReviewPlans.mockResolvedValue(response())
    const { wrapper } = await mountView()

    expect(item(wrapper, 3).classes()).toContain('done')
    expect(item(wrapper, 3).find('input[type="checkbox"]').element.checked).toBe(true)
    expect(item(wrapper, 4).classes()).not.toContain('done')
    expect(item(wrapper, 4).find('input[type="checkbox"]').element.checked).toBe(false)
  })

  it('AC9 다음·이전 7일은 응답의 from 기준으로, 오늘은 기간 없이 다시 부른다', async () => {
    reviewPlansApi.fetchReviewPlans.mockResolvedValue(response())
    const { wrapper } = await mountView()

    reviewPlansApi.fetchReviewPlans.mockResolvedValue(
      response({ from: '2026-11-27', to: '2026-12-03', items: [] }),
    )
    await wrapper.find('[data-action="next"]').trigger('click')
    await flushPromises()
    expect(reviewPlansApi.fetchReviewPlans).toHaveBeenLastCalledWith({
      from: '2026-11-27',
      to: '2026-12-03',
    })
    expect(wrapper.find('[data-message="period"]').text()).toBe('11월 27일 ~ 12월 3일')
    // 기간이 오늘부터가 아니면 밀린 복습을 숨긴다
    expect(wrapper.find('[data-section="overdue"]').exists()).toBe(false)

    await wrapper.find('[data-action="prev"]').trigger('click')
    await flushPromises()
    expect(reviewPlansApi.fetchReviewPlans).toHaveBeenLastCalledWith({
      from: '2026-11-20',
      to: '2026-11-26',
    })

    await wrapper.find('[data-action="today"]').trigger('click')
    await flushPromises()
    expect(reviewPlansApi.fetchReviewPlans).toHaveBeenLastCalledWith(undefined)
  })

  it('AC9 밀린 복습이 비면 그 절을 숨긴다', async () => {
    reviewPlansApi.fetchReviewPlans.mockResolvedValue(response({ overdue: [] }))
    const { wrapper } = await mountView()

    expect(wrapper.find('[data-section="overdue"]').exists()).toBe(false)
    expect(wrapper.findAll('[data-date]')).toHaveLength(2)
  })

  it('AC9 밀린 복습과 기간 안에 항목이 없으면 빈 상태와 개념 카드 링크를 보여 준다', async () => {
    reviewPlansApi.fetchReviewPlans.mockResolvedValue(response({ overdue: [], items: [] }))
    const { wrapper } = await mountView()

    expect(wrapper.text()).toContain('이 기간에 복습할 카드가 없습니다.')
    expect(wrapper.find('[data-action="browse-concepts"]').attributes('href')).toBe('/concepts')
  })

  it('AC9 조회 중에는 "불러오는 중…", 실패하면 서버 message와 다시 시도', async () => {
    let reject
    reviewPlansApi.fetchReviewPlans.mockReturnValue(new Promise((_, r) => (reject = r)))
    const { wrapper } = await mountAt(ReviewPlanView, '/review-plans')

    expect(wrapper.text()).toContain('불러오는 중…')

    reject(apiError(500, { code: 'COMMON_500', message: '서버에서 오류가 발생했습니다.' }))
    await flushPromises()
    expect(wrapper.find('[role="alert"]').text()).toBe('서버에서 오류가 발생했습니다.')

    reviewPlansApi.fetchReviewPlans.mockResolvedValue(response())
    await wrapper.find('[data-action="retry-load"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    expect(wrapper.findAll('[data-plan-id]')).toHaveLength(5)
  })

  it('AC10 체크하면 요청 중 끄고, 성공하면 응답으로 바꾸며 밀린 복습에서 빼고 개수를 줄인다', async () => {
    reviewPlansApi.fetchReviewPlans.mockResolvedValue(response())
    const { wrapper } = await mountView()
    let resolve
    reviewPlansApi.updateReviewPlanStatus.mockReturnValue(new Promise((r) => (resolve = r)))

    await item(wrapper, 1).find('input[type="checkbox"]').setValue(true)

    expect(reviewPlansApi.updateReviewPlanStatus).toHaveBeenCalledWith(1, 'DONE')
    expect(item(wrapper, 1).find('input[type="checkbox"]').attributes('disabled')).toBeDefined()
    expect(item(wrapper, 1).find('[data-action="remove"]').attributes('disabled')).toBeDefined()

    resolve(plan(1, '2026-11-18', { status: 'DONE', doneAt: '2026-11-20T10:00:00+09:00' }))
    await flushPromises()

    expect(item(wrapper, 1).exists()).toBe(false)
    expect(wrapper.find('[data-section="overdue"] h2').text()).toBe('밀린 복습 1개')
  })

  it('AC10 기간 안의 DONE을 풀면 TODO로 바뀐다', async () => {
    reviewPlansApi.fetchReviewPlans.mockResolvedValue(response())
    const { wrapper } = await mountView()
    reviewPlansApi.updateReviewPlanStatus.mockResolvedValue(
      plan(3, '2026-11-20', { reason: 'MANUAL' }),
    )

    await item(wrapper, 3).find('input[type="checkbox"]').setValue(false)
    await flushPromises()

    expect(reviewPlansApi.updateReviewPlanStatus).toHaveBeenCalledWith(3, 'TODO')
    expect(item(wrapper, 3).classes()).not.toContain('done')
    expect(item(wrapper, 3).find('input[type="checkbox"]').element.checked).toBe(false)
  })

  it('AC10 체크가 실패하면 체크를 되돌리고 서버 message를 항목 아래에 보여 준다', async () => {
    reviewPlansApi.fetchReviewPlans.mockResolvedValue(response())
    const { wrapper } = await mountView()
    reviewPlansApi.updateReviewPlanStatus.mockRejectedValue(
      apiError(409, { code: 'REVIEW_PLAN_409', message: '이미 복습 계획에 있는 카드입니다.' }),
    )

    await item(wrapper, 4).find('input[type="checkbox"]').setValue(true)
    await flushPromises()

    expect(item(wrapper, 4).find('input[type="checkbox"]').element.checked).toBe(false)
    expect(item(wrapper, 4).find('input[type="checkbox"]').attributes('disabled')).toBeUndefined()
    expect(item(wrapper, 4).find('[data-message="item-error"]').text()).toBe(
      '이미 복습 계획에 있는 카드입니다.',
    )
  })

  it('AC10 삭제하면 대화상자 없이 지우고 목록에서 뺀다', async () => {
    reviewPlansApi.fetchReviewPlans.mockResolvedValue(response())
    const { wrapper } = await mountView()
    const confirm = vi.spyOn(window, 'confirm')
    reviewPlansApi.deleteReviewPlan.mockResolvedValue(undefined)

    await item(wrapper, 2).find('[data-action="remove"]').trigger('click')
    await flushPromises()

    expect(confirm).not.toHaveBeenCalled()
    expect(reviewPlansApi.deleteReviewPlan).toHaveBeenCalledWith(2)
    expect(item(wrapper, 2).exists()).toBe(false)
    expect(wrapper.find('[data-section="overdue"] h2').text()).toBe('밀린 복습 1개')
  })

  it('AC10 삭제가 실패하면 항목을 남기고 서버 message를 보여 준다', async () => {
    reviewPlansApi.fetchReviewPlans.mockResolvedValue(response())
    const { wrapper } = await mountView()
    reviewPlansApi.deleteReviewPlan.mockRejectedValue(
      apiError(404, { code: 'REVIEW_PLAN_404', message: '복습 계획을 찾을 수 없습니다.' }),
    )

    await item(wrapper, 5).find('[data-action="remove"]').trigger('click')
    await flushPromises()

    expect(item(wrapper, 5).find('[data-message="item-error"]').text()).toBe(
      '복습 계획을 찾을 수 없습니다.',
    )
  })
})
