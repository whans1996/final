import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import ProjectListView from '@/views/project/ProjectListView.vue'
import * as projectsApi from '@/api/projects'
import { useAuthStore } from '@/stores/auth'
import { authGuard, routes } from '@/router'
import { apiError, deferred, mountAt } from '@/views/__tests__/viewTestUtils'
import { projectSummary } from './fixtures'

vi.mock('@/api/projects')

function login() {
  useAuthStore().setSession({
    accessToken: 'access-1',
    user: { id: 1, nickname: '코딩초보', level: 'JUNIOR', role: 'USER' },
  })
}

function page(items, partial) {
  return { items, page: 1, size: 20, total: items.length, ...partial }
}

async function mountList() {
  const mounted = await mountAt(ProjectListView, '/projects', { beforeNavigate: login })
  await flushPromises()
  return mounted
}

describe('ProjectListView (P-1a AC14)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('AC14 로그인하지 않았으면 인증 가드가 로그인 화면으로 보낸다', () => {
    const route = routes.find((r) => r.path === '/projects')
    const to = { meta: route.meta, fullPath: '/projects' }

    expect(authGuard(to, { isLoggedIn: false })).toEqual({
      name: 'login',
      query: { redirect: '/projects' },
    })
  })

  it('AC14 메뉴에 "프로젝트" 항목이 있다', () => {
    const route = routes.find((r) => r.path === '/projects')

    expect(route.meta.nav).toMatchObject({ label: '프로젝트', show: 'user', group: 'learn' })
  })

  it('AC14 프로젝트 카드에 이름·저장소 표시·버전 수·최신 버전·수준 배지가 있고 상세로 간다', async () => {
    projectsApi.fetchProjects.mockResolvedValue(
      page([
        projectSummary(),
        projectSummary({
          id: 13,
          name: 'shop.zip',
          sourceType: 'ZIP',
          sourceUrl: null,
          versionCount: 1,
          latestVersion: {
            id: 40,
            number: 1,
            commitSha: null,
            commitMessage: null,
            committedAt: null,
            createdAt: '2026-11-27T09:00:00+09:00',
          },
          levels: ['SENIOR'],
        }),
      ]),
    )

    const { wrapper } = await mountList()

    expect(projectsApi.fetchProjects).toHaveBeenCalledWith({ page: 1, size: 20 }, expect.anything())
    const cards = wrapper.findAll('[data-project-id]')
    expect(cards).toHaveLength(2)

    const repo = wrapper.find('[data-project-id="12"]')
    expect(repo.attributes('href')).toBe('/projects/12')
    expect(repo.text()).toContain('todo-app')
    expect(repo.find('[data-source]').text()).toBe('저장소')
    expect(repo.text()).toContain('버전 2개')
    expect(repo.find('[data-section="latest-version"]').text()).toContain('v2')
    expect(repo.find('[data-section="latest-version"]').text()).toContain('feat: JWT 로그인')
    expect(repo.findAll('[data-level]').map((b) => b.text())).toEqual(['입문', '주니어'])

    const zip = wrapper.find('[data-project-id="13"]')
    expect(zip.find('[data-source]').text()).toBe('ZIP')
    expect(zip.find('[data-section="latest-version"]').text()).toContain('v1')
    expect(zip.findAll('[data-level]').map((b) => b.text())).toEqual(['숙련'])
  })

  it('AC14 최신 버전이 아직 없으면(첫 분석 진행 중) 버전 줄 대신 안내를 보여 준다', async () => {
    projectsApi.fetchProjects.mockResolvedValue(
      page([projectSummary({ versionCount: 0, latestVersion: null, levels: ['JUNIOR'] })]),
    )

    const { wrapper } = await mountList()

    expect(wrapper.find('[data-section="latest-version"]').text()).toContain('버전 정하는 중')
  })

  it('AC14 불러오는 동안 로딩을 보여 준다', async () => {
    const pending = deferred()
    projectsApi.fetchProjects.mockReturnValue(pending.promise)

    const { wrapper } = await mountList()

    expect(wrapper.text()).toContain('불러오는 중')
    pending.resolve(page([]))
    await flushPromises()
    expect(wrapper.text()).not.toContain('불러오는 중')
  })

  it('AC14 빈 목록이면 안내 문구와 분석 요청 링크를 보여 준다', async () => {
    projectsApi.fetchProjects.mockResolvedValue(page([]))

    const { wrapper } = await mountList()

    expect(wrapper.text()).toContain('아직 분석한 프로젝트가 없어요')
    expect(wrapper.find('a[data-action="first-request"]').attributes('href')).toBe('/analyses/new')
  })

  it('AC14 에러면 메시지와 다시 시도를 보여 주고, 다시 시도하면 다시 부른다', async () => {
    projectsApi.fetchProjects
      .mockRejectedValueOnce(apiError(500, { code: 'COMMON_500', message: '서버 오류입니다.' }))
      .mockResolvedValueOnce(page([projectSummary()]))

    const { wrapper } = await mountList()

    expect(wrapper.find('[role="alert"]').text()).toContain('서버 오류입니다.')
    await wrapper.find('[role="alert"] button').trigger('click')
    await flushPromises()
    expect(projectsApi.fetchProjects).toHaveBeenCalledTimes(2)
    expect(wrapper.findAll('[data-project-id]')).toHaveLength(1)
  })

  it('AC14 다음 쪽으로 넘기면 그 쪽을 부른다', async () => {
    projectsApi.fetchProjects
      .mockResolvedValueOnce(page([projectSummary()], { total: 21 }))
      .mockResolvedValueOnce(page([projectSummary({ id: 99 })], { page: 2, total: 21 }))

    const { wrapper } = await mountList()
    await wrapper.find('button[data-action="next"]').trigger('click')
    await flushPromises()

    expect(projectsApi.fetchProjects).toHaveBeenLastCalledWith(
      { page: 2, size: 20 },
      expect.anything(),
    )
    expect(wrapper.find('[data-project-id="99"]').exists()).toBe(true)
  })
})
