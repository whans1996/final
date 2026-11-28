import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import AnalysisFilesTab from '@/components/analysis/AnalysisFilesTab.vue'
import * as analysesApi from '@/api/analyses'
import { apiError, deferred } from '@/views/__tests__/viewTestUtils'

vi.mock('@/api/analyses')

const files = [
  {
    id: 101,
    path: 'src/TodoController.java',
    layer: 'controller',
    role: '할 일 API 컨트롤러',
    summary: '설명',
  },
  { id: 102, path: 'src/TodoService.java', layer: 'service', role: '업무 규칙', summary: '설명' },
  { id: 108, path: 'frontend/src/stores/todo.js', layer: 'js', role: null, summary: null },
  { id: 109, path: 'frontend/src/App.vue', layer: 'vue', role: '최상위 화면', summary: '설명' },
]

function file(partial) {
  return {
    id: 101,
    path: 'src/TodoController.java',
    layer: 'controller',
    role: '할 일 API 컨트롤러',
    summary: '@RestController가 붙어 있어 반환값이 JSON으로 응답됩니다.',
    content: 'package todo;\n\nclass TodoController {}\n',
    ...partial,
  }
}

async function mountTab(props = {}) {
  const wrapper = mount(AnalysisFilesTab, {
    props: { analysisId: 42, selectedFileId: null, ...props },
  })
  await flushPromises()
  return wrapper
}

describe('AnalysisFilesTab (F-06 AC14·AC15)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('AC14 파일 목록을 레이어별로 묶어(선별 우선순위 순) 경로·역할을 보여 준다', async () => {
    analysesApi.fetchAnalysisFiles.mockResolvedValue(files)

    const wrapper = await mountTab()

    expect(analysesApi.fetchAnalysisFiles).toHaveBeenCalledWith(42)
    const groups = wrapper.findAll('[data-layer]')
    expect(groups.map((g) => g.attributes('data-layer'))).toEqual([
      'controller',
      'service',
      'vue',
      'js',
    ])
    expect(groups[0].text()).toContain('컨트롤러')
    expect(groups[0].text()).toContain('src/TodoController.java')
    expect(groups[0].text()).toContain('할 일 API 컨트롤러')
    expect(groups[2].text()).toContain('화면(Vue)')
  })

  it('AC14 파일을 누르면 select 이벤트로 파일 id를 알린다', async () => {
    analysesApi.fetchAnalysisFiles.mockResolvedValue(files)
    const wrapper = await mountTab()

    await wrapper.find('[data-file-id="102"]').trigger('click')

    expect(wrapper.emitted('select')).toEqual([[102]])
  })

  it('AC14 선택된 파일은 단건을 불러와 설명과 줄 번호가 붙은 코드를 보여 준다', async () => {
    analysesApi.fetchAnalysisFiles.mockResolvedValue(files)
    analysesApi.fetchAnalysisFile.mockResolvedValue(file())

    const wrapper = await mountTab({ selectedFileId: 101 })

    expect(analysesApi.fetchAnalysisFile).toHaveBeenCalledWith(42, 101)
    expect(wrapper.find('[data-file-id="101"]').attributes('aria-current')).toBe('true')
    const detail = wrapper.find('[data-section="file-detail"]')
    expect(detail.text()).toContain('src/TodoController.java')
    expect(detail.text()).toContain('@RestController가 붙어 있어')
    const lines = detail.findAll('[data-line]')
    expect(lines.map((l) => l.attributes('data-line'))).toEqual(['1', '2', '3'])
    expect(lines[2].text()).toContain('class TodoController {}')
  })

  it('AC14 선택이 바뀌면 새 파일을 불러온다', async () => {
    analysesApi.fetchAnalysisFiles.mockResolvedValue(files)
    analysesApi.fetchAnalysisFile.mockImplementation(async (_, id) => file({ id, path: `p${id}` }))
    const wrapper = await mountTab({ selectedFileId: 101 })

    await wrapper.setProps({ selectedFileId: 102 })
    await flushPromises()

    expect(analysesApi.fetchAnalysisFile).toHaveBeenLastCalledWith(42, 102)
    expect(wrapper.find('[data-section="file-detail"]').text()).toContain('p102')
  })

  it('AC14 summary가 NULL이면 "이 파일은 설명을 만들지 못했습니다."를 보여 준다', async () => {
    analysesApi.fetchAnalysisFiles.mockResolvedValue(files)
    analysesApi.fetchAnalysisFile.mockResolvedValue(
      file({ id: 108, path: 'frontend/src/stores/todo.js', role: null, summary: null }),
    )

    const wrapper = await mountTab({ selectedFileId: 108 })

    expect(wrapper.find('[data-section="file-detail"]').text()).toContain(
      '이 파일은 설명을 만들지 못했습니다.',
    )
    expect(wrapper.find('[data-line="1"]').exists()).toBe(true)
  })

  it('AC15 목록을 불러오는 동안 로딩 문구를 보여 준다', async () => {
    analysesApi.fetchAnalysisFiles.mockReturnValue(new Promise(() => {}))

    const wrapper = mount(AnalysisFilesTab, { props: { analysisId: 42, selectedFileId: null } })

    expect(wrapper.text()).toContain('불러오는 중')
  })

  it('AC15 목록이 실패하면 서버 message와 "다시 시도" 버튼을 보여 주고, 누르면 다시 불러온다', async () => {
    const message = '서버에서 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.'
    analysesApi.fetchAnalysisFiles
      .mockRejectedValueOnce(apiError(500, { code: 'COMMON_500', message }))
      .mockResolvedValue(files)

    const wrapper = await mountTab()
    expect(wrapper.find('[role="alert"]').text()).toBe(message)

    await wrapper.find('[data-action="retry-files"]').trigger('click')
    await flushPromises()

    expect(analysesApi.fetchAnalysisFiles).toHaveBeenCalledTimes(2)
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    expect(wrapper.findAll('[data-file-id]')).toHaveLength(4)
  })

  it('AC15 파일 단건이 ANALYSIS_001이면 "파일을 찾을 수 없습니다."를 보여 주고 목록은 그대로 둔다', async () => {
    analysesApi.fetchAnalysisFiles.mockResolvedValue(files)
    analysesApi.fetchAnalysisFile.mockRejectedValue(
      apiError(404, { code: 'ANALYSIS_001', message: '파일을 찾을 수 없습니다.' }),
    )

    const wrapper = await mountTab({ selectedFileId: 999 })

    expect(wrapper.find('[data-section="file-detail"]').text()).toContain(
      '파일을 찾을 수 없습니다.',
    )
    expect(wrapper.find('[data-action="retry-file"]').exists()).toBe(false)
    expect(wrapper.findAll('[data-file-id]')).toHaveLength(4)
  })

  it('AC15 파일 단건이 그 밖의 이유로 실패하면 message와 "다시 시도" 버튼을 보여 준다', async () => {
    const message = '서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.'
    analysesApi.fetchAnalysisFiles.mockResolvedValue(files)
    analysesApi.fetchAnalysisFile
      .mockRejectedValueOnce(apiError(500, { code: 'COMMON_500', message }))
      .mockResolvedValue(file())

    const wrapper = await mountTab({ selectedFileId: 101 })
    expect(wrapper.find('[data-section="file-detail"] [role="alert"]').text()).toBe(message)

    await wrapper.find('[data-action="retry-file"]').trigger('click')
    await flushPromises()

    expect(analysesApi.fetchAnalysisFile).toHaveBeenCalledTimes(2)
    expect(wrapper.find('[data-section="file-detail"]').text()).toContain('@RestController')
  })

  it('AC15 저장된 파일이 없으면 빈 목록 안내를 보여 준다', async () => {
    analysesApi.fetchAnalysisFiles.mockResolvedValue([])

    const wrapper = await mountTab()

    expect(wrapper.text()).toContain('저장된 파일이 없습니다')
  })
  it('I-1a AC15 highlight 범위를 코드에서 강조하고, 파일 목록이 그려진 뒤에 그 줄로 스크롤한다', async () => {
    const scroll = vi.fn()
    Element.prototype.scrollIntoView = scroll
    const list = deferred()
    analysesApi.fetchAnalysisFiles.mockReturnValue(list.promise)
    analysesApi.fetchAnalysisFile.mockResolvedValue(file())

    const wrapper = await mountTab({ selectedFileId: 101, highlight: { start: 2, end: 3 } })

    // 모바일에서는 목록이 코드 위에 쌓이므로 목록보다 먼저 스크롤하면 코드가 아래로 밀려난다
    expect(
      wrapper.findAll('[data-highlighted="true"]').map((l) => l.attributes('data-line')),
    ).toEqual(['2', '3'])
    expect(scroll).not.toHaveBeenCalled()

    list.resolve(files)
    await flushPromises()

    expect(scroll).toHaveBeenCalledTimes(1)
    expect(scroll.mock.contexts[0].getAttribute('data-line')).toBe('2')
  })
})
