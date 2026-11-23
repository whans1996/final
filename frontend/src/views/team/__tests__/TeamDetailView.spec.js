import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import TeamDetailView from '@/views/team/TeamDetailView.vue'
import * as teamsApi from '@/api/teams'
import { useAuthStore } from '@/stores/auth'
import { apiError, mountAt } from '@/views/__tests__/viewTestUtils'

vi.mock('@/api/teams', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    fetchTeam: vi.fn(),
    updateTeam: vi.fn(),
    deleteTeam: vi.fn(),
    regenerateInviteCode: vi.fn(),
    leaveTeam: vi.fn(),
    removeTeamMember: vi.fn(),
  }
})

const OWNER = {
  userId: 3,
  nickname: 'vibecoder',
  level: 'JUNIOR',
  role: 'OWNER',
  joinedAt: '2026-11-24T10:00:00+09:00',
}
const MEMBER = {
  userId: 7,
  nickname: 'learner',
  level: 'BEGINNER',
  role: 'MEMBER',
  joinedAt: '2026-11-25T11:30:00+09:00',
}

function team(partial) {
  return {
    id: 12,
    name: '스프링 스터디',
    description: 'F-05 분석 결과를 같이 공부합니다',
    myRole: 'OWNER',
    inviteCode: 'K7M2QX9P',
    memberCount: 2,
    maxMembers: 10,
    createdAt: '2026-11-24T10:00:00+09:00',
    members: [OWNER, MEMBER],
    ...partial,
  }
}

const asMember = { myRole: 'MEMBER', inviteCode: null }

const login = () =>
  useAuthStore().setSession({
    accessToken: 'access-1',
    user: { id: 3, nickname: 'vibecoder', level: 'JUNIOR', role: 'USER' },
  })

async function mountDetail(path = '/teams/12') {
  const mounted = await mountAt(TeamDetailView, path, { beforeNavigate: login })
  await flushPromises()
  return mounted
}

const button = (wrapper, action) => wrapper.find(`[data-action="${action}"]`)

describe('TeamDetailView SC-23 (F-20a AC14)', () => {
  let writeText

  beforeEach(() => {
    vi.resetAllMocks()
    writeText = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('AC14 팀 이름·소개, 멤버 n/10, 멤버 목록(링크, 수준, 팀장 칩, 가입일)이 보인다', async () => {
    teamsApi.fetchTeam.mockResolvedValue(team())

    const { wrapper } = await mountDetail()

    expect(teamsApi.fetchTeam).toHaveBeenCalledWith(12)
    expect(wrapper.find('h1').text()).toBe('스프링 스터디')
    expect(wrapper.find('[data-role="description"]').text()).toBe(
      'F-05 분석 결과를 같이 공부합니다',
    )
    expect(wrapper.find('[data-role="member-count"]').text()).toBe('멤버 2/10')
    const rows = wrapper.findAll('[data-team-member]')
    expect(rows).toHaveLength(2)
    expect(rows[0].find('a').attributes('href')).toBe('/users/3')
    expect(rows[0].find('a').text()).toContain('vibecoder')
    expect(rows[0].find('[data-role="owner"]').text()).toBe('팀장')
    expect(rows[0].find('[data-role="joined"]').text()).toBe('2026-11-24')
    expect(rows[1].find('[data-role="owner"]').exists()).toBe(false)
    expect(rows[1].find('[data-role="level"]').text()).toBe('입문')
    expect(rows[1].find('[data-role="joined"]').text()).toBe('2026-11-25')
  })

  it('AC14 팀장에게만 초대 코드 상자, 수정, 팀 삭제, 팀원 행의 내보내기가 보인다', async () => {
    teamsApi.fetchTeam.mockResolvedValue(team())

    const { wrapper } = await mountDetail()

    expect(wrapper.find('[data-role="invite-code"]').text()).toBe('K7M2QX9P')
    for (const action of ['copy-code', 'copy-link', 'regenerate', 'edit', 'delete-team']) {
      expect(button(wrapper, action).exists()).toBe(true)
    }
    const rows = wrapper.findAll('[data-team-member]')
    expect(rows[0].find('[data-action="remove-member"]').exists()).toBe(false)
    expect(rows[1].find('[data-action="remove-member"]').exists()).toBe(true)
    expect(button(wrapper, 'leave').exists()).toBe(false)
  })

  it('AC14 팀원에게는 팀 탈퇴만 보인다', async () => {
    teamsApi.fetchTeam.mockResolvedValue(team(asMember))

    const { wrapper } = await mountDetail()

    expect(wrapper.find('[data-role="invite-code"]').exists()).toBe(false)
    for (const action of ['regenerate', 'edit', 'delete-team', 'remove-member']) {
      expect(button(wrapper, action).exists()).toBe(false)
    }
    expect(button(wrapper, 'leave').exists()).toBe(true)
  })

  it('AC14 코드 복사·초대 링크 복사가 성공하면 "복사했습니다"', async () => {
    teamsApi.fetchTeam.mockResolvedValue(team())
    const { wrapper } = await mountDetail()

    await button(wrapper, 'copy-code').trigger('click')
    await flushPromises()
    expect(writeText).toHaveBeenLastCalledWith('K7M2QX9P')
    expect(wrapper.find('[data-role="copy-result"]').text()).toBe('복사했습니다')

    await button(wrapper, 'copy-link').trigger('click')
    await flushPromises()
    expect(writeText).toHaveBeenLastCalledWith(`${window.location.origin}/teams?code=K7M2QX9P`)
  })

  it('AC14 클립보드를 쓸 수 없으면 직접 복사 안내를 보여 준다', async () => {
    teamsApi.fetchTeam.mockResolvedValue(team())
    writeText.mockRejectedValue(new Error('denied'))
    const { wrapper } = await mountDetail()

    await button(wrapper, 'copy-code').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-role="copy-result"]').text()).toBe(
      '복사하지 못했습니다. 코드를 직접 복사해 주세요.',
    )

    Object.defineProperty(navigator, 'clipboard', { value: undefined, configurable: true })
    await button(wrapper, 'copy-link').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-role="copy-result"]').text()).toBe(
      '복사하지 못했습니다. 코드를 직접 복사해 주세요.',
    )
  })

  it('AC14 404면 안내와 내 팀으로 링크, 그 밖의 실패는 메시지와 다시 시도', async () => {
    teamsApi.fetchTeam.mockRejectedValueOnce(
      apiError(404, { code: 'TEAM_404', message: '팀을 찾을 수 없습니다.' }),
    )
    const { wrapper } = await mountDetail()
    expect(wrapper.text()).toContain('팀을 찾을 수 없습니다.')
    const back = wrapper.find('[data-action="back-to-teams"]')
    expect(back.attributes('href')).toBe('/teams')
    expect(back.text()).toBe('← 내 팀으로')

    teamsApi.fetchTeam.mockRejectedValueOnce(
      apiError(500, { code: 'COMMON_500', message: '서버 오류가 발생했습니다.' }),
    )
    teamsApi.fetchTeam.mockResolvedValueOnce(team())
    const second = await mountDetail()
    expect(second.wrapper.find('[role="alert"]').text()).toBe('서버 오류가 발생했습니다.')
    await button(second.wrapper, 'retry').trigger('click')
    await flushPromises()
    expect(second.wrapper.find('h1').text()).toBe('스프링 스터디')
  })
})

describe('TeamDetailView 관리 동작 (F-20a AC15)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('AC15 수정은 같은 화면의 폼으로 바뀌고 updateTeam 응답으로 화면을 갱신한다', async () => {
    teamsApi.fetchTeam.mockResolvedValue(team())
    teamsApi.updateTeam.mockResolvedValue(team({ name: '바뀐 이름', description: null }))
    const { wrapper } = await mountDetail()

    await button(wrapper, 'edit').trigger('click')
    const form = wrapper.find('[data-form="edit"]')
    expect(form.find('input[name="name"]').element.value).toBe('스프링 스터디')
    expect(form.find('input[name="name"]').attributes('maxlength')).toBe('30')
    await form.find('input[name="name"]').setValue('바뀐 이름')
    await form.find('textarea[name="description"]').setValue('')
    await form.trigger('submit')
    await flushPromises()

    expect(teamsApi.updateTeam).toHaveBeenCalledWith(12, { name: '바뀐 이름', description: '' })
    expect(wrapper.find('[data-form="edit"]').exists()).toBe(false)
    expect(wrapper.find('h1').text()).toBe('바뀐 이름')
    expect(wrapper.find('[data-role="description"]').exists()).toBe(false)
  })

  it('AC15 확인을 취소하면 재발급·내보내기·삭제·탈퇴 요청을 보내지 않는다', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    teamsApi.fetchTeam.mockResolvedValue(team())
    const { wrapper } = await mountDetail()

    await button(wrapper, 'regenerate').trigger('click')
    await button(wrapper, 'remove-member').trigger('click')
    await button(wrapper, 'delete-team').trigger('click')
    await flushPromises()

    expect(window.confirm).toHaveBeenNthCalledWith(1, '이전 코드로는 더 가입할 수 없습니다.')
    expect(window.confirm).toHaveBeenNthCalledWith(2, 'learner 님을 팀에서 내보낼까요?')
    expect(window.confirm).toHaveBeenNthCalledWith(3, '팀과 멤버 정보가 모두 삭제됩니다.')
    expect(teamsApi.regenerateInviteCode).not.toHaveBeenCalled()
    expect(teamsApi.removeTeamMember).not.toHaveBeenCalled()
    expect(teamsApi.deleteTeam).not.toHaveBeenCalled()

    teamsApi.fetchTeam.mockResolvedValue(team(asMember))
    const member = await mountDetail()
    await button(member.wrapper, 'leave').trigger('click')
    await flushPromises()
    expect(window.confirm).toHaveBeenLastCalledWith('팀에서 탈퇴할까요?')
    expect(teamsApi.leaveTeam).not.toHaveBeenCalled()
  })

  it('AC15 재발급은 새 코드를 보여 주고 내보내기는 멤버 목록을 다시 불러온다', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    teamsApi.fetchTeam.mockResolvedValueOnce(team())
    teamsApi.fetchTeam.mockResolvedValueOnce(team({ memberCount: 1, members: [OWNER] }))
    teamsApi.regenerateInviteCode.mockResolvedValue({ inviteCode: 'P4ZR8NWE' })
    teamsApi.removeTeamMember.mockResolvedValue(undefined)
    const { wrapper } = await mountDetail()

    await button(wrapper, 'regenerate').trigger('click')
    await flushPromises()
    expect(teamsApi.regenerateInviteCode).toHaveBeenCalledWith(12)
    expect(wrapper.find('[data-role="invite-code"]').text()).toBe('P4ZR8NWE')

    await button(wrapper, 'remove-member').trigger('click')
    await flushPromises()
    expect(teamsApi.removeTeamMember).toHaveBeenCalledWith(12, 7)
    expect(teamsApi.fetchTeam).toHaveBeenCalledTimes(2)
    expect(wrapper.findAll('[data-team-member]')).toHaveLength(1)
    expect(wrapper.find('[data-role="member-count"]').text()).toBe('멤버 1/10')
  })

  it('AC15 삭제·탈퇴는 SC-22로 간다', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    teamsApi.fetchTeam.mockResolvedValue(team())
    teamsApi.deleteTeam.mockResolvedValue(undefined)
    teamsApi.leaveTeam.mockResolvedValue(undefined)
    const owner = await mountDetail()

    await button(owner.wrapper, 'delete-team').trigger('click')
    await flushPromises()
    expect(teamsApi.deleteTeam).toHaveBeenCalledWith(12)
    await vi.waitFor(() => expect(owner.router.currentRoute.value.fullPath).toBe('/teams'), {
      timeout: 2000,
    })

    teamsApi.fetchTeam.mockResolvedValue(team(asMember))
    const member = await mountDetail()
    await button(member.wrapper, 'leave').trigger('click')
    await flushPromises()
    expect(teamsApi.leaveTeam).toHaveBeenCalledWith(12)
    await vi.waitFor(() => expect(member.router.currentRoute.value.fullPath).toBe('/teams'), {
      timeout: 2000,
    })
  })

  it('AC15 실패하면 서버 메시지를 보여 준다', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    teamsApi.fetchTeam.mockResolvedValue(team())
    teamsApi.deleteTeam.mockRejectedValue(
      apiError(403, { code: 'TEAM_403', message: '팀장만 할 수 있습니다.' }),
    )
    const { wrapper, router } = await mountDetail()

    await button(wrapper, 'delete-team').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-error="action"]').text()).toBe('팀장만 할 수 있습니다.')
    expect(router.currentRoute.value.fullPath).toBe('/teams/12')
  })
})
