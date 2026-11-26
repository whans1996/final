import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import AnalysisDetailView from '@/views/AnalysisDetailView.vue'
import * as analysesApi from '@/api/analyses'
import * as quizzesApi from '@/api/quizzes'
import * as challengesApi from '@/api/challenges'
import { myChallenge, page } from '@/components/challenge/__tests__/fixtures'
import { useAuthStore } from '@/stores/auth'
import { authGuard, routes } from '@/router'
import { apiError, mountAt } from './viewTestUtils'

vi.mock('@/api/analyses')
vi.mock('@/api/quizzes')
vi.mock('@/api/challenges')
const mermaid = vi.hoisted(() => ({ initialize: vi.fn(), render: vi.fn() }))
vi.mock('mermaid', () => ({ default: mermaid }))

function login() {
  useAuthStore().setSession({
    accessToken: 'access-1',
    user: { id: 1, nickname: '코딩초보', level: 'JUNIOR', role: 'USER' },
  })
}

function status(partial) {
  return {
    analysisId: 42,
    status: 'FETCHING',
    progress: 5,
    errorCode: null,
    errorMessage: null,
    updatedAt: '2026-11-24T14:20:01+09:00',
    ...partial,
  }
}

async function mountDetail(path = '/analyses/42') {
  const mounted = await mountAt(AnalysisDetailView, path, { beforeNavigate: login })
  await flushPromises()
  return mounted
}

async function advance(ms) {
  await vi.advanceTimersByTimeAsync(ms)
  await flushPromises()
}

describe('AnalysisDetailView (F-05a AC15)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('AC15 로그인하지 않았으면 인증 가드가 로그인 화면으로 보낸다', () => {
    const route = routes.find((r) => r.path.startsWith('/analyses/:id'))
    const to = { meta: route.meta, fullPath: '/analyses/42' }

    expect(authGuard(to, { isLoggedIn: false })).toEqual({
      name: 'login',
      query: { redirect: '/analyses/42' },
    })
  })

  it('AC15 상태를 바로 1회, 이후 2초마다 조회하며 단계 라벨과 진행률 막대를 보여 준다', async () => {
    analysesApi.fetchAnalysisStatus
      .mockResolvedValueOnce(status({ status: 'FETCHING', progress: 5 }))
      .mockResolvedValueOnce(status({ status: 'FILTERING', progress: 15 }))
      .mockResolvedValue(status({ status: 'SELECTING', progress: 25 }))

    const { wrapper } = await mountDetail()

    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledTimes(1)
    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledWith(42)
    expect(wrapper.text()).toContain('코드 수집 중')
    expect(wrapper.find('[role="progressbar"]').attributes('aria-valuenow')).toBe('5')

    await advance(1999)
    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledTimes(1)
    await advance(1)
    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('파일 검사 중')
    expect(wrapper.find('[role="progressbar"]').attributes('aria-valuenow')).toBe('15')

    await advance(2000)
    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledTimes(3)
    expect(wrapper.text()).toContain('주요 파일 선별 중')
    wrapper.unmount()
  })

  it('AC15 DONE이면 완료 안내를 보여 주고 폴링을 멈춘다', async () => {
    analysesApi.fetchAnalysisStatus
      .mockResolvedValueOnce(status({ status: 'SELECTING', progress: 25 }))
      .mockResolvedValue(status({ status: 'DONE', progress: 100 }))

    const { wrapper } = await mountDetail()
    await advance(2000)

    expect(wrapper.find('[data-message="done"]').text()).toContain('분석이 끝났습니다')
    await advance(10000)
    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledTimes(2)
  })

  it('AC15 FAILED면 errorMessage와 다시 요청하기 링크를 보여 주고 폴링을 멈춘다', async () => {
    const message = '지원하는 코드 파일이 312개입니다. 200개 이하로 줄여서 다시 올려 주세요.'
    analysesApi.fetchAnalysisStatus.mockResolvedValue(
      status({ status: 'FAILED', progress: 15, errorCode: 'ANALYSIS_413', errorMessage: message }),
    )

    const { wrapper } = await mountDetail()

    expect(wrapper.find('[data-message="failed"]').text()).toContain(message)
    expect(wrapper.find('a[data-action="retry"]').attributes('href')).toBe('/analyses/new')
    expect(wrapper.text()).toContain('분석 실패')
    await advance(10000)
    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledTimes(1)
  })

  it('AC15 화면을 떠나면 폴링을 멈춘다', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(status({ status: 'FILTERING', progress: 15 }))

    const { wrapper } = await mountDetail()
    wrapper.unmount()
    await advance(10000)

    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledTimes(1)
  })

  it('AC15 404면 "분석을 찾을 수 없습니다."를 보여 주고 멈춘다', async () => {
    analysesApi.fetchAnalysisStatus.mockRejectedValue(
      apiError(404, { code: 'ANALYSIS_404', message: '분석을 찾을 수 없습니다.', details: null }),
    )

    const { wrapper } = await mountDetail()

    expect(wrapper.text()).toContain('분석을 찾을 수 없습니다.')
    expect(wrapper.find('[role="progressbar"]').exists()).toBe(false)
    await advance(10000)
    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledTimes(1)
  })

  it('AC15 그 밖의 에러는 서버 메시지를 보여 주고 다음 주기에 다시 조회한다', async () => {
    const message = '서버에서 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.'
    analysesApi.fetchAnalysisStatus
      .mockRejectedValueOnce(apiError(500, { code: 'COMMON_500', message }))
      .mockResolvedValue(status({ status: 'FILTERING', progress: 15 }))

    const { wrapper } = await mountDetail()
    expect(wrapper.find('[role="alert"]').text()).toBe(message)

    await advance(2000)
    expect(analysesApi.fetchAnalysisStatus).toHaveBeenCalledTimes(2)
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('파일 검사 중')
    wrapper.unmount()
  })

  it('AC15 다른 분석으로 이동하면 새 id로 다시 폴링하고 이전 폴링은 멈춘다', async () => {
    analysesApi.fetchAnalysisStatus.mockImplementation(async (id) =>
      status({ analysisId: id, status: 'FILTERING', progress: 15 }),
    )

    const { wrapper, router } = await mountDetail('/analyses/42')
    await router.push('/analyses/43')
    await flushPromises()
    await advance(2000)

    const ids = analysesApi.fetchAnalysisStatus.mock.calls.map(([id]) => id)
    expect(ids).toEqual([42, 43, 43])
    wrapper.unmount()
  })
})

function detail(partial) {
  return {
    id: 42,
    title: 'todo-app',
    memo: null,
    mine: true,
    owner: { id: 1, nickname: '코딩초보' },
    projectName: 'todo-app',
    sourceType: 'ZIP',
    level: 'JUNIOR',
    status: 'DONE',
    progress: 100,
    isPublic: false,
    errorCode: null,
    errorMessage: null,
    createdAt: '2026-11-24T14:20:00+09:00',
    fileCount: 2,
    explainedFileCount: 2,
    liteMode: false,
    overview: {
      summary: '할 일을 등록하는 Todo 서비스입니다.',
      techStack: ['Spring Boot'],
      layers: [{ layer: 'controller', role: 'HTTP 요청을 받습니다.' }],
      readingOrder: [{ fileId: 101, path: 'src/TodoController.java', reason: '먼저 봅니다.' }],
      apis: [{ method: 'GET', path: '/api/todos', fileId: 101 }],
    },
    ...partial,
  }
}

const files = [
  {
    id: 101,
    path: 'src/TodoController.java',
    layer: 'controller',
    role: '컨트롤러',
    summary: '설명',
  },
  { id: 102, path: 'src/TodoService.java', layer: 'service', role: '서비스', summary: '설명' },
]

describe('AnalysisDetailView 결과 탭 (F-06 AC13~AC15)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] })
    analysesApi.fetchAnalysisFiles.mockResolvedValue(files)
    analysesApi.fetchAnalysisFile.mockImplementation(async (_, fileId) => ({
      ...files.find((f) => f.id === fileId),
      content: 'class A {}',
    }))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('AC13 처음부터 DONE이면 상세를 1회 불러와 제목과 개요 탭(기본)을 보여 준다', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(status({ status: 'DONE', progress: 100 }))
    analysesApi.fetchAnalysis.mockResolvedValue(detail())

    const { wrapper } = await mountDetail()

    expect(analysesApi.fetchAnalysis).toHaveBeenCalledTimes(1)
    expect(analysesApi.fetchAnalysis).toHaveBeenCalledWith(42)
    expect(wrapper.find('h1').text()).toBe('todo-app')
    const tabs = wrapper.findAll('[role="tab"]')
    expect(tabs.map((t) => t.text())).toEqual(['개요', '구조', '파일', '실행 흐름', '개념', '퀴즈'])
    expect(tabs[0].attributes('aria-selected')).toBe('true')
    expect(wrapper.find('[data-section="summary"]').text()).toContain('Todo 서비스')
    expect(analysesApi.fetchAnalysisFiles).not.toHaveBeenCalled()
  })

  it('F-27 AC11 liteMode면 탭 위에 간이 모드 배너를, 아니면 배너를 보여 주지 않는다', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(status({ status: 'DONE', progress: 100 }))
    analysesApi.fetchAnalysis.mockResolvedValue(detail({ liteMode: true }))

    const { wrapper } = await mountDetail()

    const banner = wrapper.find('[data-message="lite-mode"]')
    expect(banner.exists()).toBe(true)
    expect(banner.attributes('role')).toBe('status')
    expect(banner.text()).toBe(
      '일부 결과는 간이 모드(서버의 로컬 AI)로 만들어 설명이 짧거나 덜 정확할 수 있어요.',
    )
    const html = wrapper.find('.result').html()
    expect(html.indexOf('data-message="lite-mode"')).toBeLessThan(html.indexOf('role="tablist"'))

    analysesApi.fetchAnalysis.mockResolvedValue(detail({ liteMode: false }))
    const second = await mountDetail()
    expect(second.wrapper.find('[data-message="lite-mode"]').exists()).toBe(false)
  })

  it('AC13 폴링 중 DONE으로 바뀌면 그때 상세를 다시 불러와 결과 탭을 보여 준다', async () => {
    analysesApi.fetchAnalysisStatus
      .mockResolvedValueOnce(status({ status: 'EXPLAINING', progress: 45 }))
      .mockResolvedValue(status({ status: 'DONE', progress: 100 }))
    analysesApi.fetchAnalysis
      .mockResolvedValueOnce(detail({ status: 'EXPLAINING', progress: 45, overview: null }))
      .mockResolvedValue(detail())

    const { wrapper } = await mountDetail()
    expect(wrapper.text()).toContain('설명 생성 중')
    // F-19 AC13: 진행 중에도 관리 영역을 위해 상세를 1회 불러온다
    expect(analysesApi.fetchAnalysis).toHaveBeenCalledTimes(1)
    expect(wrapper.find('[role="tab"]').exists()).toBe(false)

    await advance(2000)
    await advance(10000)

    expect(analysesApi.fetchAnalysis).toHaveBeenCalledTimes(2)
    expect(wrapper.find('h1').text()).toBe('todo-app')
    expect(wrapper.findAll('[role="tab"]')).toHaveLength(6)
  })

  it('AC13 FAILED면 결과 탭 없이 F-05a 실패 화면 그대로다', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(
      status({
        status: 'FAILED',
        progress: 40,
        errorCode: 'LLM_503',
        errorMessage: 'AI 응답 실패',
      }),
    )
    // F-19부터 FAILED도 관리 영역(삭제)을 위해 상세를 1회 불러온다
    analysesApi.fetchAnalysis.mockResolvedValue(detail({ status: 'FAILED', overview: null }))

    const { wrapper } = await mountDetail()

    expect(analysesApi.fetchAnalysis).toHaveBeenCalledTimes(1)
    expect(wrapper.find('[data-message="failed"]').text()).toContain('AI 응답 실패')
    expect(wrapper.find('[role="tab"]').exists()).toBe(false)
  })

  it('AC15 상세가 실패하면 서버 message와 "다시 시도" 버튼을 보여 주고, 누르면 다시 불러온다', async () => {
    const message = '서버에서 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.'
    analysesApi.fetchAnalysisStatus.mockResolvedValue(status({ status: 'DONE', progress: 100 }))
    analysesApi.fetchAnalysis
      .mockRejectedValueOnce(apiError(500, { code: 'COMMON_500', message }))
      .mockResolvedValue(detail())

    const { wrapper } = await mountDetail()
    expect(wrapper.find('[role="alert"]').text()).toBe(message)

    await wrapper.find('[data-action="retry-detail"]').trigger('click')
    await flushPromises()

    expect(analysesApi.fetchAnalysis).toHaveBeenCalledTimes(2)
    expect(wrapper.find('h1').text()).toBe('todo-app')
  })

  it('AC14 "파일" 탭을 누르면 쿼리에 tab=files가 남고 파일 목록을 보여 준다', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(status({ status: 'DONE', progress: 100 }))
    analysesApi.fetchAnalysis.mockResolvedValue(detail())
    const { wrapper, router } = await mountDetail()

    await wrapper.findAll('[role="tab"]')[2].trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.query).toEqual({ tab: 'files' })
    expect(wrapper.findAll('[role="tab"]')[2].attributes('aria-selected')).toBe('true')
    expect(wrapper.findAll('[data-file-id]')).toHaveLength(2)
  })

  it('AC14 ?tab=files&file=101로 열면 파일 탭과 선택 파일을 복원하고, 뒤로 가기로 이전 선택으로 돌아간다', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(status({ status: 'DONE', progress: 100 }))
    analysesApi.fetchAnalysis.mockResolvedValue(detail())

    const { wrapper, router } = await mountDetail('/analyses/42?tab=files&file=101')

    expect(analysesApi.fetchAnalysisFile).toHaveBeenCalledWith(42, 101)
    expect(wrapper.find('[data-file-id="101"]').attributes('aria-current')).toBe('true')

    await wrapper.find('[data-file-id="102"]').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual({ tab: 'files', file: '102' })
    expect(analysesApi.fetchAnalysisFile).toHaveBeenLastCalledWith(42, 102)

    router.back()
    await flushPromises()
    await advance(0)
    expect(router.currentRoute.value.query).toEqual({ tab: 'files', file: '101' })
    expect(analysesApi.fetchAnalysisFile).toHaveBeenLastCalledWith(42, 101)
    expect(analysesApi.fetchAnalysis).toHaveBeenCalledTimes(1)
  })

  it('AC13 개요의 읽는 순서를 누르면 파일 탭에서 그 파일을 선택한다', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(status({ status: 'DONE', progress: 100 }))
    analysesApi.fetchAnalysis.mockResolvedValue(detail())
    const { wrapper, router } = await mountDetail()

    await wrapper.find('[data-section="reading-order"] a').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.query).toEqual({ tab: 'files', file: '101' })
    expect(analysesApi.fetchAnalysisFile).toHaveBeenCalledWith(42, 101)
  })
  it('F-07 AC14 ?tab=flows로 열면 실행 흐름 탭에서 개요 apis를 보여 주고, 단계 경로를 누르면 파일 탭으로 간다', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(status({ status: 'DONE', progress: 100 }))
    analysesApi.fetchAnalysis.mockResolvedValue(detail())
    analysesApi.fetchAnalysisFlows.mockResolvedValue([
      {
        id: 7,
        method: 'GET',
        path: '/api/todos',
        status: 'DONE',
        errorCode: null,
        errorMessage: null,
        summary: '목록을 조회합니다.',
        steps: [
          {
            no: 1,
            layer: 'controller',
            fileId: 101,
            path: 'src/TodoController.java',
            symbol: 'TodoController.list',
            title: '요청 받기',
            description: '설명',
          },
        ],
        createdAt: '2026-12-09T10:00:00+09:00',
      },
    ])

    const { wrapper, router } = await mountDetail('/analyses/42?tab=flows')

    expect(wrapper.findAll('[role="tab"]')[3].attributes('aria-selected')).toBe('true')
    expect(analysesApi.fetchAnalysisFlows).toHaveBeenCalledWith(42)
    expect(wrapper.find('input[data-api="GET /api/todos"]').exists()).toBe(true)

    await wrapper.find('[data-action="open-file"]').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual({ tab: 'files', file: '101' })
  })

  it('F-21a AC14 구조 탭은 개요 다음이고, ?tab=structure로 열면 복원하며 연결 목록의 파일을 누르면 파일 탭으로 간다', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(status({ status: 'DONE', progress: 100 }))
    analysesApi.fetchAnalysis.mockResolvedValue(detail())
    analysesApi.fetchAnalysisStructure.mockResolvedValue({
      nodes: [
        { fileId: 101, path: 'src/A.java', name: 'A.java', layer: 'controller', role: null },
        { fileId: 102, path: 'src/B.java', name: 'B.java', layer: 'service', role: null },
      ],
      edges: [{ from: 101, to: 102, type: 'IMPORT', line: 4 }],
      unresolved: [],
      edgesTruncated: false,
      unresolvedTruncated: false,
    })
    mermaid.render.mockResolvedValue({ svg: '<svg></svg>' })

    const { wrapper, router } = await mountDetail('/analyses/42?tab=structure')

    const tabs = wrapper.findAll('[role="tab"]')
    expect(tabs[1].text()).toBe('구조')
    expect(tabs[1].attributes('aria-selected')).toBe('true')
    expect(analysesApi.fetchAnalysisStructure).toHaveBeenCalledWith(42)
    expect(mermaid.render).toHaveBeenCalledTimes(1)

    await wrapper.find('details[data-section="edges"] li a').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual({ tab: 'files', file: '101' })
  })

  it('F-21a AC14 남의 공개 분석에서도 구조 탭을 연다', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(status({ status: 'DONE', progress: 100 }))
    analysesApi.fetchAnalysis.mockResolvedValue(
      detail({ mine: false, owner: { id: 7, nickname: 'vibecoder' } }),
    )
    analysesApi.fetchAnalysisStructure.mockResolvedValue({
      nodes: [],
      edges: [],
      unresolved: [],
      edgesTruncated: false,
      unresolvedTruncated: false,
    })

    const { wrapper } = await mountDetail('/analyses/42?tab=structure')

    expect(wrapper.find('[role="tab"][aria-selected="true"]').text()).toBe('구조')
    expect(wrapper.find('[data-message="no-edges"]').exists()).toBe(true)
  })

  it('F-08 AC8 개념 탭은 실행 흐름 다음이고, ?tab=concepts로 열면 복원하며 위치를 누르면 파일 탭으로 간다', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(status({ status: 'DONE', progress: 100 }))
    analysesApi.fetchAnalysis.mockResolvedValue(detail())
    analysesApi.fetchAnalysisConcepts.mockResolvedValue({
      items: [
        {
          concept: {
            id: 12,
            name: '의존성 주입 (DI)',
            category: 'spring',
            difficulty: 'BEGINNER',
            summary: '요약',
            avgRating: 0,
            wishCount: 0,
          },
          fileId: 102,
          filePath: 'src/TodoService.java',
          lineHint: 'L8',
          contextNote: '생성자로 받습니다.',
        },
      ],
      pendingCount: 0,
    })

    const { wrapper, router } = await mountDetail('/analyses/42?tab=concepts')

    const tabs = wrapper.findAll('[role="tab"]')
    expect(tabs.map((t) => t.text())).toEqual(['개요', '구조', '파일', '실행 흐름', '개념', '퀴즈'])
    expect(tabs[4].attributes('aria-selected')).toBe('true')
    expect(analysesApi.fetchAnalysisConcepts).toHaveBeenCalledWith(42)
    expect(wrapper.find('[data-concept-id="12"]').exists()).toBe(true)

    await wrapper.find('[data-action="open-file"]').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual({ tab: 'files', file: '102' })
  })

  it('F-14 AC9 퀴즈 탭은 개념 다음이고, ?tab=quiz로 열면 복원해 문항 수와 풀이 링크를 보여 준다', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(status({ status: 'DONE', progress: 100 }))
    analysesApi.fetchAnalysis.mockResolvedValue(detail())
    quizzesApi.fetchAnalysisQuiz.mockResolvedValue({
      quizId: 3,
      analysisId: 42,
      questions: [1, 2, 3, 4, 5].map((n) => ({
        id: n,
        no: n,
        question: 'q',
        choices: ['a', 'b', 'c', 'd'],
      })),
      lastAttempt: null,
    })

    const { wrapper } = await mountDetail('/analyses/42?tab=quiz')

    const tabs = wrapper.findAll('[role="tab"]')
    expect(tabs.map((t) => t.text())).toEqual(['개요', '구조', '파일', '실행 흐름', '개념', '퀴즈'])
    expect(tabs[5].attributes('aria-selected')).toBe('true')
    expect(quizzesApi.fetchAnalysisQuiz).toHaveBeenCalledWith(42)
    expect(wrapper.find('[data-message="count"]').text()).toBe('객관식 5문항')
    expect(wrapper.find('[data-action="start-quiz"]').attributes('href')).toBe('/analyses/42/quiz')
  })
})

describe('AnalysisDetailView 저장소 링크 (F-05b AC14)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('AC14 URL 분석이면 제목 근처에 새 탭으로 여는 "저장소 보기" 링크가 있다', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(status({ status: 'DONE', progress: 100 }))
    analysesApi.fetchAnalysis.mockResolvedValue(
      detail({ sourceType: 'URL', sourceUrl: 'https://github.com/octo/todo-app' }),
    )

    const { wrapper } = await mountDetail()

    const link = wrapper.find('[data-action="source"]')
    expect(link.exists()).toBe(true)
    expect(link.text()).toBe('저장소 보기')
    expect(link.attributes('href')).toBe('https://github.com/octo/todo-app')
    expect(link.attributes('target')).toBe('_blank')
    expect(link.attributes('rel')).toBe('noopener noreferrer')
  })

  it('AC14 ZIP 분석에는 링크가 없다', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(status({ status: 'DONE', progress: 100 }))
    analysesApi.fetchAnalysis.mockResolvedValue(detail({ sourceUrl: null }))

    const { wrapper } = await mountDetail()

    expect(wrapper.find('h1').text()).toBe('todo-app')
    expect(wrapper.find('[data-action="source"]').exists()).toBe(false)
  })

  it('AC14 저장소 수집에 실패하면 errorMessage와 다시 요청하기를 보여 준다', async () => {
    const message =
      '저장소를 찾을 수 없습니다. 공개 저장소인지, 주소가 맞는지, 기본 브랜치에 코드가 있는지 확인해 주세요.'
    analysesApi.fetchAnalysisStatus.mockResolvedValue(
      status({ status: 'FAILED', progress: 5, errorCode: 'ANALYSIS_004', errorMessage: message }),
    )

    const { wrapper } = await mountDetail()

    expect(wrapper.find('[data-message="failed"]').text()).toContain(message)
    expect(wrapper.find('[data-action="retry"]').text()).toBe('다시 요청하기')
  })
})

describe('AnalysisDetailView 분석 관리·공개 분석 (F-19)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] })
    analysesApi.fetchAnalysisFlows.mockResolvedValue([])
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('AC13 주인이면 관리 영역, 메모(줄바꿈 유지), DONE일 때 "커뮤니티에 질문하기" 링크를 보여 준다', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(status({ status: 'DONE', progress: 100 }))
    analysesApi.fetchAnalysis.mockResolvedValue(detail({ memo: 'JWT 필터\n순서 다시 보기' }))

    const { wrapper } = await mountDetail()

    expect(wrapper.find('[aria-label="분석 관리"]').exists()).toBe(true)
    expect(wrapper.find('[data-section="memo"]').text()).toBe('JWT 필터\n순서 다시 보기')
    expect(wrapper.find('[data-section="owner"]').exists()).toBe(false)
    const ask = wrapper.find('[data-action="ask-community"]')
    expect(ask.text()).toBe('커뮤니티에 질문하기')
    expect(ask.attributes('href')).toBe('/community/write?analysisId=42')
  })

  it('#211 내 분석이 DONE이면 진행 중인 분석 챌린지 달성도를 보여 준다', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(status({ status: 'DONE', progress: 100 }))
    analysesApi.fetchAnalysis.mockResolvedValue(detail())
    challengesApi.fetchMyChallenges.mockResolvedValue(
      page([myChallenge(5, { goalType: 'ANALYSIS_COUNT', goalValue: 3, progress: 2 })]),
    )

    const { wrapper } = await mountDetail()

    expect(wrapper.find('[data-challenge-id="5"]').text()).toContain('2 / 3회')
  })

  it('#211 남의 분석이거나 진행 중이면 챌린지 달성도를 부르지 않는다', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(status({ status: 'DONE', progress: 100 }))
    analysesApi.fetchAnalysis.mockResolvedValue(
      detail({ mine: false, owner: { id: 7, nickname: 'vibecoder' } }),
    )
    await mountDetail()

    analysesApi.fetchAnalysisStatus.mockResolvedValue(
      status({ status: 'EXPLAINING', progress: 45 }),
    )
    analysesApi.fetchAnalysis.mockResolvedValue(detail({ status: 'EXPLAINING', overview: null }))
    await mountDetail()

    expect(challengesApi.fetchMyChallenges).not.toHaveBeenCalled()
  })

  it('AC13 FAILED면 관리 영역(삭제)은 있고 질문 링크는 없다', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(
      status({ status: 'FAILED', progress: 40, errorCode: 'LLM_503', errorMessage: '실패' }),
    )
    analysesApi.fetchAnalysis.mockResolvedValue(detail({ status: 'FAILED', overview: null }))

    const { wrapper } = await mountDetail()

    expect(wrapper.find('[data-action="delete"]').exists()).toBe(true)
    expect(wrapper.find('[data-action="ask-community"]').exists()).toBe(false)
  })

  it('AC13 진행 중이면 관리 영역에서 제목·메모를 고칠 수 있고, 공개는 꺼져 있으며 삭제·질문은 없다', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(
      status({ status: 'EXPLAINING', progress: 45 }),
    )
    analysesApi.fetchAnalysis.mockResolvedValue(
      detail({ status: 'EXPLAINING', progress: 45, overview: null, memo: '나중에 볼 것' }),
    )

    const { wrapper } = await mountDetail()

    expect(analysesApi.fetchAnalysis).toHaveBeenCalledTimes(1)
    expect(wrapper.find('[aria-label="분석 관리"]').exists()).toBe(true)
    expect(wrapper.find('[data-action="edit"]').exists()).toBe(true)
    expect(wrapper.find('[data-action="visibility"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('[data-action="delete"]').exists()).toBe(false)
    expect(wrapper.find('[data-action="ask-community"]').exists()).toBe(false)
    expect(wrapper.find('[data-section="memo"]').text()).toBe('나중에 볼 것')
    // 진행 상태 화면은 그대로다
    expect(wrapper.text()).toContain('설명 생성 중')
  })

  it('AC13 진행 중에 상세를 못 불러오면 서버 message와 "다시 시도" 버튼을 보여 준다', async () => {
    const message = '서버에서 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.'
    analysesApi.fetchAnalysisStatus.mockResolvedValue(
      status({ status: 'EXPLAINING', progress: 45 }),
    )
    analysesApi.fetchAnalysis
      .mockRejectedValueOnce(apiError(500, { code: 'COMMON_500', message }))
      .mockResolvedValue(detail({ status: 'EXPLAINING', overview: null }))

    const { wrapper } = await mountDetail()
    expect(wrapper.find('[data-error="detail"]').text()).toBe(message)
    // 다음 폴링이 상세를 다시 부르지 않는다 (같은 단계)
    await advance(2000)
    expect(analysesApi.fetchAnalysis).toHaveBeenCalledTimes(1)

    await wrapper.find('[data-action="retry-detail"]').trigger('click')
    await flushPromises()

    expect(analysesApi.fetchAnalysis).toHaveBeenCalledTimes(2)
    expect(wrapper.find('[aria-label="분석 관리"]').exists()).toBe(true)
  })

  it('AC13 FAILED에서 상세를 못 불러오면 서버 message와 "다시 시도" 버튼을 보여 준다', async () => {
    const message = '서버에서 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.'
    analysesApi.fetchAnalysisStatus.mockResolvedValue(
      status({ status: 'FAILED', progress: 40, errorCode: 'LLM_503', errorMessage: '실패' }),
    )
    analysesApi.fetchAnalysis
      .mockRejectedValueOnce(apiError(500, { code: 'COMMON_500', message }))
      .mockResolvedValue(detail({ status: 'FAILED', overview: null }))

    const { wrapper } = await mountDetail()
    expect(wrapper.find('[data-message="failed"]').text()).toContain('실패')
    expect(wrapper.find('[data-error="detail"]').text()).toBe(message)

    await wrapper.find('[data-action="retry-detail"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-action="delete"]').exists()).toBe(true)
  })

  it('AC13 삭제에 성공하면 내 분석 목록으로 이동한다', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(status({ status: 'DONE', progress: 100 }))
    analysesApi.fetchAnalysis.mockResolvedValue(detail())
    analysesApi.deleteAnalysis.mockResolvedValue(undefined)
    vi.spyOn(window, 'confirm').mockReturnValue(true)

    const { wrapper, router } = await mountDetail()
    // 목록 화면은 지연 로딩 라우트라 이동 완료 대신 이동 요청을 확인한다
    const push = vi.spyOn(router, 'push')
    await wrapper.find('[data-action="delete"]').trigger('click')
    await flushPromises()

    expect(analysesApi.deleteAnalysis).toHaveBeenCalledWith(42)
    expect(push).toHaveBeenCalledWith({ name: 'analyses' })
  })

  it('AC13 제목을 고치면 h1이 바뀐 제목으로 바뀐다', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(status({ status: 'DONE', progress: 100 }))
    analysesApi.fetchAnalysis.mockResolvedValue(detail())
    analysesApi.updateAnalysis.mockResolvedValue(detail({ title: '할 일 앱 백엔드' }))

    const { wrapper } = await mountDetail()
    await wrapper.find('[data-action="edit"]').trigger('click')
    await wrapper.find('[data-field="title"]').setValue('할 일 앱 백엔드')
    await wrapper.find('[data-form="edit"]').trigger('submit')
    await flushPromises()

    expect(wrapper.find('h1').text()).toBe('할 일 앱 백엔드')
  })

  it('AC14 남의 공개 분석이면 닉네임 링크, 퀴즈 없는 탭, 관리·질문·메모 없음', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(status({ status: 'DONE', progress: 100 }))
    analysesApi.fetchAnalysis.mockResolvedValue(
      detail({ mine: false, memo: null, isPublic: true, owner: { id: 7, nickname: 'vibecoder' } }),
    )

    const { wrapper } = await mountDetail('/analyses/42?tab=quiz')

    const owner = wrapper.find('[data-section="owner"] a')
    expect(owner.text()).toBe('vibecoder님의 분석')
    expect(owner.attributes('href')).toBe('/users/7')
    expect(wrapper.findAll('[role="tab"]').map((t) => t.text())).toEqual([
      '개요',
      '구조',
      '파일',
      '실행 흐름',
      '개념',
    ])
    // ?tab=quiz로 들어와도 개요
    expect(wrapper.find('[role="tab"][aria-selected="true"]').text()).toBe('개요')
    expect(wrapper.find('[aria-label="분석 관리"]').exists()).toBe(false)
    expect(wrapper.find('[data-action="ask-community"]').exists()).toBe(false)
    expect(wrapper.find('[data-section="memo"]').exists()).toBe(false)
    expect(quizzesApi.fetchAnalysisQuiz).not.toHaveBeenCalled()
  })

  it('AC14 남의 분석의 실행 흐름 탭은 읽기 전용이다', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(status({ status: 'DONE', progress: 100 }))
    analysesApi.fetchAnalysis.mockResolvedValue(
      detail({ mine: false, owner: { id: 7, nickname: 'vibecoder' } }),
    )

    const { wrapper } = await mountDetail('/analyses/42?tab=flows')

    expect(wrapper.find('[aria-label="API 선택"]').exists()).toBe(false)
    expect(wrapper.find('[data-action="request-flows"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('아직 만든 실행 흐름이 없습니다.')
  })

  it('AC12 수집에 실패한 URL 분석은 FAILED 화면에도 상태의 sourceUrl로 "저장소 보기" 링크를 보여 준다', async () => {
    analysesApi.fetchAnalysisStatus.mockResolvedValue(
      status({
        status: 'FAILED',
        progress: 5,
        errorCode: 'ANALYSIS_004',
        errorMessage: '저장소를 찾을 수 없습니다.',
        sourceUrl: 'https://github.com/octo/todo-app',
      }),
    )
    analysesApi.fetchAnalysis.mockRejectedValue(apiError(500, { code: 'COMMON_500' }))

    const { wrapper } = await mountDetail()

    const link = wrapper.find('[data-action="source"]')
    expect(link.attributes('href')).toBe('https://github.com/octo/todo-app')
    expect(link.attributes('target')).toBe('_blank')
    expect(wrapper.find('[data-message="failed"]').exists()).toBe(true)
  })
})
