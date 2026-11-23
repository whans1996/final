import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import TeamUnderstandingView from '@/views/team/TeamUnderstandingView.vue'
import * as teamsApi from '@/api/teams'
import { apiError, mountAt } from '@/views/__tests__/viewTestUtils'

vi.mock('@/api/teams', async (importOriginal) => ({
  ...(await importOriginal()),
  fetchTeamUnderstanding: vi.fn(),
}))

function understanding(partial) {
  return {
    analysisId: 42,
    title: 'spring-realworld',
    sharedBy: { userId: 3, nickname: 'vibecoder' },
    questionCount: 4,
    memberCount: 3,
    solvedMemberCount: 2,
    modules: [
      { layer: 'controller', questionCount: 2, correctRate: 75 },
      { layer: 'service', questionCount: 1, correctRate: 50 },
      { layer: 'other', questionCount: 1, correctRate: 50 },
    ],
    members: [
      {
        userId: 3,
        nickname: 'vibecoder',
        role: 'OWNER',
        lastAttempt: { attemptId: 9, score: 2, total: 4, createdAt: '2026-11-25T15:02:00+09:00' },
        modules: [
          { layer: 'controller', correct: 1, total: 2 },
          { layer: 'service', correct: 1, total: 1 },
          { layer: 'other', correct: 0, total: 1 },
        ],
      },
      { userId: 9, nickname: 'newbie', role: 'MEMBER', lastAttempt: null, modules: [] },
    ],
    ...partial,
  }
}

async function mountView(path = '/teams/12/analyses/42') {
  const mounted = await mountAt(TeamUnderstandingView, path)
  await flushPromises()
  return mounted
}

describe('TeamUnderstandingView SC-24 (F-20b AC12)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('AC12 제목·공유자·풀이 수·문항 수와 퀴즈 풀기·팀으로 링크를 보여 준다', async () => {
    teamsApi.fetchTeamUnderstanding.mockResolvedValue(understanding())
    const { wrapper, router } = await mountView()

    expect(router.currentRoute.value.name).toBe('team-understanding')
    expect(teamsApi.fetchTeamUnderstanding).toHaveBeenCalledWith(12, 42)
    expect(wrapper.find('h1').text()).toBe('spring-realworld')
    expect(wrapper.text()).toContain('vibecoder 님 공유')
    expect(wrapper.text()).toContain('2/3명 풀이 · 객관식 4문항')
    expect(wrapper.find('[data-action="solve"]').attributes('href')).toBe(
      '/analyses/42/quiz?team=12',
    )
    expect(wrapper.find('[data-action="back-to-team"]').attributes('href')).toBe('/teams/12')
  })

  it('AC12 모듈마다 라벨·문항 수·정답률 막대를 보여 주고 가장 낮은 모듈(같으면 앞)에 칩을 단다', async () => {
    teamsApi.fetchTeamUnderstanding.mockResolvedValue(understanding())
    const { wrapper } = await mountView()

    const modules = wrapper.findAll('[data-module]')
    expect(modules).toHaveLength(3)
    expect(modules[0].text()).toContain('컨트롤러')
    expect(modules[0].text()).toContain('2문항')
    expect(modules[0].text()).toContain('75%')
    expect(modules[0].find('[data-role="bar"]').attributes('style')).toContain('width: 75%')
    expect(wrapper.findAll('[data-role="weakest"]')).toHaveLength(1)
    expect(modules[1].find('[data-role="weakest"]').text()).toBe('가장 약한 모듈')
  })

  it('AC12 멤버 × 모듈 표에 맞힌 수/문항 수, 안 푼 멤버는 아직 풀지 않음과 –를 보여 준다', async () => {
    teamsApi.fetchTeamUnderstanding.mockResolvedValue(understanding())
    const { wrapper } = await mountView()

    const rows = wrapper.findAll('[data-member-row]')
    expect(rows).toHaveLength(2)
    expect(rows[0].find('a').attributes('href')).toBe('/users/3')
    expect(rows[0].text()).toContain('2/4')
    expect(rows[0].findAll('[data-cell]').map((c) => c.text())).toEqual(['1/2', '1/1', '0/1'])
    expect(rows[1].text()).toContain('아직 풀지 않음')
    expect(rows[1].findAll('[data-cell]').map((c) => c.text())).toEqual(['–', '–', '–'])
  })

  it('AC12 아무도 풀지 않았으면 막대 대신 풀이 없음, 표 대신 안내를 보여 준다', async () => {
    teamsApi.fetchTeamUnderstanding.mockResolvedValue(
      understanding({
        solvedMemberCount: 0,
        modules: [{ layer: 'service', questionCount: 4, correctRate: null }],
        members: [
          { userId: 3, nickname: 'vibecoder', role: 'OWNER', lastAttempt: null, modules: [] },
        ],
      }),
    )
    const { wrapper } = await mountView()

    expect(wrapper.find('[data-module]').text()).toContain('풀이 없음')
    expect(wrapper.find('[data-role="bar"]').exists()).toBe(false)
    expect(wrapper.find('[data-role="weakest"]').exists()).toBe(false)
    expect(wrapper.find('[data-member-row]').exists()).toBe(false)
    expect(wrapper.text()).toContain('아직 이 분석의 퀴즈를 푼 팀원이 없습니다.')
  })

  it('AC12 TEAM_006이면 공유 분석 없음 안내와 팀으로 링크를 보여 준다', async () => {
    teamsApi.fetchTeamUnderstanding.mockRejectedValue(
      apiError(404, { code: 'TEAM_006', message: '팀에 공유된 분석을 찾을 수 없습니다.' }),
    )
    const { wrapper } = await mountView()

    expect(wrapper.text()).toContain('팀에 공유된 분석을 찾을 수 없습니다.')
    expect(wrapper.find('[data-action="back-to-team"]').attributes('href')).toBe('/teams/12')
  })

  it('AC12 TEAM_404이면 팀 없음 안내와 내 팀으로 링크를 보여 준다', async () => {
    teamsApi.fetchTeamUnderstanding.mockRejectedValue(
      apiError(404, { code: 'TEAM_404', message: '팀을 찾을 수 없습니다.' }),
    )
    const { wrapper } = await mountView()

    expect(wrapper.text()).toContain('팀을 찾을 수 없습니다.')
    expect(wrapper.find('[data-action="back-to-teams"]').attributes('href')).toBe('/teams')
    expect(wrapper.find('[data-action="solve"]').exists()).toBe(false)
  })

  it('AC12 그 밖의 실패는 message와 다시 시도를 보여 준다', async () => {
    teamsApi.fetchTeamUnderstanding
      .mockRejectedValueOnce(apiError(500, { code: 'COMMON_500', message: '서버 오류입니다.' }))
      .mockResolvedValueOnce(understanding())
    const { wrapper } = await mountView()

    expect(wrapper.find('[role="alert"]').text()).toBe('서버 오류입니다.')
    await wrapper.find('[data-action="retry"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('h1').text()).toBe('spring-realworld')
  })
})
