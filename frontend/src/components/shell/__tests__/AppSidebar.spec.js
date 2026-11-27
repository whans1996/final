import { beforeEach, describe, expect, it } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import AppSidebar from '@/components/shell/AppSidebar.vue'
import { SIDEBAR_KEY } from '@/components/shell/sidebarState'

const Blank = { template: '<div />' }
const paths = ['/dashboard', '/analyses', '/analyses/new', '/analyses/:id', '/concepts']

const groups = [
  {
    key: 'dashboard',
    label: '대시보드',
    items: [{ label: '대시보드', to: '/dashboard', icon: 'LayoutDashboard' }],
  },
  {
    key: 'learn',
    label: '학습',
    items: [
      { label: '개념 카드', to: '/concepts', icon: 'BookOpen' },
      { label: '분석 요청', to: '/analyses/new', icon: 'FilePlus2' },
      { label: '내 분석', to: '/analyses', icon: 'FileSearch' },
    ],
  },
]

async function mountAt(path) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: paths.map((p) => ({ path: p, component: Blank })),
  })
  router.push(path)
  await router.isReady()
  const wrapper = mount(AppSidebar, { props: { groups }, global: { plugins: [router] } })
  await flushPromises()
  return wrapper
}

const link = (wrapper, label) => wrapper.find(`a[aria-label="${label}"]`)
const current = (wrapper) =>
  wrapper.findAll('a[aria-current="page"]').map((a) => a.attributes('aria-label'))

describe('AppSidebar (F-29b AC2·AC3·AC4)', () => {
  beforeEach(() => localStorage.clear())

  it('AC2 그룹 제목과 아이콘·이름 메뉴를 그룹 순서대로 그린다', async () => {
    const wrapper = await mountAt('/dashboard')

    expect(wrapper.find('nav').attributes('aria-label')).toBe('주 메뉴')
    expect(wrapper.findAll('h2').map((h) => h.text())).toEqual(['대시보드', '학습'])
    expect(wrapper.findAll('a').map((a) => a.attributes('aria-label'))).toEqual([
      '대시보드',
      '개념 카드',
      '분석 요청',
      '내 분석',
    ])
    expect(link(wrapper, '개념 카드').attributes('href')).toBe('/concepts')
    expect(link(wrapper, '개념 카드').find('svg').attributes('aria-hidden')).toBe('true')
  })

  it('AC2 지금 경로의 메뉴에만 aria-current="page"가 붙는다', async () => {
    expect(current(await mountAt('/concepts'))).toEqual(['개념 카드'])
  })

  it('AC2 하위 경로(/analyses/12)는 상위 메뉴(내 분석)가 활성이다', async () => {
    expect(current(await mountAt('/analyses/12'))).toEqual(['내 분석'])
  })

  it('AC2 /analyses/new는 분석 요청만 활성이다 (가장 길게 맞는 메뉴)', async () => {
    expect(current(await mountAt('/analyses/new'))).toEqual(['분석 요청'])
  })

  it('AC3 접기 버튼을 누르면 이름이 숨고 title·aria-label로 남으며 localStorage에 저장된다', async () => {
    const wrapper = await mountAt('/dashboard')
    const toggle = wrapper.find('button[aria-controls]')

    expect(toggle.attributes('aria-expanded')).toBe('true')
    expect(toggle.attributes('aria-label')).toBe('메뉴 접기')
    expect(link(wrapper, '대시보드').attributes('title')).toBeUndefined()

    await toggle.trigger('click')

    expect(toggle.attributes('aria-expanded')).toBe('false')
    expect(toggle.attributes('aria-label')).toBe('메뉴 펼치기')
    expect(link(wrapper, '대시보드').attributes('title')).toBe('대시보드')
    expect(wrapper.find('aside').attributes('data-collapsed')).toBe('true')
    expect(localStorage.getItem(SIDEBAR_KEY)).toBe('collapsed')

    await toggle.trigger('click')
    expect(localStorage.getItem(SIDEBAR_KEY)).toBe('expanded')
  })

  it('AC3 저장된 접힘 상태로 다시 그리면(새로고침) 접힌 채로 시작한다', async () => {
    localStorage.setItem(SIDEBAR_KEY, 'collapsed')
    const wrapper = await mountAt('/dashboard')

    expect(wrapper.find('aside').attributes('data-collapsed')).toBe('true')
    expect(wrapper.find('button[aria-controls]').attributes('aria-expanded')).toBe('false')
  })

  it('AC4 아이콘만 보일 때 쓰는 이름 툴팁이 메뉴마다 있다 (640~1024는 CSS로 항상 아이콘만)', async () => {
    const wrapper = await mountAt('/dashboard')

    expect(wrapper.findAll('[data-tooltip]').map((t) => t.text())).toEqual([
      '대시보드',
      '개념 카드',
      '분석 요청',
      '내 분석',
    ])
  })
})
