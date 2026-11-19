import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import AnalysisConceptsTab from '@/components/analysis/AnalysisConceptsTab.vue'
import * as analysesApi from '@/api/analyses'
import { apiError } from '@/views/__tests__/viewTestUtils'

vi.mock('@/api/analyses')

function item(partial) {
  return {
    concept: {
      id: 12,
      name: '의존성 주입 (DI)',
      category: 'spring',
      difficulty: 'BEGINNER',
      summary: '스프링 컨테이너가 의존 객체를 넣어 줍니다.',
      avgRating: 4.5,
      wishCount: 3,
    },
    fileId: 102,
    filePath: 'src/main/java/com/example/todo/TodoService.java',
    lineHint: 'L8-L15',
    contextNote: '생성자로 TodoMapper를 받습니다.',
    ...partial,
  }
}

async function mountTab() {
  const wrapper = mount(AnalysisConceptsTab, {
    props: { analysisId: 42 },
    global: { stubs: { RouterLink: RouterLinkStub } },
  })
  await flushPromises()
  return wrapper
}

describe('AnalysisConceptsTab (F-08)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('AC8 개념 목록을 1회 불러와 카드와 위치·맥락을 보여 준다', async () => {
    analysesApi.fetchAnalysisConcepts.mockResolvedValue({
      items: [
        item(),
        item({
          concept: { ...item().concept, id: 40, name: 'JWT 기반 인증' },
          fileId: null,
          filePath: null,
          lineHint: null,
          contextNote: '설정 파일에서 JWT 필터를 등록합니다.',
        }),
      ],
      pendingCount: 0,
    })

    const wrapper = await mountTab()

    expect(analysesApi.fetchAnalysisConcepts).toHaveBeenCalledTimes(1)
    expect(analysesApi.fetchAnalysisConcepts).toHaveBeenCalledWith(42)
    const items = wrapper.findAll('[data-concept-item]')
    expect(items).toHaveLength(2)
    const links = wrapper.findAllComponents(RouterLinkStub)
    expect(links.map((l) => l.props('to'))).toEqual(['/concepts/12', '/concepts/40'])
    expect(items[0].find('[data-action="open-file"]').text()).toContain(
      'src/main/java/com/example/todo/TodoService.java',
    )
    expect(items[0].find('[data-action="open-file"]').text()).toContain('· L8-L15')
    expect(items[0].find('.note').text()).toBe('생성자로 TodoMapper를 받습니다.')
    // fileId가 없으면 위치 줄을 그리지 않는다
    expect(items[1].find('[data-action="open-file"]').exists()).toBe(false)
    expect(items[1].find('.note').text()).toBe('설정 파일에서 JWT 필터를 등록합니다.')
    expect(wrapper.find('[data-message="pending"]').exists()).toBe(false)
  })

  it('AC8 위치를 누르면 select-file로 fileId를 알린다', async () => {
    analysesApi.fetchAnalysisConcepts.mockResolvedValue({ items: [item()], pendingCount: 0 })

    const wrapper = await mountTab()
    await wrapper.find('[data-action="open-file"]').trigger('click')

    expect(wrapper.emitted('select-file')).toEqual([[102]])
  })

  it('AC8 줄 위치가 없으면 경로만, 맥락이 없으면 맥락 줄을 건너뛴다', async () => {
    analysesApi.fetchAnalysisConcepts.mockResolvedValue({
      items: [item({ lineHint: null, contextNote: null })],
      pendingCount: 0,
    })

    const wrapper = await mountTab()

    expect(wrapper.find('[data-action="open-file"]').text()).not.toContain('·')
    expect(wrapper.find('.note').exists()).toBe(false)
  })

  it('AC8 pendingCount가 있으면 검수 대기 안내를, 목록이 비면 빈 상태를 보여 준다', async () => {
    analysesApi.fetchAnalysisConcepts.mockResolvedValue({ items: [], pendingCount: 2 })

    const wrapper = await mountTab()

    expect(wrapper.find('[data-message="pending"]').text()).toBe(
      '새 개념 2개는 관리자 검수 후 카드로 공개됩니다.',
    )
    expect(wrapper.text()).toContain('이 분석에서 연결된 개념 카드가 없습니다.')
  })

  it('AC9 로딩 중 문구를 보여 주고, 실패하면 서버 message와 다시 시도 버튼으로 재요청한다', async () => {
    let resolve
    analysesApi.fetchAnalysisConcepts.mockReturnValueOnce(new Promise((r) => (resolve = r)))
    const wrapper = mount(AnalysisConceptsTab, {
      props: { analysisId: 42 },
      global: { stubs: { RouterLink: RouterLinkStub } },
    })
    expect(wrapper.text()).toContain('개념을 불러오는 중…')
    resolve({ items: [], pendingCount: 0 })
    await flushPromises()
    expect(wrapper.text()).not.toContain('개념을 불러오는 중…')

    analysesApi.fetchAnalysisConcepts.mockRejectedValueOnce(
      apiError(500, { code: 'COMMON_500', message: '잠시 후 다시 시도해 주세요.' }),
    )
    const failed = await mountTab()
    expect(failed.find('[role="alert"]').text()).toBe('잠시 후 다시 시도해 주세요.')

    analysesApi.fetchAnalysisConcepts.mockResolvedValueOnce({ items: [item()], pendingCount: 0 })
    await failed.find('[data-action="retry-load"]').trigger('click')
    await flushPromises()
    expect(failed.find('[role="alert"]').exists()).toBe(false)
    expect(failed.findAll('[data-concept-item]')).toHaveLength(1)
  })
})
