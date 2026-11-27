import { describe, expect, it } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import AppHeader from '@/components/shell/AppHeader.vue'

const Blank = { template: '<div />' }

async function mountHeader(slots = {}) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: ['/', '/concepts', '/community'].map((path) => ({ path, component: Blank })),
  })
  router.push('/')
  await router.isReady()
  return mount(AppHeader, { slots, global: { plugins: [router] } })
}

describe('AppHeader (F-29b AC2·AC9·AC12)', () => {
  it('AC2 로고·검색·다크 토글을 그리고 계정 자리는 슬롯으로 받는다', async () => {
    const wrapper = await mountHeader({ account: '<span id="acc">계정</span>' })

    expect(wrapper.find('a[href="/"]').text()).toBe('바이브노트')
    expect(wrapper.findAll('form[role="search"]')).toHaveLength(1)
    expect(wrapper.find('button[aria-label^="테마:"]').exists()).toBe(true)
    expect(wrapper.find('#acc').exists()).toBe(true)
  })

  it('AC12 알림 자리는 #notifications 슬롯뿐이고 기본으로는 아무것도 그리지 않는다', async () => {
    const empty = await mountHeader()
    const labels = empty.findAll('button').map((b) => b.attributes('aria-label') ?? '')

    expect(labels.some((label) => label.includes('알림'))).toBe(false)
    expect(empty.find('[data-notifications]').exists()).toBe(false)

    const filled = await mountHeader({ notifications: '<button data-notifications>알림</button>' })
    expect(filled.find('[data-notifications]').exists()).toBe(true)
  })

  it('AC9 640 미만 🔍 버튼을 누르면 헤더 아래 검색 줄이 펼쳐지고, 검색하면 닫힌다', async () => {
    const wrapper = await mountHeader()
    const toggle = wrapper.find('button[aria-controls="mobile-search"]')

    expect(toggle.attributes('aria-expanded')).toBe('false')
    expect(wrapper.find('#mobile-search').exists()).toBe(false)

    await toggle.trigger('click')
    expect(toggle.attributes('aria-expanded')).toBe('true')
    const row = wrapper.find('#mobile-search')
    expect(row.find('form[role="search"]').exists()).toBe(true)

    await row.find('input[type="search"]').setValue('트랜잭션')
    await row.find('form').trigger('submit')
    await flushPromises()
    expect(wrapper.find('#mobile-search').exists()).toBe(false)
  })
})
