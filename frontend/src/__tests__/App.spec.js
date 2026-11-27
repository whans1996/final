import { afterEach, describe, expect, it } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import App from '@/App.vue'
import { routes } from '@/router'
import { useAuthStore } from '@/stores/auth'

const Page = { template: '<p id="page">본문</p>' }
// 실제 route에 더해 셸 선택만 보는 가짜 화면 (F-29b에는 bare를 쓰는 실제 route가 없다)
const testRoutes = [
  { path: '/test/bare', component: Page, meta: { layout: 'bare' } },
  { path: '/test/wide', component: Page, meta: { wide: true } },
  { path: '/test/plain', component: Page },
]

function loginAs(role) {
  useAuthStore().setSession({
    accessToken: 'access-1',
    user: { id: 1, nickname: '코딩초보', level: 'JUNIOR', role },
  })
}

async function mountApp(path, role) {
  setActivePinia(createPinia())
  if (role) loginAs(role)
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [...testRoutes, ...routes],
  })
  await router.push(path)
  await router.isReady()
  const wrapper = mount(App, { global: { plugins: [router] }, attachTo: document.body })
  await flushPromises()
  return wrapper
}

const sidebarLinks = (wrapper) =>
  wrapper.findAll('aside nav a').map((a) => a.attributes('aria-label'))
const topLinks = (wrapper) => wrapper.findAll('header nav a').map((a) => a.text())
const contentWidth = (wrapper) => wrapper.find('#main > div').classes()

describe('App 셸 선택 (F-29b AC6·AC7, F-00b AC8)', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('AC6 비회원은 옆 메뉴 없이 윗쪽 메뉴(개념 카드·커뮤니티·챌린지)와 로그인·회원가입을 본다', async () => {
    const wrapper = await mountApp('/concepts')

    expect(wrapper.find('aside').exists()).toBe(false)
    expect(wrapper.find('nav[aria-label="하단 탭"]').exists()).toBe(false)
    expect(topLinks(wrapper)).toEqual(['개념 카드', '커뮤니티', '챌린지'])
    expect(wrapper.find('header nav a[aria-current="page"]').text()).toBe('개념 카드')
    expect(wrapper.find('header a[href="/login"]').text()).toBe('로그인')
    expect(wrapper.find('header a[href="/signup"]').text()).toBe('회원가입')
    expect(wrapper.find('header button[aria-label="메뉴 열기"]').exists()).toBe(true)
  })

  it('AC2 회원은 공개 화면(개념 카드)에서도 회원 셸: 그룹 옆 메뉴·하단 탭·닉네임 메뉴 (관리자 없음)', async () => {
    const wrapper = await mountApp('/concepts', 'USER')

    expect(wrapper.find('header nav').exists()).toBe(false)
    expect(sidebarLinks(wrapper)).toEqual([
      '대시보드',
      '개념 카드',
      '분석 요청',
      '내 분석',
      '복습',
      '커뮤니티',
      '피드',
      '팀',
      '챌린지',
    ])
    expect(wrapper.findAll('aside h2').map((h) => h.text())).toEqual(['대시보드', '학습', '함께'])
    expect(wrapper.find('aside a[aria-current="page"]').attributes('aria-label')).toBe('개념 카드')
    expect(wrapper.find('nav[aria-label="하단 탭"]').exists()).toBe(true)
    expect(wrapper.find('header button[aria-label="내 메뉴"]').text()).toContain('코딩초보')
    expect(wrapper.find('a[href="/admin"]').exists()).toBe(false)
    expect(wrapper.find('header a[href="/login"]').exists()).toBe(false)
  })

  it('AC13 ADMIN이 /admin을 열면 회원 셸 본문 안에 관리자 사이드 메뉴(AdminLayout)가 들어간다', async () => {
    const wrapper = await mountApp('/admin', 'ADMIN')

    expect(wrapper.find('aside nav[aria-label="주 메뉴"]').exists()).toBe(true)
    expect(wrapper.find('#main nav[aria-label="관리 메뉴"]').exists()).toBe(true)
  })

  it('AC7 meta.layout이 bare면 셸 없이 본문만 그린다', async () => {
    const wrapper = await mountApp('/test/bare', 'USER')

    expect(wrapper.find('#page').exists()).toBe(true)
    expect(wrapper.find('header').exists()).toBe(false)
    expect(wrapper.find('aside').exists()).toBe(false)
  })

  it('AC7 본문 최대 폭은 meta.wide면 1440px, 아니면 1080px (회원·비회원 모두)', async () => {
    expect(contentWidth(await mountApp('/test/wide', 'USER'))).toContain('max-w-[1440px]')
    document.body.innerHTML = ''
    expect(contentWidth(await mountApp('/test/plain', 'USER'))).toContain('max-w-[1080px]')
    document.body.innerHTML = ''
    expect(contentWidth(await mountApp('/test/wide'))).toContain('max-w-[1440px]')
  })

  it('AC7 /analyses/:id와 팀 분석 상세는 wide다', () => {
    const meta = (name) => routes.find((r) => r.name === name)?.meta
    expect(meta('analysis-detail').wide).toBe(true)
    expect(meta('team-understanding').wide).toBe(true)
  })

  it('AC14 첫 Tab 대상은 본문으로 건너뛰기 링크이고 #main으로 간다', async () => {
    const wrapper = await mountApp('/concepts', 'USER')
    const first = wrapper.find('a')

    expect(first.text()).toBe('본문으로 건너뛰기')
    expect(first.attributes('href')).toBe('#main')
    expect(wrapper.find('main#main').attributes('tabindex')).toBe('-1')
  })
})
