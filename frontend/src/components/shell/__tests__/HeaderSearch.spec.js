import { describe, expect, it } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import HeaderSearch from '@/components/shell/HeaderSearch.vue'
import { SEARCH_TARGETS, searchLocation } from '@/utils/searchTargets'

const Blank = { template: '<div />' }

async function mountSearch() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: ['/', '/concepts', '/community'].map((path) => ({ path, component: Blank })),
  })
  router.push('/')
  await router.isReady()
  const wrapper = mount(HeaderSearch, { global: { plugins: [router] } })
  return { wrapper, router }
}

async function submit(wrapper) {
  await wrapper.find('form').trigger('submit')
  await flushPromises()
}

describe('HeaderSearch (F-29b AC8·AC9)', () => {
  it('AC8 개념 카드·Spring·트랜잭션으로 Enter를 누르면 /concepts?keyword=트랜잭션&category=spring', async () => {
    const { wrapper, router } = await mountSearch()

    expect(wrapper.find('form').attributes('role')).toBe('search')
    await wrapper.find('select[aria-label="검색 대상"]').setValue('concepts')
    await wrapper.find('select[aria-label="카테고리"]').setValue('spring')
    await wrapper.find('input[type="search"]').setValue('트랜잭션')
    await submit(wrapper)

    expect(router.currentRoute.value.path).toBe('/concepts')
    expect(router.currentRoute.value.query).toEqual({ keyword: '트랜잭션', category: 'spring' })
  })

  it('AC8 대상이 커뮤니티면 /community?keyword=… 이고 카테고리 선택은 보이지 않는다', async () => {
    const { wrapper, router } = await mountSearch()

    expect(wrapper.find('select[aria-label="카테고리"]').exists()).toBe(true)
    await wrapper.find('select[aria-label="검색 대상"]').setValue('posts')
    expect(wrapper.find('select[aria-label="카테고리"]').exists()).toBe(false)

    await wrapper.find('input[type="search"]').setValue('  트랜잭션 ')
    await submit(wrapper)

    expect(router.currentRoute.value.path).toBe('/community')
    expect(router.currentRoute.value.query).toEqual({ keyword: '트랜잭션' })
  })

  it('AC8 카테고리를 고르지 않으면 keyword만 넘긴다', async () => {
    const { wrapper, router } = await mountSearch()

    await wrapper.find('input[type="search"]').setValue('트랜잭션')
    await submit(wrapper)

    expect(router.currentRoute.value.fullPath).toBe(
      `/concepts?keyword=${encodeURIComponent('트랜잭션')}`,
    )
  })

  it('AC8 빈 검색어·공백만 있으면 이동하지 않는다', async () => {
    const { wrapper, router } = await mountSearch()

    await submit(wrapper)
    await wrapper.find('input[type="search"]').setValue('   ')
    await submit(wrapper)

    expect(router.currentRoute.value.path).toBe('/')
  })

  it('AC9 검색 대상은 keyword API가 있는 개념 카드·커뮤니티뿐이고 searchTargets 배열에서 온다', async () => {
    const { wrapper } = await mountSearch()
    const options = wrapper.findAll('select[aria-label="검색 대상"] option')

    expect(SEARCH_TARGETS.map((t) => t.key)).toEqual(['concepts', 'posts'])
    expect(options.map((o) => [o.attributes('value'), o.text()])).toEqual([
      ['concepts', '개념 카드'],
      ['posts', '커뮤니티'],
    ])
  })

  it('AC9 searchLocation: 모르는 대상은 첫 대상으로, 대상에 없는 필터는 버린다', () => {
    expect(searchLocation('posts', 'a', { category: 'spring' })).toEqual({
      path: '/community',
      query: { keyword: 'a' },
    })
    expect(searchLocation('nope', ' a ')).toEqual({ path: '/concepts', query: { keyword: 'a' } })
    expect(searchLocation('concepts', '')).toBeNull()
  })
})
