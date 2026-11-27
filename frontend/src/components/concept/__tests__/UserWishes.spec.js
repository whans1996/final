import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import UserWishes from '@/components/concept/UserWishes.vue'
import * as conceptsApi from '@/api/concepts'
import { routes } from '@/router'
import { apiError, deferred } from '@/views/__tests__/viewTestUtils'

vi.mock('@/api/concepts')

function card(id) {
  return {
    id,
    name: `카드 ${id}`,
    category: 'security',
    difficulty: 'JUNIOR',
    summary: '요약',
    avgRating: 4.0,
    wishCount: 3,
  }
}

const cards = (from, count) => Array.from({ length: count }, (_, i) => card(from + i))

async function mountWishes(props = { userId: 3, isMe: false }) {
  setActivePinia(createPinia())
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push(`/users/${props.userId}`)
  await router.isReady()
  const wrapper = mount(UserWishes, { props, global: { plugins: [router] } })
  await flushPromises()
  return { wrapper, router }
}

const moreButton = (wrapper) => wrapper.find('[data-action="more-wishes"]')

describe('UserWishes (F-12 AC15)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('AC15 "찜한 카드 N" 제목과 ConceptCard 격자를 보여 주고 카드는 상세로 간다', async () => {
    conceptsApi.fetchUserWishes.mockResolvedValue({
      items: cards(1, 12),
      page: 1,
      size: 12,
      total: 13,
    })

    const { wrapper } = await mountWishes()

    expect(conceptsApi.fetchUserWishes).toHaveBeenCalledWith(3, { page: 1, size: 12 })
    expect(wrapper.find('h2').text()).toBe('찜한 카드 13')
    const links = wrapper.findAll('[data-concept-id]')
    expect(links).toHaveLength(12)
    expect(links[0].attributes('href')).toBe('/concepts/1')
  })

  it('AC15 "더 보기"는 다음 12개를 아래에 붙이고 다 불러오면 사라진다', async () => {
    conceptsApi.fetchUserWishes
      .mockResolvedValueOnce({ items: cards(1, 12), page: 1, size: 12, total: 13 })
      .mockResolvedValueOnce({ items: cards(13, 1), page: 2, size: 12, total: 13 })
    const { wrapper } = await mountWishes()

    await moreButton(wrapper).trigger('click')
    await flushPromises()

    expect(conceptsApi.fetchUserWishes).toHaveBeenLastCalledWith(3, { page: 2, size: 12 })
    expect(wrapper.findAll('[data-concept-id]')).toHaveLength(13)
    expect(moreButton(wrapper).exists()).toBe(false)
  })

  it('AC15 그사이 찜이 늘어 다음 쪽이 앞 쪽과 겹치면 같은 카드를 두 번 붙이지 않고, 한 쪽보다 적게 오면 "더 보기"를 숨긴다', async () => {
    // 1쪽(1~12)을 받은 뒤 새 찜(0)이 맨 앞에 생겨 2쪽이 12·13으로 밀려 온다
    conceptsApi.fetchUserWishes
      .mockResolvedValueOnce({ items: cards(1, 12), page: 1, size: 12, total: 13 })
      .mockResolvedValueOnce({ items: cards(12, 2), page: 2, size: 12, total: 14 })
    const { wrapper } = await mountWishes()

    await moreButton(wrapper).trigger('click')
    await flushPromises()

    const ids = wrapper.findAll('[data-concept-id]').map((a) => a.attributes('data-concept-id'))
    expect(ids).toHaveLength(13)
    expect(new Set(ids).size).toBe(13)
    expect(ids.at(-1)).toBe('13')
    expect(moreButton(wrapper).exists()).toBe(false)
  })

  it('AC15 전부 한 번에 왔으면 "더 보기"가 없다', async () => {
    conceptsApi.fetchUserWishes.mockResolvedValue({
      items: cards(1, 2),
      page: 1,
      size: 12,
      total: 2,
    })

    const { wrapper } = await mountWishes()

    expect(moreButton(wrapper).exists()).toBe(false)
  })

  it('AC15 0건이면 내 프로필은 안내와 둘러보기 링크를 보여 준다', async () => {
    conceptsApi.fetchUserWishes.mockResolvedValue({ items: [], page: 1, size: 12, total: 0 })

    const { wrapper } = await mountWishes({ userId: 3, isMe: true })

    expect(wrapper.find('h2').text()).toBe('찜한 카드 0')
    expect(wrapper.text()).toContain('아직 찜한 카드가 없습니다.')
    const link = wrapper.find('[data-action="browse-concepts"]')
    expect(link.text()).toBe('개념 카드 둘러보기')
    expect(link.attributes('href')).toBe('/concepts')
  })

  it('AC15 0건이면 남의 프로필은 안내만 보여 준다', async () => {
    conceptsApi.fetchUserWishes.mockResolvedValue({ items: [], page: 1, size: 12, total: 0 })

    const { wrapper } = await mountWishes({ userId: 3, isMe: false })

    expect(wrapper.text()).toContain('아직 찜한 카드가 없습니다.')
    expect(wrapper.find('[data-action="browse-concepts"]').exists()).toBe(false)
  })

  it('AC15 불러오는 동안 안내를 보여 준다', async () => {
    conceptsApi.fetchUserWishes.mockReturnValue(new Promise(() => {}))

    const { wrapper } = await mountWishes()

    expect(wrapper.text()).toContain('찜한 카드를 불러오는 중…')
  })

  it('AC15 조회가 실패하면 절 안에 서버 message를 보여 준다', async () => {
    conceptsApi.fetchUserWishes.mockRejectedValue(
      apiError(500, { code: 'COMMON_500', message: '서버 오류가 발생했습니다.' }),
    )

    const { wrapper } = await mountWishes()

    expect(wrapper.find('[role="alert"]').text()).toBe('서버 오류가 발생했습니다.')
  })

  it('AC15 userId가 바뀌면 처음부터 다시 불러온다', async () => {
    conceptsApi.fetchUserWishes
      .mockResolvedValueOnce({ items: cards(1, 12), page: 1, size: 12, total: 20 })
      .mockResolvedValueOnce({ items: cards(50, 1), page: 1, size: 12, total: 1 })
    const { wrapper } = await mountWishes()

    await wrapper.setProps({ userId: 4 })
    await flushPromises()

    expect(conceptsApi.fetchUserWishes).toHaveBeenLastCalledWith(4, { page: 1, size: 12 })
    expect(wrapper.find('h2').text()).toBe('찜한 카드 1')
    expect(wrapper.findAll('[data-concept-id]')).toHaveLength(1)
  })
})

describe('UserWishes 늦은 응답 (V-4 AC6·AC7)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('AC6·AC7 이전 회원의 늦은 실패와 사라진 뒤 온 응답은 상태를 바꾸지 않는다', async () => {
    const first = deferred()
    const second = deferred()
    conceptsApi.fetchUserWishes
      .mockReturnValueOnce(first.promise)
      .mockReturnValueOnce(second.promise)
    const { wrapper } = await mountWishes({ userId: 3, isMe: false })

    await wrapper.setProps({ userId: 4 })
    first.reject(apiError(500, { code: 'COMMON_500', message: '옛 찜 실패' }))
    await flushPromises()
    const vm = wrapper.vm
    expect(vm.error).toBe('')
    expect(vm.loading).toBe(true)

    wrapper.unmount()
    second.resolve({ items: cards(1, 2), page: 1, size: 12, total: 2 })
    await flushPromises()
    expect(vm.items).toEqual([])
    expect(vm.loading).toBe(true)
  })
})
