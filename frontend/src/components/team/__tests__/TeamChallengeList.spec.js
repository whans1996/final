import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import TeamChallengeList from '@/components/team/TeamChallengeList.vue'
import * as teamsApi from '@/api/teams'
import { apiError } from '@/views/__tests__/viewTestUtils'

vi.mock('@/api/teams', async (importOriginal) => ({
  ...(await importOriginal()),
  fetchTeamChallenges: vi.fn(),
  deleteTeamChallenge: vi.fn(),
}))

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
    memberCount: 3,
    solvedMemberCount: 2,
    achievedMemberCount: 1,
    achieved: false,
    members: [],
    createdAt: '2026-11-24T10:00:00+09:00',
    ...partial,
  }
}

async function mountList(props = {}) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/:p(.*)*', component: { template: '<div />' } }],
  })
  await router.push('/')
  const wrapper = mount(TeamChallengeList, {
    props: { teamId: 12, isOwner: true, ...props },
    global: { plugins: [router] },
  })
  await flushPromises()
  return wrapper
}

const items = (wrapper) => wrapper.findAll('[data-team-challenge]')

describe('TeamChallengeList SC-23 팀 목표 (F-20c AC11)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 10, 24, 10, 0))
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('AC11 항목마다 제목·목표·기간·상태 칩·달성 수·이해도 링크를 보여 준다', async () => {
    teamsApi.fetchTeamChallenges.mockResolvedValue([
      challenge(),
      challenge({
        challengeId: 8,
        analysisId: 43,
        title: null,
        startDate: '2026-11-27',
        endDate: '2026-12-01',
        status: 'UPCOMING',
        achievedMemberCount: 0,
      }),
      challenge({
        challengeId: 9,
        startDate: '2026-11-10',
        endDate: '2026-11-16',
        status: 'ENDED',
        achievedMemberCount: 3,
        achieved: true,
      }),
      challenge({
        challengeId: 10,
        startDate: '2026-11-01',
        endDate: '2026-11-07',
        status: 'ENDED',
      }),
    ])
    const wrapper = await mountList()

    expect(teamsApi.fetchTeamChallenges).toHaveBeenCalledWith(12)
    const [ongoing, upcoming, achieved, missed] = items(wrapper)
    expect(ongoing.text()).toContain('spring-realworld · 정답률 80% 이상')
    expect(ongoing.text()).toContain('11-24 ~ 11-30')
    expect(ongoing.find('[data-role="status"]').text()).toBe('6일 남음')
    expect(ongoing.find('[data-role="progress"]').text()).toBe('1/3명 달성')
    expect(ongoing.find('[data-role="achieved"]').exists()).toBe(false)
    expect(ongoing.find('[data-role="missed"]').exists()).toBe(false)
    expect(ongoing.find('[data-action="challenge-understanding"]').attributes('href')).toBe(
      '/teams/12/analyses/42',
    )
    expect(upcoming.text()).toContain('spring-realworld-proj · 정답률 80% 이상')
    expect(upcoming.find('[data-role="status"]').text()).toBe('D-3')
    expect(achieved.find('[data-role="status"]').text()).toBe('종료')
    expect(achieved.find('[data-role="achieved"]').text()).toBe('달성')
    expect(missed.find('[data-role="missed"]').text()).toBe('미달성')
  })

  it('AC11 진행 중 목표의 종료일이 오늘이면 "오늘 마감"이다', async () => {
    teamsApi.fetchTeamChallenges.mockResolvedValue([challenge({ endDate: '2026-11-24' })])
    const wrapper = await mountList()

    expect(wrapper.find('[data-role="status"]').text()).toBe('오늘 마감')
  })

  it('AC11 팀장만 삭제하고, 확인을 취소하면 요청하지 않으며, 성공하면 다시 불러온다', async () => {
    teamsApi.fetchTeamChallenges.mockResolvedValueOnce([challenge()]).mockResolvedValueOnce([])
    teamsApi.deleteTeamChallenge.mockResolvedValue(undefined)
    const confirm = vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValueOnce(true)
    const wrapper = await mountList()

    await wrapper.find('[data-action="delete-challenge"]').trigger('click')
    expect(confirm).toHaveBeenCalledWith('이 팀 목표를 삭제할까요?')
    expect(teamsApi.deleteTeamChallenge).not.toHaveBeenCalled()

    await wrapper.find('[data-action="delete-challenge"]').trigger('click')
    await flushPromises()
    expect(teamsApi.deleteTeamChallenge).toHaveBeenCalledWith(12, 7)
    expect(teamsApi.fetchTeamChallenges).toHaveBeenCalledTimes(2)
    expect(items(wrapper)).toHaveLength(0)
  })

  it('AC11 팀원에게는 삭제 버튼이 없다', async () => {
    teamsApi.fetchTeamChallenges.mockResolvedValue([challenge()])
    const wrapper = await mountList({ isOwner: false })

    expect(wrapper.find('[data-action="delete-challenge"]').exists()).toBe(false)
  })

  it('AC11 삭제가 실패하면 서버 메시지를 보여 준다', async () => {
    teamsApi.fetchTeamChallenges.mockResolvedValue([challenge()])
    teamsApi.deleteTeamChallenge.mockRejectedValue(
      apiError(404, { code: 'TEAM_013', message: '팀 목표를 찾을 수 없습니다.' }),
    )
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const wrapper = await mountList()

    await wrapper.find('[data-action="delete-challenge"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-error="delete-challenge"]').text()).toBe(
      '팀 목표를 찾을 수 없습니다.',
    )
  })

  it('AC11 목표가 없으면 안내를 보여 주고 팀장에게는 만드는 곳을 덧붙인다', async () => {
    teamsApi.fetchTeamChallenges.mockResolvedValue([])
    const owner = await mountList()
    expect(owner.text()).toContain(
      '아직 팀 목표가 없습니다. 공유 분석의 이해도 화면에서 목표를 만들 수 있습니다.',
    )

    const member = await mountList({ isOwner: false })
    expect(member.text()).toContain('아직 팀 목표가 없습니다.')
    expect(member.text()).not.toContain('이해도 화면에서 목표를 만들 수 있습니다.')
  })

  it('AC11 조회가 실패하면 영역 안에 message와 다시 시도를 보여 준다', async () => {
    teamsApi.fetchTeamChallenges
      .mockRejectedValueOnce(apiError(500, { code: 'COMMON_500', message: '서버 오류입니다.' }))
      .mockResolvedValueOnce([challenge()])
    const wrapper = await mountList()

    expect(wrapper.find('[data-error="team-challenges"]').text()).toBe('서버 오류입니다.')
    await wrapper.find('[data-action="retry-team-challenges"]').trigger('click')
    await flushPromises()
    expect(items(wrapper)).toHaveLength(1)
  })
})
