import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import AnalysisStructureTab from '@/components/analysis/AnalysisStructureTab.vue'
import * as analysesApi from '@/api/analyses'
import { apiError } from '@/views/__tests__/viewTestUtils'

vi.mock('@/api/analyses')
const mermaid = vi.hoisted(() => ({ initialize: vi.fn(), render: vi.fn() }))
vi.mock('mermaid', () => ({ default: mermaid }))

function node(fileId, path, layer) {
  return { fileId, path, name: path.split('/').pop(), layer, role: null }
}

const NODES = [
  node(101, 'src/main/java/com/example/todo/TodoController.java', 'controller'),
  node(102, 'src/main/java/com/example/todo/TodoService.java', 'service'),
  node(105, 'frontend/src/api/todoApi.js', 'js'),
  node(106, 'frontend/src/views/TodoView.vue', 'vue'),
  node(107, 'src/main/java/com/example/todo/TodoDto.java', 'domain'),
]

function structure(partial) {
  return {
    nodes: NODES,
    edges: [
      { from: 101, to: 102, type: 'IMPORT', line: 18 },
      { from: 105, to: 101, type: 'HTTP', line: 7 },
      { from: 106, to: 105, type: 'IMPORT', line: 3 },
    ],
    unresolved: [
      {
        fileId: 102,
        line: 5,
        kind: 'IMPORT',
        reason: 'NOT_SELECTED',
        detail: 'com.example.todo.TodoValidator',
      },
      { fileId: 105, line: 15, kind: 'HTTP_CALL', reason: 'URL_UNKNOWN', detail: 'http.get(url)' },
    ],
    edgesTruncated: false,
    unresolvedTruncated: false,
    ...partial,
  }
}

function mountTab() {
  return mount(AnalysisStructureTab, {
    props: { analysisId: 42 },
    global: { stubs: { RouterLink: RouterLinkStub } },
  })
}

function fileLink(fileId) {
  return { query: { tab: 'files', file: String(fileId) } }
}

describe('AnalysisStructureTab (F-21a)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mermaid.render.mockResolvedValue({ svg: '<svg data-test="graph"></svg>' })
  })

  it('AC14 로딩 문구 뒤 범례와 구조 그림을 보여 주고, 간선 없는 파일은 접힌 목록에 둔다', async () => {
    analysesApi.fetchAnalysisStructure.mockResolvedValue(structure())

    const wrapper = mountTab()
    expect(wrapper.text()).toContain('구조를 불러오는 중입니다…')
    await flushPromises()

    expect(analysesApi.fetchAnalysisStructure).toHaveBeenCalledWith(42)
    expect(wrapper.find('[data-role="legend"]').text()).toBe(
      '실선 = 코드 참조 · 점선 = API 호출 · 코드에서 확정한 연결만 그립니다',
    )
    const code = mermaid.render.mock.calls[0][1]
    expect(code.startsWith('flowchart LR')).toBe(true)
    expect(code).toContain('f105 -.->|HTTP| f101')
    expect(code).not.toContain('f107')
    expect(wrapper.find('[data-role="diagram"] svg').exists()).toBe(true)

    const isolated = wrapper.find('details[data-section="isolated"]')
    expect(isolated.attributes('open')).toBeUndefined()
    expect(isolated.find('summary').text()).toBe('연결 없는 파일 1개')
    expect(isolated.findAll('li').map((li) => li.text())).toEqual([
      'src/main/java/com/example/todo/TodoDto.java',
    ])
    expect(wrapper.find('[data-message="edges-truncated"]').exists()).toBe(false)
  })

  it('AC14 간선이 잘렸으면 150개까지만 보여 준다고 알린다', async () => {
    analysesApi.fetchAnalysisStructure.mockResolvedValue(structure({ edgesTruncated: true }))

    const wrapper = mountTab()
    await flushPromises()

    expect(wrapper.find('[data-message="edges-truncated"]').text()).toBe(
      '연결이 많아 150개까지만 보여 줍니다.',
    )
  })

  it('AC15 연결 목록은 줄마다 "파일:줄 → 파일 (종류)"이고 양쪽 파일 이름은 파일 탭 링크다', async () => {
    analysesApi.fetchAnalysisStructure.mockResolvedValue(structure())

    const wrapper = mountTab()
    await flushPromises()

    const edges = wrapper.find('details[data-section="edges"]')
    expect(edges.attributes('open')).toBeUndefined()
    expect(edges.find('summary').text()).toBe('연결 목록')
    const rows = edges.findAll('li')
    expect(rows.map((li) => li.text().replace(/\s+/g, ' '))).toEqual([
      'TodoController.java:18 → TodoService.java (코드 참조)',
      'todoApi.js:7 → TodoController.java (API 호출)',
      'TodoView.vue:3 → todoApi.js (코드 참조)',
    ])
    const links = rows[2].findAllComponents(RouterLinkStub)
    expect(links.map((l) => l.props('to'))).toEqual([fileLink(106), fileLink(105)])
  })

  it('AC15 확인하지 못한 연결은 "파일:줄 · 사유 · detail"로 접어 보여 주고, 없으면 그리지 않는다', async () => {
    analysesApi.fetchAnalysisStructure.mockResolvedValue(structure())

    const wrapper = mountTab()
    await flushPromises()

    const unresolved = wrapper.find('details[data-section="unresolved"]')
    expect(unresolved.attributes('open')).toBeUndefined()
    expect(unresolved.find('summary').text()).toBe('확인하지 못한 연결 2개')
    expect(unresolved.findAll('li').map((li) => li.text().replace(/\s+/g, ' '))).toEqual([
      'TodoService.java:5 · 분석에 포함되지 않은 프로젝트 파일을 참조합니다 · com.example.todo.TodoValidator',
      'todoApi.js:15 · 요청 주소가 실행 중에 정해져 코드만으로 확정할 수 없습니다 · http.get(url)',
    ])

    analysesApi.fetchAnalysisStructure.mockResolvedValue(structure({ unresolved: [] }))
    const empty = mountTab()
    await flushPromises()
    expect(empty.find('details[data-section="unresolved"]').exists()).toBe(false)
  })

  it('AC15 간선이 0개면 그림 대신 안내와 레이어별 파일 목록을 보여 주고 확인하지 못한 연결은 그대로 둔다', async () => {
    analysesApi.fetchAnalysisStructure.mockResolvedValue(structure({ edges: [] }))

    const wrapper = mountTab()
    await flushPromises()

    expect(wrapper.find('[data-message="no-edges"]').text()).toBe(
      '파일 사이 연결을 찾지 못했습니다.',
    )
    expect(mermaid.render).not.toHaveBeenCalled()
    expect(wrapper.find('[data-role="legend"]').exists()).toBe(false)
    expect(wrapper.find('details[data-section="edges"]').exists()).toBe(false)
    const groups = wrapper.findAll('[data-layer-group]')
    expect(groups.map((g) => g.find('h3').text())).toEqual([
      '컨트롤러',
      '서비스',
      '화면(Vue)',
      '스크립트',
      '도메인·DTO',
    ])
    expect(groups[0].findAll('li').map((li) => li.text())).toEqual([
      'src/main/java/com/example/todo/TodoController.java',
    ])
    expect(wrapper.find('details[data-section="unresolved"]').exists()).toBe(true)
  })

  it('AC15 API가 실패하면 서버 메시지와 다시 시도를 보여 주고, 다시 시도하면 불러온다', async () => {
    analysesApi.fetchAnalysisStructure.mockRejectedValueOnce(
      apiError(404, { code: 'ANALYSIS_404', message: '분석을 찾을 수 없습니다.' }),
    )
    analysesApi.fetchAnalysisStructure.mockResolvedValueOnce(structure())

    const wrapper = mountTab()
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toBe('분석을 찾을 수 없습니다.')
    await wrapper.find('[data-action="retry-load"]').trigger('click')
    await flushPromises()

    expect(analysesApi.fetchAnalysisStructure).toHaveBeenCalledTimes(2)
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    expect(wrapper.find('[data-role="legend"]').exists()).toBe(true)
  })
})
