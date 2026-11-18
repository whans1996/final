import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import App from '@/App.vue'
import AnalysisListView from '@/views/AnalysisListView.vue'
import * as analysesApi from '@/api/analyses'
import { useAuthStore } from '@/stores/auth'
import { authGuard, routes } from '@/router'
import { apiError, mountAt } from './viewTestUtils'

vi.mock('@/api/analyses')

function login() {
  useAuthStore().setSession({
    accessToken: 'access-1',
    user: { id: 1, nickname: '코딩초보', level: 'JUNIOR', role: 'USER' },
  })
}

function item(partial) {
  return {
    id: 42,
    title: 'todo-app',
    projectName: 'todo-app',
    sourceType: 'ZIP',
    level: 'JUNIOR',
    status: 'DONE',
    progress: 100,
    isPublic: false,
    createdAt: '2026-11-24T14:20:00+09:00',
    ...partial,
  }
}

function page(items, { page = 1, total = items.length } = {}) {
  return { items, page, size: 20, total }
}

async function mountList() {
  const mounted = await mountAt(AnalysisListView, '/analyses', { beforeNavigate: login })
  await flushPromises()
  return mounted
}

describe('AnalysisListView (F-05a AC16)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('AC16 로그인하지 않았으면 인증 가드가 로그인 화면으로 보낸다', () => {
    const route = routes.find((r) => r.path === '/analyses')
    const to = { meta: route.meta, fullPath: '/analyses' }

    expect(authGuard(to, { isLoggedIn: false })).toEqual({
      name: 'login',
      query: { redirect: '/analyses' },
    })
  })

  it('AC16 내 분석을 카드로 보여 준다 (제목, 수준, 상태 배지, 진행 중이면 진행률, 요청일)', async () => {
    analysesApi.fetchAnalyses.mockResolvedValue(
      page([
        item({ id: 43, title: 'shop-api', level: 'SENIOR', status: 'SELECTING', progress: 25 }),
        item({ id: 42 }),
      ]),
    )

    const { wrapper } = await mountList()

    expect(analysesApi.fetchAnalyses).toHaveBeenCalledWith({ page: 1, size: 20 })
    const cards = wrapper.findAll('[data-analysis-id]')
    expect(cards).toHaveLength(2)
    expect(cards[0].text()).toContain('shop-api')
    expect(cards[0].text()).toContain('숙련')
    expect(cards[0].text()).toContain('주요 파일 선별 중')
    expect(cards[0].find('[role="progressbar"]').attributes('aria-valuenow')).toBe('25')
    expect(cards[0].text()).toContain('2026.11.24')
    expect(cards[1].text()).toContain('todo-app')
    expect(cards[1].text()).toContain('분석 완료')
    expect(cards[1].find('[role="progressbar"]').exists()).toBe(false)
  })

  it('AC16 카드를 누르면 /analyses/{id}로 간다', async () => {
    analysesApi.fetchAnalyses.mockResolvedValue(page([item({ id: 42 })]))

    const { wrapper } = await mountList()

    expect(wrapper.find('[data-analysis-id="42"]').attributes('href')).toBe('/analyses/42')
  })

  it('AC16 목록이 비면 안내와 첫 분석 요청하기 버튼을 보여 준다', async () => {
    analysesApi.fetchAnalyses.mockResolvedValue(page([]))

    const { wrapper } = await mountList()

    expect(wrapper.text()).toContain('아직 분석이 없습니다')
    const cta = wrapper.find('a[data-action="first-request"]')
    expect(cta.text()).toBe('첫 분석 요청하기')
    expect(cta.attributes('href')).toBe('/analyses/new')
  })

  it('AC16 불러오는 중에는 로딩 문구를, 실패하면 서버 메시지를 보여 준다', async () => {
    const message = '서버에서 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.'
    let reject
    analysesApi.fetchAnalyses.mockReturnValue(new Promise((_, r) => (reject = r)))

    const { wrapper } = await mountList()
    expect(wrapper.text()).toContain('불러오는 중')

    reject(apiError(500, { code: 'COMMON_500', message }))
    await flushPromises()
    expect(wrapper.find('[role="alert"]').text()).toBe(message)
  })

  it('AC16 20개 넘으면 이전·다음 페이지로 이동한다', async () => {
    const first = Array.from({ length: 20 }, (_, i) => item({ id: 100 - i }))
    analysesApi.fetchAnalyses
      .mockResolvedValueOnce(page(first, { page: 1, total: 21 }))
      .mockResolvedValueOnce(page([item({ id: 80 })], { page: 2, total: 21 }))
      .mockResolvedValueOnce(page(first, { page: 1, total: 21 }))

    const { wrapper } = await mountList()
    const prev = () => wrapper.find('button[data-action="prev"]')
    const next = () => wrapper.find('button[data-action="next"]')

    expect(prev().attributes('disabled')).toBeDefined()
    expect(next().attributes('disabled')).toBeUndefined()

    await next().trigger('click')
    await flushPromises()
    expect(analysesApi.fetchAnalyses).toHaveBeenLastCalledWith({ page: 2, size: 20 })
    expect(wrapper.findAll('[data-analysis-id]')).toHaveLength(1)
    expect(next().attributes('disabled')).toBeDefined()

    await prev().trigger('click')
    await flushPromises()
    expect(analysesApi.fetchAnalyses).toHaveBeenLastCalledWith({ page: 1, size: 20 })
  })

  it('AC16 20개 이하면 페이지 이동 버튼을 보여 주지 않는다', async () => {
    analysesApi.fetchAnalyses.mockResolvedValue(page([item({ id: 42 })]))

    const { wrapper } = await mountList()

    expect(wrapper.find('button[data-action="next"]').exists()).toBe(false)
  })

  it('AC16 로그인 헤더에 분석 요청·내 분석 링크가 있다', async () => {
    const { wrapper } = await mountAt(App, '/', { beforeNavigate: login })
    await flushPromises()

    const links = wrapper.findAll('header a').map((a) => [a.text(), a.attributes('href')])
    expect(links).toContainEqual(['분석 요청', '/analyses/new'])
    expect(links).toContainEqual(['내 분석', '/analyses'])
  })

  it('AC16 비로그인 헤더에는 분석 링크가 없다', async () => {
    const { wrapper } = await mountAt(App, '/')
    await flushPromises()

    expect(wrapper.find('header').text()).not.toContain('내 분석')
  })
})
