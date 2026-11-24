import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import TeamUnderstandingView from '@/views/team/TeamUnderstandingView.vue'
import * as teamsApi from '@/api/teams'
import { useAuthStore } from '@/stores/auth'
import { apiError, mountAt } from '@/views/__tests__/viewTestUtils'

vi.mock('@/api/teams', async (importOriginal) => ({
  ...(await importOriginal()),
  fetchTeamUnderstanding: vi.fn(),
  fetchTeamChallenges: vi.fn(),
  createTeamChallenge: vi.fn(),
}))

function understanding(partial) {
  return {
    analysisId: 42,
    title: 'spring-realworld',
    projectName: 'spring-realworld-proj',
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
    teamsApi.fetchTeamChallenges.mockResolvedValue([])
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

function challenge(partial) {
  return {
    challengeId: 7,
    analysisId: 42,
    title: 'spring-realworld',
    projectName: 'spring-realworld-proj',
    startDate: '2026-11-24',
    endDate: '2026-11-30',
    targetRate: 80,
    status: 'ONGOING',
    memberCount: 2,
    solvedMemberCount: 1,
    achievedMemberCount: 1,
    achieved: false,
    members: [
      {
        userId: 3,
        nickname: 'vibecoder',
        role: 'OWNER',
        attempt: { attemptId: 9, score: 4, total: 5, createdAt: '2026-11-24T15:02:00+09:00' },
        achieved: true,
      },
      { userId: 9, nickname: 'newbie', role: 'MEMBER', attempt: null, achieved: false },
    ],
    createdAt: '2026-11-24T10:00:00+09:00',
    ...partial,
  }
}

/** 로그인 회원 id. 3이면 팀장(understanding().members[0]), 9면 팀원. */
async function mountAs(userId, path = '/teams/12/analyses/42') {
  const login = () =>
    useAuthStore().setSession({
      accessToken: 'access-1',
      user: { id: userId, nickname: 'x', level: 'JUNIOR', role: 'USER' },
    })
  const mounted = await mountAt(TeamUnderstandingView, path, { beforeNavigate: login })
  await flushPromises()
  return mounted
}

describe('TeamUnderstandingView SC-24 팀 목표 (F-20c AC12·AC13)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 10, 24, 10, 0))
    teamsApi.fetchTeamUnderstanding.mockResolvedValue(understanding())
    teamsApi.fetchTeamChallenges.mockResolvedValue([])
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  const form = (wrapper) => wrapper.find('[data-form="challenge"]')

  it('AC12 이해도 위에 이 분석의 팀 목표를 보여 준다', async () => {
    teamsApi.fetchTeamChallenges.mockResolvedValue([challenge()])
    const { wrapper } = await mountAs(9)

    expect(teamsApi.fetchTeamChallenges).toHaveBeenCalledWith(12, { analysisId: 42 })
    const html = wrapper.html()
    expect(html.indexOf('data-section="team-challenge"')).toBeLessThan(html.indexOf('data-module'))
    expect(wrapper.find('[data-role="goal"]').text()).toBe('정답률 80% 이상 · 11-24 ~ 11-30')
    expect(wrapper.find('[data-role="progress"]').text()).toBe('1/2명 달성')
    expect(wrapper.findAll('[data-challenge-member]')).toHaveLength(2)
  })

  it('AC12 예정·진행 중이 없으면 가장 최근 종료 목표를 보여 준다', async () => {
    teamsApi.fetchTeamChallenges.mockResolvedValue([
      challenge({
        challengeId: 9,
        status: 'ENDED',
        startDate: '2026-11-17',
        endDate: '2026-11-23',
      }),
      challenge({
        challengeId: 8,
        status: 'ENDED',
        startDate: '2026-11-01',
        endDate: '2026-11-07',
      }),
    ])
    const { wrapper } = await mountAs(9)

    expect(wrapper.find('[data-challenge="9"]').exists()).toBe(true)
    expect(wrapper.find('[data-challenge="8"]').exists()).toBe(false)
  })

  it('AC12 목표가 없으면 안내를 보여 주고 팀원에게는 폼이 없다', async () => {
    const { wrapper } = await mountAs(9)

    expect(wrapper.find('[data-role="no-challenge"]').text()).toBe('이 분석에 팀 목표가 없습니다.')
    expect(form(wrapper).exists()).toBe(false)
  })

  it('AC12 팀장은 예정·진행 중 목표가 없을 때만 폼을 본다', async () => {
    teamsApi.fetchTeamChallenges
      .mockResolvedValueOnce([challenge({ status: 'ENDED', endDate: '2026-11-23' })])
      .mockResolvedValueOnce([challenge()])
    const ended = await mountAs(3)
    expect(form(ended.wrapper).exists()).toBe(true)

    const open = await mountAs(3)
    expect(form(open.wrapper).exists()).toBe(false)
  })

  it('AC12 제출하면 분석 id와 함께 만들고 목표 영역을 다시 불러온다', async () => {
    teamsApi.fetchTeamChallenges.mockResolvedValueOnce([]).mockResolvedValueOnce([challenge()])
    let resolve
    teamsApi.createTeamChallenge.mockReturnValue(new Promise((r) => (resolve = r)))
    const { wrapper } = await mountAs(3)

    await form(wrapper).trigger('submit')
    expect(wrapper.find('[data-action="create-challenge"]').attributes('disabled')).toBeDefined()
    resolve(challenge())
    await flushPromises()

    expect(teamsApi.createTeamChallenge).toHaveBeenCalledWith(12, {
      analysisId: 42,
      startDate: '2026-11-24',
      endDate: '2026-11-30',
      targetRate: 80,
    })
    expect(teamsApi.fetchTeamChallenges).toHaveBeenCalledTimes(2)
    expect(wrapper.find('[data-challenge="7"]').exists()).toBe(true)
    expect(form(wrapper).exists()).toBe(false)
  })

  it('AC12 만든 뒤 다시 불러온 목록에 열린 목표가 없으면 폼의 제출 버튼이 다시 활성이다', async () => {
    teamsApi.fetchTeamChallenges.mockResolvedValue([])
    teamsApi.createTeamChallenge.mockResolvedValue(challenge())
    const { wrapper } = await mountAs(3)

    await form(wrapper).trigger('submit')
    await flushPromises()

    expect(teamsApi.fetchTeamChallenges).toHaveBeenCalledTimes(2)
    expect(form(wrapper).exists()).toBe(true)
    expect(wrapper.find('[data-action="create-challenge"]').attributes('disabled')).toBeUndefined()
  })

  it('AC12 400은 필드 아래, 409·404는 폼 아래에 보여 주고 입력을 유지한다', async () => {
    teamsApi.createTeamChallenge
      .mockRejectedValueOnce(
        apiError(400, {
          code: 'COMMON_400',
          message: '입력값이 올바르지 않습니다.',
          details: { endDate: '기간은 최대 30일입니다.' },
        }),
      )
      .mockRejectedValueOnce(
        apiError(409, {
          code: 'TEAM_011',
          message: '이 분석에는 이미 예정되거나 진행 중인 팀 목표가 있습니다.',
        }),
      )
      .mockRejectedValueOnce(
        apiError(404, { code: 'TEAM_006', message: '팀에 공유된 분석을 찾을 수 없습니다.' }),
      )
    const { wrapper } = await mountAs(3)
    await wrapper.find('input[name="targetRate"]').setValue('95')

    await form(wrapper).trigger('submit')
    await flushPromises()
    expect(wrapper.find('[data-error="endDate"]').text()).toBe('기간은 최대 30일입니다.')

    await form(wrapper).trigger('submit')
    await flushPromises()
    expect(wrapper.find('[data-error="endDate"]').exists()).toBe(false)
    expect(wrapper.find('[data-error="challenge-form"]').text()).toBe(
      '이 분석에는 이미 예정되거나 진행 중인 팀 목표가 있습니다.',
    )

    await form(wrapper).trigger('submit')
    await flushPromises()
    expect(wrapper.find('[data-error="challenge-form"]').text()).toBe(
      '팀에 공유된 분석을 찾을 수 없습니다.',
    )
    expect(wrapper.find('input[name="targetRate"]').element.value).toBe('95')
    expect(wrapper.find('[data-action="create-challenge"]').attributes('disabled')).toBeUndefined()
  })

  it('AC12 목표 조회가 실패하면 그 영역에만 message와 다시 시도를 보여 주고 이해도는 그대로다', async () => {
    teamsApi.fetchTeamChallenges
      .mockRejectedValueOnce(apiError(500, { code: 'COMMON_500', message: '서버 오류입니다.' }))
      .mockResolvedValueOnce([challenge()])
    const { wrapper } = await mountAs(9)

    expect(wrapper.find('[data-error="team-challenge"]').text()).toBe('서버 오류입니다.')
    expect(wrapper.findAll('[data-module]')).toHaveLength(3)

    await wrapper.find('[data-action="retry-team-challenge"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-challenge="7"]').exists()).toBe(true)
  })

  it('AC13 분석 제목이 없으면 projectName을 제목으로 보여 준다 (#89)', async () => {
    teamsApi.fetchTeamUnderstanding.mockResolvedValue(understanding({ title: null }))
    const { wrapper } = await mountAs(9)

    expect(wrapper.find('h1').text()).toBe('spring-realworld-proj')
  })
})
