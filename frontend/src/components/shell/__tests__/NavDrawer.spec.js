import { afterEach, describe, expect, it } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, h, nextTick, ref } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import NavDrawer from '@/components/shell/NavDrawer.vue'

const Blank = { template: '<div />' }
const groups = [
  {
    key: 'learn',
    label: '학습',
    items: [{ label: '분석 요청', to: '/analyses/new', icon: 'FilePlus2' }],
  },
  { key: 'together', label: '함께', items: [{ label: '팀', to: '/teams', icon: 'Users' }] },
]

// 연 버튼과 드로어를 함께 그리는 셸을 흉내 낸다. 슬롯에는 셸마다 다른 항목(계정 메뉴)이 들어간다
async function mountHost() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: ['/', '/analyses/new', '/teams', '/me'].map((path) => ({ path, component: Blank })),
  })
  router.push('/')
  await router.isReady()
  const open = ref(false)
  const extra = ({ rowClass }) => h('a', { href: '/me', class: rowClass }, '프로필')
  const Host = defineComponent({
    setup: () => () =>
      h('div', [
        h('button', { id: 'opener', onClick: () => (open.value = true) }, '더보기'),
        h(
          NavDrawer,
          { open: open.value, 'onUpdate:open': (v) => (open.value = v), groups },
          { default: extra },
        ),
      ]),
  })
  const wrapper = mount(Host, { attachTo: document.body, global: { plugins: [router] } })
  const opener = document.getElementById('opener')
  opener.focus()
  opener.click()
  await flushPromises()
  await nextTick()
  return { wrapper, open, opener }
}

const dialog = () => document.querySelector('[role="dialog"]')

describe('NavDrawer (F-29b AC5·AC6·AC14)', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('AC5 열면 그룹별 메뉴와 슬롯 항목이 보이고 포커스가 드로어 안으로 들어간다', async () => {
    const { wrapper } = await mountHost()

    expect(dialog()).not.toBeNull()
    expect([...dialog().querySelectorAll('h3')].map((el) => el.textContent.trim())).toEqual([
      '학습',
      '함께',
    ])
    expect([...dialog().querySelectorAll('a')].map((a) => a.textContent.trim())).toEqual([
      '분석 요청',
      '팀',
      '프로필',
    ])
    expect(dialog().contains(document.activeElement)).toBe(true)
    wrapper.unmount()
  })

  it('AC5 메뉴를 고르면 닫힌다', async () => {
    const { wrapper, open } = await mountHost()

    dialog().querySelector('a').click()
    await flushPromises()

    expect(open.value).toBe(false)
    wrapper.unmount()
  })

  it('AC5 Esc를 누르면 닫히고 포커스가 연 버튼으로 돌아간다', async () => {
    const { wrapper, open, opener } = await mountHost()

    document.activeElement.dispatchEvent(
      new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }),
    )
    await flushPromises()
    await nextTick()

    expect(open.value).toBe(false)
    expect(document.activeElement).toBe(opener)
    wrapper.unmount()
  })
})
