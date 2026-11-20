import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import ReviewPlanAddButton from '@/components/reviewplan/ReviewPlanAddButton.vue'
import * as reviewPlansApi from '@/api/reviewPlans'
import { routes } from '@/router'
import { useAuthStore } from '@/stores/auth'
import { apiError } from '@/views/__tests__/viewTestUtils'

vi.mock('@/api/reviewPlans')

async function mountButton({ loggedIn = true } = {}) {
  const pinia = createPinia()
  setActivePinia(pinia)
  if (loggedIn) {
    useAuthStore().setSession({
      accessToken: 'access-1',
      user: { id: 1, nickname: '코딩초보', level: 'JUNIOR', role: 'USER' },
    })
  }
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push('/concepts/12')
  await router.isReady()
  return mount(ReviewPlanAddButton, {
    props: { conceptId: 12 },
    global: { plugins: [pinia, router] },
  })
}

const button = (wrapper) => wrapper.find('[data-action="add-review-plan"]')

describe('ReviewPlanAddButton (F-15 AC11)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('AC11 비로그인이면 버튼을 보이지 않는다', async () => {
    const wrapper = await mountButton({ loggedIn: false })

    expect(button(wrapper).exists()).toBe(false)
  })

  it('AC11 누르면 planDate 없이 추가하고, 요청 중 버튼을 끄며, 성공 문구와 링크를 보여 준다', async () => {
    let resolve
    reviewPlansApi.addReviewPlan.mockReturnValue(new Promise((r) => (resolve = r)))
    const wrapper = await mountButton()

    expect(button(wrapper).text()).toBe('복습에 추가')
    await button(wrapper).trigger('click')

    expect(reviewPlansApi.addReviewPlan).toHaveBeenCalledWith({ conceptId: 12 })
    expect(button(wrapper).attributes('disabled')).toBeDefined()

    resolve({ id: 40, planDate: '2026-11-21', status: 'TODO', reason: 'MANUAL' })
    await flushPromises()

    expect(button(wrapper).attributes('disabled')).toBeUndefined()
    const message = wrapper.find('[data-message="review-plan-added"]')
    expect(message.text()).toBe('11월 21일 복습 계획에 추가했습니다. 복습 계획 보기')
    expect(message.find('[data-action="review-plans"]').attributes('href')).toBe('/review-plans')
  })

  it('AC11 실패(409)하면 서버 message를 버튼 옆에 보여 준다', async () => {
    reviewPlansApi.addReviewPlan.mockRejectedValue(
      apiError(409, { code: 'REVIEW_PLAN_409', message: '이미 복습 계획에 있는 카드입니다.' }),
    )
    const wrapper = await mountButton()

    await button(wrapper).trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-message="review-plan-error"]').text()).toBe(
      '이미 복습 계획에 있는 카드입니다.',
    )
    expect(wrapper.find('[data-message="review-plan-added"]').exists()).toBe(false)
    expect(button(wrapper).attributes('disabled')).toBeUndefined()
  })
})
