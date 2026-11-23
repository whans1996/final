import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import TeamAnalysisPicker from '@/components/team/TeamAnalysisPicker.vue'
import * as teamsApi from '@/api/teams'
import * as analysesApi from '@/api/analyses'
import { apiError } from '@/views/__tests__/viewTestUtils'

vi.mock('@/api/teams', async (importOriginal) => ({
  ...(await importOriginal()),
  shareTeamAnalysis: vi.fn(),
}))
vi.mock('@/api/analyses', () => ({ fetchAnalyses: vi.fn() }))

const analysis = (id, partial) => ({
  id,
  title: `분석 ${id}`,
  projectName: `proj-${id}`,
  status: 'DONE',
  ...partial,
})

function mountPicker(props = {}) {
  return mount(TeamAnalysisPicker, {
    props: { teamId: 12, sharedIds: [], full: false, ...props },
    global: { stubs: { RouterLink: RouterLinkStub } },
  })
}

async function open(wrapper) {
  await wrapper.find('[data-action="open-share"]').trigger('click')
  await flushPromises()
}

describe('TeamAnalysisPicker (F-20b AC11)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('AC11 내 분석 중 DONE이고 이 팀에 공유하지 않은 것만 고를 수 있다', async () => {
    analysesApi.fetchAnalyses.mockResolvedValue({
      items: [
        analysis(1),
        analysis(2, { status: 'EXPLAINING' }),
        analysis(3),
        analysis(4, { title: null }),
      ],
      total: 4,
    })
    const wrapper = mountPicker({ sharedIds: [3] })

    await open(wrapper)

    expect(analysesApi.fetchAnalyses).toHaveBeenCalledWith({ page: 1, size: 100 })
    const options = wrapper.findAll('[data-field="analysis"] option[value]:not([value=""])')
    expect(options.map((o) => o.text())).toEqual(['분석 1', 'proj-4'])
  })

  it('AC11 하나를 골라 공유하면 shareTeamAnalysis 후 shared를 알리고 목록을 닫는다', async () => {
    analysesApi.fetchAnalyses.mockResolvedValue({ items: [analysis(1), analysis(5)], total: 2 })
    teamsApi.shareTeamAnalysis.mockResolvedValue({
      analysisId: 5,
      sharedAt: '2026-11-25T14:20:00+09:00',
    })
    const wrapper = mountPicker()
    await open(wrapper)

    const share = () => wrapper.find('[data-action="share"]')
    expect(share().attributes('disabled')).toBeDefined()
    await wrapper.find('[data-field="analysis"]').setValue('5')
    await share().trigger('click')
    await flushPromises()

    expect(teamsApi.shareTeamAnalysis).toHaveBeenCalledWith(12, 5)
    expect(wrapper.emitted('shared')).toHaveLength(1)
    expect(wrapper.find('[data-field="analysis"]').exists()).toBe(false)
  })

  it('AC11 고를 것이 없으면 안내와 분석 요청하기 링크를 보여 준다', async () => {
    analysesApi.fetchAnalyses.mockResolvedValue({ items: [analysis(1)], total: 1 })
    const wrapper = mountPicker({ sharedIds: [1] })
    await open(wrapper)

    expect(wrapper.text()).toContain('공유할 수 있는 완료된 분석이 없습니다.')
    expect(wrapper.findComponent(RouterLinkStub).props('to')).toBe('/analyses/new')
  })

  it('AC11 공유가 10개면 분석 공유 버튼이 비활성이다', () => {
    const wrapper = mountPicker({ full: true })

    expect(wrapper.find('[data-action="open-share"]').attributes('disabled')).toBeDefined()
  })

  it('AC11 공유 요청 중에는 버튼이 비활성이고, 실패하면 고르기 목록 아래 서버 message를 보여 주며 선택을 유지한다', async () => {
    analysesApi.fetchAnalyses.mockResolvedValue({ items: [analysis(1)], total: 1 })
    let reject
    teamsApi.shareTeamAnalysis.mockReturnValue(new Promise((_, r) => (reject = r)))
    const wrapper = mountPicker()
    await open(wrapper)
    await wrapper.find('[data-field="analysis"]').setValue('1')

    await wrapper.find('[data-action="share"]').trigger('click')
    expect(wrapper.find('[data-action="share"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('[data-action="open-share"]').attributes('disabled')).toBeDefined()

    reject(
      apiError(409, { code: 'TEAM_008', message: '팀에 공유할 수 있는 분석은 최대 10개입니다.' }),
    )
    await flushPromises()

    expect(wrapper.find('[data-error="share"]').text()).toBe(
      '팀에 공유할 수 있는 분석은 최대 10개입니다.',
    )
    expect(wrapper.find('[data-field="analysis"]').element.value).toBe('1')
    expect(wrapper.emitted('shared')).toBeUndefined()
  })
})
