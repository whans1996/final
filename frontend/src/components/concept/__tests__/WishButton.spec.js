import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import WishButton from '@/components/concept/WishButton.vue'
import * as conceptsApi from '@/api/concepts'
import { routes } from '@/router'
import { useAuthStore } from '@/stores/auth'
import { apiError } from '@/views/__tests__/viewTestUtils'

vi.mock('@/api/concepts')

async function mountButton({ loggedIn = true, wished = false, wishCount = 12 } = {}) {
  const pinia = createPinia()
  setActivePinia(pinia)
  if (loggedIn) {
    useAuthStore().setSession({
      accessToken: 'access-1',
      user: { id: 1, nickname: '코딩초보', level: 'JUNIOR', role: 'USER' },
    })
  }
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push('/concepts/57')
  await router.isReady()
  const wrapper = mount(WishButton, {
    props: { conceptId: 57, wished, wishCount },
    global: { plugins: [pinia, router] },
  })
  return { wrapper, router }
}

const button = (wrapper) => wrapper.find('[data-action="toggle-wish"]')

function deferred() {
  let resolve
  let reject
  const promise = new Promise((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

describe('WishButton (F-12 AC12~AC14)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('AC12 찜하지 않았으면 빈 하트와 찜 수, aria 속성을 보여 준다', async () => {
    const { wrapper } = await mountButton({ wished: false, wishCount: 12 })

    expect(button(wrapper).text()).toBe('♡ 12')
    expect(button(wrapper).attributes('aria-pressed')).toBe('false')
    expect(button(wrapper).attributes('aria-label')).toBe('찜하기 12')
    expect(button(wrapper).classes()).not.toContain('on')
  })

  it('AC12 찜했으면 채운 하트와 강조 표시, 찜 해제 라벨이다', async () => {
    const { wrapper } = await mountButton({ wished: true, wishCount: 13 })

    expect(button(wrapper).text()).toBe('♥ 13')
    expect(button(wrapper).attributes('aria-pressed')).toBe('true')
    expect(button(wrapper).attributes('aria-label')).toBe('찜 해제 13')
    expect(button(wrapper).classes()).toContain('on')
  })

  it('AC12 비로그인이 누르면 요청 없이 로그인 화면으로 간다', async () => {
    const { wrapper, router } = await mountButton({ loggedIn: false })

    await button(wrapper).trigger('click')
    await flushPromises()

    expect(conceptsApi.wishConcept).not.toHaveBeenCalled()
    // 로그인 화면 컴포넌트를 지연 로드하므로 이동이 끝날 때까지 기다린다
    await vi.waitFor(() => expect(router.currentRoute.value.path).toBe('/login'))
    expect(router.currentRoute.value.query.redirect).toBe('/concepts/57')
  })

  it('AC12 prop이 바뀌면 새 카드의 찜 여부와 수를 보여 준다', async () => {
    const { wrapper } = await mountButton({ wished: true, wishCount: 13 })

    await wrapper.setProps({ conceptId: 58, wished: false, wishCount: 3 })

    expect(button(wrapper).text()).toBe('♡ 3')
    expect(button(wrapper).attributes('aria-pressed')).toBe('false')
  })

  it('AC13 누르면 즉시 채운 하트·N+1로 바꾸고 요청 중 버튼을 끈 뒤 응답 값과 복습 안내를 보여 준다', async () => {
    const request = deferred()
    conceptsApi.wishConcept.mockReturnValue(request.promise)
    const { wrapper } = await mountButton({ wished: false, wishCount: 12 })

    await button(wrapper).trigger('click')

    expect(conceptsApi.wishConcept).toHaveBeenCalledWith(57)
    expect(button(wrapper).text()).toBe('♥ 13')
    expect(button(wrapper).attributes('disabled')).toBeDefined()

    request.resolve({ wished: true, wishCount: 20, reviewPlanDate: '2026-12-04' })
    await flushPromises()

    expect(button(wrapper).text()).toBe('♥ 20')
    expect(button(wrapper).attributes('disabled')).toBeUndefined()
    const message = wrapper.find('[data-message="wish-review-plan"]')
    expect(message.text()).toBe('12월 4일 복습 계획에 추가했습니다. 복습 계획 보기')
    expect(message.find('a').attributes('href')).toBe('/review-plans')
    expect(wrapper.emitted('change')).toEqual([[{ wished: true, wishCount: 20 }]])
  })

  it('AC13 reviewPlanDate가 null이면 복습 안내를 보이지 않는다', async () => {
    conceptsApi.wishConcept.mockResolvedValue({ wished: true, wishCount: 13, reviewPlanDate: null })
    const { wrapper } = await mountButton()

    await button(wrapper).trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-message="wish-review-plan"]').exists()).toBe(false)
  })

  it('AC13 실패하면 원래 상태·숫자로 되돌리고 서버 message를 보여 준다', async () => {
    conceptsApi.wishConcept.mockRejectedValue(
      apiError(404, { code: 'CONCEPT_404', message: '개념 카드를 찾을 수 없습니다.' }),
    )
    const { wrapper } = await mountButton({ wished: false, wishCount: 12 })

    await button(wrapper).trigger('click')
    await flushPromises()

    expect(button(wrapper).text()).toBe('♡ 12')
    expect(button(wrapper).attributes('aria-pressed')).toBe('false')
    expect(wrapper.find('[data-message="wish-error"]').text()).toBe('개념 카드를 찾을 수 없습니다.')
    expect(wrapper.emitted('change')).toBeUndefined()
  })

  it('AC14 찜한 카드에서 누르면 즉시 빈 하트·N−1로 바꾸고 응답 값으로 맞추며 복습 안내를 지운다', async () => {
    conceptsApi.wishConcept.mockResolvedValue({
      wished: true,
      wishCount: 13,
      reviewPlanDate: '2026-12-04',
    })
    const { wrapper } = await mountButton({ wished: false, wishCount: 12 })
    await button(wrapper).trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-message="wish-review-plan"]').exists()).toBe(true)

    const request = deferred()
    conceptsApi.unwishConcept.mockReturnValue(request.promise)
    await button(wrapper).trigger('click')

    expect(conceptsApi.unwishConcept).toHaveBeenCalledWith(57)
    expect(button(wrapper).text()).toBe('♡ 12')
    expect(button(wrapper).attributes('disabled')).toBeDefined()
    expect(wrapper.find('[data-message="wish-review-plan"]').exists()).toBe(false)

    request.resolve({ wished: false, wishCount: 10, reviewPlanDate: null })
    await flushPromises()

    expect(button(wrapper).text()).toBe('♡ 10')
    expect(wrapper.emitted('change').at(-1)).toEqual([{ wished: false, wishCount: 10 }])
  })

  it('AC14 해제가 실패하면 되돌리고 서버 message를 보여 준다', async () => {
    conceptsApi.unwishConcept.mockRejectedValue(
      apiError(500, { code: 'COMMON_500', message: '서버 오류입니다.' }),
    )
    const { wrapper } = await mountButton({ wished: true, wishCount: 13 })

    await button(wrapper).trigger('click')
    await flushPromises()

    expect(button(wrapper).text()).toBe('♥ 13')
    expect(wrapper.find('[data-message="wish-error"]').text()).toBe('서버 오류입니다.')
  })
})

describe('WishButton 카드가 바뀐 뒤 온 응답 (V-4 AC9)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  async function startOnAThenMoveToB() {
    const call = deferred()
    conceptsApi.wishConcept.mockReturnValueOnce(call.promise)
    const { wrapper } = await mountButton({ wished: false, wishCount: 12 })
    await button(wrapper).trigger('click')
    expect(button(wrapper).element.disabled).toBe(true)
    await wrapper.setProps({ conceptId: 58, wished: true, wishCount: 30 })
    return { wrapper, call }
  }

  it('AC9 A의 성공 응답은 B의 찜 상태·수·복습 예정일을 바꾸지 않고 change를 올리지 않으며 잠금은 풀린다', async () => {
    const { wrapper, call } = await startOnAThenMoveToB()

    call.resolve({ wished: true, wishCount: 13, reviewPlanDate: '2026-12-01' })
    await flushPromises()

    expect(button(wrapper).text()).toBe('♥ 30')
    expect(button(wrapper).attributes('aria-pressed')).toBe('true')
    expect(wrapper.find('[data-message="wish-review-plan"]').exists()).toBe(false)
    expect(wrapper.emitted('change')).toBeUndefined()
    expect(button(wrapper).element.disabled).toBe(false)
  })

  it('AC9 A의 실패 응답은 B를 되돌리거나 에러를 보이지 않고 잠금은 풀린다', async () => {
    const { wrapper, call } = await startOnAThenMoveToB()
    expect(button(wrapper).element.disabled).toBe(false)

    call.reject(apiError(500, { code: 'COMMON_500', message: '찜하지 못했습니다' }))
    await flushPromises()

    expect(button(wrapper).text()).toBe('♥ 30')
    expect(wrapper.find('[data-message="wish-error"]').exists()).toBe(false)
    expect(wrapper.emitted('change')).toBeUndefined()
    expect(button(wrapper).element.disabled).toBe(false)
  })
})
