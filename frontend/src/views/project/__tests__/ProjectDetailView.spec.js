import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import ProjectDetailView from '@/views/project/ProjectDetailView.vue'
import * as projectsApi from '@/api/projects'
import { useAuthStore } from '@/stores/auth'
import { authGuard, routes } from '@/router'
import { apiError, deferred, mountAt } from '@/views/__tests__/viewTestUtils'
import { projectDetail } from './fixtures'

vi.mock('@/api/projects')

function login() {
  useAuthStore().setSession({
    accessToken: 'access-1',
    user: { id: 1, nickname: '코딩초보', level: 'JUNIOR', role: 'USER' },
  })
}

async function mountDetail(path = '/projects/12') {
  const mounted = await mountAt(ProjectDetailView, path, { beforeNavigate: login })
  await flushPromises()
  return mounted
}

const zipProject = () =>
  projectDetail({
    id: 13,
    name: 'shop.zip',
    sourceType: 'ZIP',
    sourceUrl: null,
    versions: [
      {
        id: 40,
        number: 1,
        commitSha: null,
        commitMessage: null,
        committedAt: null,
        createdAt: '2026-11-27T09:00:00+09:00',
      },
    ],
    analyses: [
      {
        id: 140,
        versionId: 40,
        level: 'SENIOR',
        status: 'DONE',
        quizStatus: 'READY',
        title: 'shop.zip',
        createdAt: '2026-11-27T09:00:00+09:00',
      },
    ],
  })

describe('ProjectDetailView (P-1a AC15)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    projectsApi.fetchLatestCommit.mockResolvedValue({
      status: 'UP_TO_DATE',
      latest: { sha: 'a'.repeat(40), message: 'feat: JWT 로그인', committedAt: null },
      analyzedSha: 'a'.repeat(40),
      checkedAt: '2026-11-29T10:00:00+09:00',
    })
  })

  it('AC15 로그인하지 않았으면 인증 가드가 로그인 화면으로 보낸다', () => {
    const route = routes.find((r) => r.path.startsWith('/projects/:id'))
    const to = { meta: route.meta, fullPath: '/projects/12' }

    expect(authGuard(to, { isLoggedIn: false })).toEqual({
      name: 'login',
      query: { redirect: '/projects/12' },
    })
  })

  it('AC15 버전별로 묶인 분석(번호 내림차순, 수준·상태)을 보고 누르면 분석 상세로 간다', async () => {
    projectsApi.fetchProject.mockResolvedValue(projectDetail())

    const { wrapper } = await mountDetail()

    expect(projectsApi.fetchProject).toHaveBeenCalledWith(12, expect.anything())
    expect(wrapper.find('h1').text()).toBe('todo-app')
    const versions = wrapper.findAll('[data-version-id]')
    expect(versions.map((v) => v.attributes('data-version-id'))).toEqual(['31', '30'])

    const v2 = versions[0]
    expect(v2.text()).toContain('v2')
    expect(v2.text()).toContain('feat: JWT 로그인')
    expect(v2.text()).toContain('9f2c4e1')
    const links = v2.findAll('a[data-analysis-id]')
    expect(links.map((a) => a.attributes('href'))).toEqual(['/analyses/128', '/analyses/129'])
    expect(links[0].text()).toContain('입문')
    expect(links[0].text()).toContain('분석 완료')
    expect(links[1].text()).toContain('숙련')
    expect(links[1].text()).toContain('설명 생성 중')

    expect(versions[1].text()).toContain('v1')
    expect(versions[1].text()).toContain('커밋 정보 없음')
  })

  it('AC15 아직 버전이 없는 분석(FETCHING·FAILED)은 따로 모아 보여 준다', async () => {
    const project = projectDetail()
    project.analyses.push({
      id: 130,
      versionId: null,
      level: 'JUNIOR',
      status: 'FETCHING',
      quizStatus: null,
      title: 'todo-app',
      createdAt: '2026-11-29T10:00:00+09:00',
    })
    projectsApi.fetchProject.mockResolvedValue(project)

    const { wrapper } = await mountDetail()

    const pending = wrapper.find('[data-section="no-version"]')
    expect(pending.text()).toContain('버전 정하는 중')
    expect(pending.find('a[data-analysis-id="130"]').attributes('href')).toBe('/analyses/130')
  })

  it('AC15 저장소 프로젝트는 새 커밋 배지와 "최신 커밋 분석" 버튼(→ /analyses/new?projectId=)이 있다', async () => {
    projectsApi.fetchProject.mockResolvedValue(projectDetail())

    const { wrapper } = await mountDetail()

    expect(projectsApi.fetchLatestCommit).toHaveBeenCalledWith(12, expect.anything())
    expect(wrapper.find('[data-commit="UP_TO_DATE"]').exists()).toBe(true)
    const action = wrapper.find('a[data-action="new-version"]')
    expect(action.text()).toContain('최신 커밋 분석')
    expect(action.attributes('href')).toBe('/analyses/new?projectId=12')
    expect(wrapper.find('a[data-action="source"]').attributes('href')).toBe(
      'https://github.com/octo/todo-app',
    )
  })

  it('AC15 ZIP 프로젝트는 배지를 부르지 않고 "ZIP 다시 올리기" 버튼이 있다', async () => {
    projectsApi.fetchProject.mockResolvedValue(zipProject())

    const { wrapper } = await mountDetail('/projects/13')

    expect(projectsApi.fetchLatestCommit).not.toHaveBeenCalled()
    const action = wrapper.find('a[data-action="new-version"]')
    expect(action.text()).toContain('ZIP 다시 올리기')
    expect(action.attributes('href')).toBe('/analyses/new?projectId=13')
    expect(wrapper.find('a[data-action="source"]').exists()).toBe(false)
  })

  it('AC15 배지 조회가 실패해도 상세는 그대로 보인다', async () => {
    projectsApi.fetchProject.mockResolvedValue(projectDetail())
    projectsApi.fetchLatestCommit.mockRejectedValue(apiError(500, { message: '서버 오류' }))

    const { wrapper } = await mountDetail()

    expect(wrapper.findAll('[data-version-id]')).toHaveLength(2)
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
  })

  it('AC15 404면 "프로젝트를 찾을 수 없어요"를 보여 준다', async () => {
    projectsApi.fetchProject.mockRejectedValue(
      apiError(404, { code: 'PROJECT_404', message: '프로젝트를 찾을 수 없습니다.' }),
    )

    const { wrapper } = await mountDetail()

    expect(wrapper.find('[data-message="not-found"]').text()).toContain('프로젝트를 찾을 수 없어요')
    expect(projectsApi.fetchLatestCommit).not.toHaveBeenCalled()
  })

  it('AC15 그 밖의 에러면 메시지와 다시 시도를 보여 준다', async () => {
    projectsApi.fetchProject
      .mockRejectedValueOnce(apiError(500, { code: 'COMMON_500', message: '서버 오류입니다.' }))
      .mockResolvedValueOnce(projectDetail())

    const { wrapper } = await mountDetail()

    expect(wrapper.find('[role="alert"]').text()).toContain('서버 오류입니다.')
    await wrapper.find('[role="alert"] button').trigger('click')
    await flushPromises()
    expect(wrapper.find('h1').text()).toBe('todo-app')
  })

  it('AC15 이름을 바꾸면 앞뒤 공백을 떼고 PATCH한 뒤 제목을 바꾼다', async () => {
    projectsApi.fetchProject.mockResolvedValue(projectDetail())
    projectsApi.updateProject.mockResolvedValue({ id: 12, name: '할 일 앱' })

    const { wrapper } = await mountDetail()
    await wrapper.find('[data-action="rename"]').trigger('click')
    await wrapper.find('input[name="name"]').setValue('  할 일 앱  ')
    await wrapper.find('form[data-form="rename"]').trigger('submit')
    await flushPromises()

    expect(projectsApi.updateProject).toHaveBeenCalledWith(12, { name: '할 일 앱' })
    expect(wrapper.find('h1').text()).toBe('할 일 앱')
    expect(wrapper.find('form[data-form="rename"]').exists()).toBe(false)
  })

  it('AC15 이름이 비었으면 보내지 않고, 서버 details.name은 입력 아래에 보여 준다', async () => {
    projectsApi.fetchProject.mockResolvedValue(projectDetail())
    projectsApi.updateProject.mockRejectedValue(
      apiError(400, {
        code: 'COMMON_400',
        message: '입력값이 올바르지 않습니다.',
        details: { name: '프로젝트 이름은 1~100자로 입력하세요.' },
      }),
    )

    const { wrapper } = await mountDetail()
    await wrapper.find('[data-action="rename"]').trigger('click')
    await wrapper.find('input[name="name"]').setValue('   ')
    expect(
      wrapper.find('form[data-form="rename"] button[type="submit"]').attributes(),
    ).toHaveProperty('disabled')

    await wrapper.find('input[name="name"]').setValue('x')
    await wrapper.find('form[data-form="rename"]').trigger('submit')
    await flushPromises()

    expect(wrapper.find('[data-error="name"]').text()).toBe('프로젝트 이름은 1~100자로 입력하세요.')
    expect(wrapper.find('input[name="name"]').attributes('aria-invalid')).toBe('true')
    // 수정 중에는 제목 대신 입력 칸이 보이고, 취소하면 원래 이름 그대로다
    await wrapper.find('[data-action="cancel-rename"]').trigger('click')
    expect(wrapper.find('h1').text()).toBe('todo-app')
  })

  it('AC15 다른 프로젝트로 옮기면 이전 응답은 버린다', async () => {
    const first = deferred()
    projectsApi.fetchProject.mockReturnValueOnce(first.promise).mockResolvedValueOnce(zipProject())

    const { wrapper, router } = await mountDetail('/projects/12')
    await router.push('/projects/13')
    await flushPromises()
    first.resolve(projectDetail())
    await flushPromises()

    expect(wrapper.find('h1').text()).toBe('shop.zip')
  })
})
