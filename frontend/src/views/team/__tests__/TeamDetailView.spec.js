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
    fetchTeamAnalyses: vi.fn(),
    shareTeamAnalysis: vi.fn(),
    unshareTeamAnalysis: vi.fn(),
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
    teamsApi.fetchTeamAnalyses.mockResolvedValue([])
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
    teamsApi.fetchTeamAnalyses.mockResolvedValue([])
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

describe('TeamDetailView 다른 팀으로 이동한 뒤 늦게 온 응답 (#84)', () => {
  const OTHER = team({
    id: 13,
    name: '뷰 스터디',
    inviteCode: 'B3NQ7TWX',
    memberCount: 1,
    members: [OWNER],
  })

  /** 직접 resolve·reject할 수 있는 Promise. */
  function deferred() {
    let resolve
    let reject
    const promise = new Promise((res, rej) => {
      resolve = res
      reject = rej
    })
    return { promise, resolve, reject }
  }

  /** 팀 12에서 동작을 걸어 둔 채 팀 13으로 이동한다. */
  async function startThenMove(start) {
    teamsApi.fetchTeam.mockImplementation(async (id) => (id === 13 ? OTHER : team()))
    const mounted = await mountDetail()
    await start(mounted.wrapper)
    await flushPromises()
    await mounted.router.push('/teams/13')
    await flushPromises()
    return mounted
  }

  function expectShowsOther(wrapper) {
    expect(wrapper.find('h1').text()).toBe('뷰 스터디')
    expect(wrapper.find('[data-role="invite-code"]').text()).toBe('B3NQ7TWX')
    expect(wrapper.find('[data-role="member-count"]').text()).toBe('멤버 1/10')
    expect(wrapper.find('[data-error="action"]').exists()).toBe(false)
  }

  beforeEach(() => {
    vi.resetAllMocks()
    teamsApi.fetchTeamAnalyses.mockResolvedValue([])
    vi.spyOn(window, 'confirm').mockReturnValue(true)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('#84 이동하면 이전 팀의 진행 중 상태(수정 폼, busy)를 풀고 새 팀을 보여 준다', async () => {
    const save = deferred()
    teamsApi.updateTeam.mockReturnValue(save.promise)

    const { wrapper } = await startThenMove(async (w) => {
      await button(w, 'edit').trigger('click')
      await w.find('[data-form="edit"]').trigger('submit')
    })

    expect(wrapper.find('[data-form="edit"]').exists()).toBe(false)
    expectShowsOther(wrapper)
    expect(button(wrapper, 'edit').attributes('disabled')).toBeUndefined()
    expect(button(wrapper, 'regenerate').attributes('disabled')).toBeUndefined()
  })

  it('#84 늦게 온 수정 응답·실패는 새 팀 화면을 바꾸지 않는다', async () => {
    const save = deferred()
    teamsApi.updateTeam.mockReturnValueOnce(save.promise)
    const { wrapper } = await startThenMove(async (w) => {
      await button(w, 'edit').trigger('click')
      await w.find('[data-form="edit"]').trigger('submit')
    })
    save.resolve(team({ name: '바뀐 이름' }))
    await flushPromises()
    expectShowsOther(wrapper)

    // 새 팀에서 수정을 연 상태에서 이전 팀의 실패가 늦게 와도 폼에 에러가 붙지 않는다
    const failing = deferred()
    teamsApi.updateTeam.mockReturnValueOnce(failing.promise)
    const second = await startThenMove(async (w) => {
      await button(w, 'edit').trigger('click')
      await w.find('[data-form="edit"]').trigger('submit')
    })
    await button(second.wrapper, 'edit').trigger('click')
    failing.reject(apiError(403, { code: 'TEAM_403', message: '팀장만 할 수 있습니다.' }))
    await flushPromises()
    const form = second.wrapper.find('[data-form="edit"]')
    expect(form.find('input[name="name"]').element.value).toBe('뷰 스터디')
    expect(second.wrapper.text()).not.toContain('팀장만 할 수 있습니다.')
  })

  it('#84 늦게 온 재발급 응답은 새 팀의 초대 코드를 바꾸지 않는다', async () => {
    const regenerate = deferred()
    teamsApi.regenerateInviteCode.mockReturnValue(regenerate.promise)
    const { wrapper } = await startThenMove((w) => button(w, 'regenerate').trigger('click'))

    regenerate.resolve({ inviteCode: 'P4ZR8NWE' })
    await flushPromises()

    expect(teamsApi.regenerateInviteCode).toHaveBeenCalledWith(12)
    expectShowsOther(wrapper)
  })

  it('#84 늦게 끝난 내보내기는 이전 팀을 다시 불러와 새 팀을 덮어쓰지 않는다', async () => {
    const remove = deferred()
    teamsApi.removeTeamMember.mockReturnValue(remove.promise)
    const { wrapper } = await startThenMove((w) => button(w, 'remove-member').trigger('click'))
    teamsApi.fetchTeam.mockClear()

    remove.resolve(undefined)
    await flushPromises()

    expect(teamsApi.fetchTeam).not.toHaveBeenCalled()
    expectShowsOther(wrapper)
  })

  it('#84 늦게 끝난 삭제·탈퇴는 새 팀 화면에서 내 팀으로 끌고 가지 않는다', async () => {
    const remove = deferred()
    teamsApi.deleteTeam.mockReturnValue(remove.promise)
    const owner = await startThenMove((w) => button(w, 'delete-team').trigger('click'))
    remove.resolve(undefined)
    await flushPromises()
    expect(owner.router.currentRoute.value.fullPath).toBe('/teams/13')
    expectShowsOther(owner.wrapper)

    const leave = deferred()
    teamsApi.leaveTeam.mockReturnValue(leave.promise)
    teamsApi.fetchTeam.mockImplementation(async (id) => (id === 13 ? OTHER : team(asMember)))
    const member = await mountDetail()
    await button(member.wrapper, 'leave').trigger('click')
    await member.router.push('/teams/13')
    await flushPromises()
    leave.resolve(undefined)
    await flushPromises()
    expect(member.router.currentRoute.value.fullPath).toBe('/teams/13')
  })

  it('#84 늦게 온 재발급 실패는 새 팀에 에러를 띄우지 않고 새 팀의 동작을 잠그지 않는다', async () => {
    const regenerate = deferred()
    teamsApi.regenerateInviteCode.mockReturnValueOnce(regenerate.promise)
    const { wrapper } = await startThenMove((w) => button(w, 'regenerate').trigger('click'))

    // 새 팀에서 재발급을 시작해 busy인 동안 이전 팀의 실패가 온다
    const current = deferred()
    teamsApi.regenerateInviteCode.mockReturnValueOnce(current.promise)
    await button(wrapper, 'regenerate').trigger('click')
    regenerate.reject(apiError(403, { code: 'TEAM_403', message: '팀장만 할 수 있습니다.' }))
    await flushPromises()

    expect(wrapper.find('[data-error="action"]').exists()).toBe(false)
    expect(button(wrapper, 'edit').attributes('disabled')).toBeDefined()
    current.resolve({ inviteCode: 'Q9WE2RTY' })
    await flushPromises()
    expect(teamsApi.regenerateInviteCode).toHaveBeenLastCalledWith(13)
    expect(wrapper.find('[data-role="invite-code"]').text()).toBe('Q9WE2RTY')
    expect(button(wrapper, 'edit').attributes('disabled')).toBeUndefined()
  })

  it('#84 팀 상세를 떠난 뒤 늦게 끝난 삭제는 내 팀으로 끌고 가지 않는다', async () => {
    const remove = deferred()
    teamsApi.deleteTeam.mockReturnValue(remove.promise)
    teamsApi.fetchTeam.mockResolvedValue(team())
    const { wrapper, router } = await mountDetail()
    await button(wrapper, 'delete-team').trigger('click')
    await router.push('/')
    wrapper.unmount()

    remove.resolve(undefined)
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/')
  })
})

function shared(partial) {
  return {
    analysisId: 42,
    title: 'spring-realworld',
    projectName: 'spring-realworld-proj',
    level: 'JUNIOR',
    sharedBy: { userId: 7, nickname: 'learner' },
    sharedAt: '2026-11-25T14:20:00+09:00',
    questionCount: 6,
    solvedMemberCount: 1,
    myLastAttempt: { attemptId: 5, score: 4, total: 6, createdAt: '2026-11-25T15:02:00+09:00' },
    canUnshare: true,
    ...partial,
  }
}

describe('TeamDetailView SC-23 공유 분석 (F-20b AC10)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    teamsApi.fetchTeamAnalyses.mockResolvedValue([])
    teamsApi.fetchTeam.mockResolvedValue(team())
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  const items = (wrapper) => wrapper.findAll('[data-team-analysis]')

  it('AC10 공유 분석 n/10과 항목마다 제목·공유자·풀이 수·내 점수·링크·해제 버튼을 보여 준다', async () => {
    teamsApi.fetchTeamAnalyses.mockResolvedValue([
      shared(),
      shared({ analysisId: 43, title: null, myLastAttempt: null, canUnshare: false }),
    ])
    const { wrapper } = await mountDetail()

    expect(teamsApi.fetchTeamAnalyses).toHaveBeenCalledWith(12)
    expect(wrapper.find('[data-role="shared-count"]').text()).toBe('공유 분석 2/10')
    const [first, second] = items(wrapper)
    expect(first.text()).toContain('spring-realworld')
    expect(first.text()).toContain('learner 님 공유 · 2026-11-25')
    expect(first.text()).toContain('1/2명 풀이')
    expect(first.text()).toContain('내 점수 4/6')
    expect(first.find('[data-action="solve"]').attributes('href')).toBe('/analyses/42/quiz?team=12')
    expect(first.find('[data-action="understanding"]').attributes('href')).toBe(
      '/teams/12/analyses/42',
    )
    expect(first.find('[data-action="unshare"]').exists()).toBe(true)
    expect(second.text()).toContain('spring-realworld-proj')
    expect(second.text()).toContain('아직 풀지 않음')
    expect(second.find('[data-action="unshare"]').exists()).toBe(false)
  })

  it('AC10 공유가 없으면 안내를 보여 준다', async () => {
    teamsApi.fetchTeamAnalyses.mockResolvedValue([])
    const { wrapper } = await mountDetail()

    expect(wrapper.text()).toContain(
      '아직 공유한 분석이 없습니다. 내 분석을 공유해 팀원과 함께 퀴즈를 풀어 보세요.',
    )
  })

  it('AC10 목록 조회가 실패하면 그 영역에 message와 다시 시도를 보여 주고 팀 정보는 그대로다', async () => {
    teamsApi.fetchTeamAnalyses
      .mockRejectedValueOnce(apiError(500, { code: 'COMMON_500', message: '서버 오류입니다.' }))
      .mockResolvedValueOnce([shared()])
    const { wrapper } = await mountDetail()

    expect(wrapper.find('h1').text()).toBe('스프링 스터디')
    expect(wrapper.findAll('[data-team-member]')).toHaveLength(2)
    expect(wrapper.find('[data-error="team-analyses"]').text()).toBe('서버 오류입니다.')

    await button(wrapper, 'retry-team-analyses').trigger('click')
    await flushPromises()
    expect(items(wrapper)).toHaveLength(1)
  })

  it('AC10 공유 해제는 확인해야 요청하고, 성공하면 목록을 다시 불러온다', async () => {
    teamsApi.fetchTeamAnalyses.mockResolvedValueOnce([shared()]).mockResolvedValueOnce([])
    teamsApi.unshareTeamAnalysis.mockResolvedValue(undefined)
    const confirm = vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValueOnce(true)
    const { wrapper } = await mountDetail()

    await button(wrapper, 'unshare').trigger('click')
    expect(confirm).toHaveBeenCalledWith(
      '이 분석의 공유를 해제할까요? 팀원의 풀이 기록은 남습니다.',
    )
    expect(teamsApi.unshareTeamAnalysis).not.toHaveBeenCalled()

    await button(wrapper, 'unshare').trigger('click')
    await flushPromises()
    expect(teamsApi.unshareTeamAnalysis).toHaveBeenCalledWith(12, 42)
    expect(teamsApi.fetchTeamAnalyses).toHaveBeenCalledTimes(2)
    expect(items(wrapper)).toHaveLength(0)
  })
})
