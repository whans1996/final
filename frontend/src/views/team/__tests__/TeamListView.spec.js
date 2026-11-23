import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import TeamListView from '@/views/team/TeamListView.vue'
import * as teamsApi from '@/api/teams'
import { authGuard, routes } from '@/router'
import { useAuthStore } from '@/stores/auth'
import { navItems } from '@/utils/navItems'
import { apiError, mountAt } from '@/views/__tests__/viewTestUtils'

vi.mock('@/api/teams', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    fetchMyTeams: vi.fn(),
    createTeam: vi.fn(),
    joinTeam: vi.fn(),
  }
})

function myTeam(partial) {
  return {
    id: 12,
    name: '스프링 스터디',
    description: 'F-05 분석 결과를 같이 공부합니다',
    myRole: 'OWNER',
    memberCount: 3,
    ownerNickname: 'vibecoder',
    joinedAt: '2026-11-24T10:00:00+09:00',
    ...partial,
  }
}

const login = () =>
  useAuthStore().setSession({
    accessToken: 'access-1',
    user: { id: 3, nickname: 'vibecoder', level: 'JUNIOR', role: 'USER' },
  })

async function mountList(path = '/teams') {
  const mounted = await mountAt(TeamListView, path, { beforeNavigate: login })
  await flushPromises()
  return mounted
}

const createForm = (wrapper) => wrapper.find('[data-form="create"]')
const joinForm = (wrapper) => wrapper.find('[data-form="join"]')

describe('TeamListView SC-22 (F-20a AC12)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    teamsApi.fetchMyTeams.mockResolvedValue([])
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('AC12 헤더 메뉴의 "팀"은 로그인 회원에게만 피드 다음에 보인다', () => {
    const guest = navItems(routes, { isLoggedIn: false, isAdmin: false }).map((i) => i.label)
    const user = navItems(routes, { isLoggedIn: true, isAdmin: false }).map((i) => i.label)

    expect(guest).not.toContain('팀')
    expect(user.indexOf('팀')).toBe(user.indexOf('피드') + 1)
  })

  it('AC12 비로그인은 /login?redirect=/teams로 간다', () => {
    const teams = routes.find((r) => r.path === '/teams')
    const to = { meta: teams.meta, fullPath: '/teams' }

    expect(authGuard(to, { isLoggedIn: false, isAdmin: false })).toEqual({
      name: 'login',
      query: { redirect: '/teams' },
    })
  })

  it('AC12 내 팀 카드에 이름, 역할 칩, 인원, 소개가 보이고 누르면 SC-23으로 간다', async () => {
    teamsApi.fetchMyTeams.mockResolvedValue([
      myTeam({ id: 21, name: '팀원인 팀', myRole: 'MEMBER', memberCount: 5, description: null }),
      myTeam(),
    ])

    const { wrapper } = await mountList()

    const cards = wrapper.findAll('[data-team-card]')
    expect(cards).toHaveLength(2)
    expect(cards[0].attributes('href')).toBe('/teams/21')
    expect(cards[0].find('[data-role="name"]').text()).toBe('팀원인 팀')
    expect(cards[0].find('[data-role="role"]').text()).toBe('팀원')
    expect(cards[0].find('[data-role="count"]').text()).toBe('5명')
    expect(cards[0].find('[data-role="description"]').exists()).toBe(false)
    expect(cards[1].attributes('href')).toBe('/teams/12')
    expect(cards[1].find('[data-role="role"]').text()).toBe('팀장')
    expect(cards[1].find('[data-role="count"]').text()).toBe('3명')
    expect(cards[1].find('[data-role="description"]').text()).toBe(
      'F-05 분석 결과를 같이 공부합니다',
    )
  })

  it('AC12 속한 팀이 없으면 안내 문구를 보여 준다', async () => {
    const { wrapper } = await mountList()

    expect(wrapper.text()).toContain(
      '아직 속한 팀이 없습니다. 팀을 만들거나 초대 코드로 가입해 보세요.',
    )
  })

  it('AC12 불러오기에 실패하면 서버 메시지와 다시 시도를 보여 준다', async () => {
    teamsApi.fetchMyTeams.mockRejectedValueOnce(
      apiError(500, { code: 'COMMON_500', message: '서버 오류가 발생했습니다.' }),
    )
    teamsApi.fetchMyTeams.mockResolvedValueOnce([myTeam()])

    const { wrapper } = await mountList()
    expect(wrapper.find('[role="alert"]').text()).toBe('서버 오류가 발생했습니다.')

    await wrapper.find('[data-action="retry"]').trigger('click')
    await flushPromises()

    expect(teamsApi.fetchMyTeams).toHaveBeenCalledTimes(2)
    expect(wrapper.findAll('[data-team-card]')).toHaveLength(1)
  })
})

describe('TeamListView 만들기·가입 (F-20a AC13)', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    teamsApi.fetchMyTeams.mockResolvedValue([])
  })

  it('AC13 만들기를 누르면 createTeam 후 SC-23으로 간다', async () => {
    teamsApi.createTeam.mockResolvedValue({ id: 12, inviteCode: 'K7M2QX9P' })
    const { wrapper, router } = await mountList()

    await createForm(wrapper).find('input[name="name"]').setValue('스프링 스터디')
    await createForm(wrapper).find('textarea[name="description"]').setValue('같이 공부해요')
    await createForm(wrapper).trigger('submit')
    await flushPromises()

    expect(teamsApi.createTeam).toHaveBeenCalledWith({
      name: '스프링 스터디',
      description: '같이 공부해요',
    })
    await vi.waitFor(() => expect(router.currentRoute.value.fullPath).toBe('/teams/12'), {
      timeout: 2000,
    })
  })

  it('AC13 가입을 누르면 joinTeam 후 응답 teamId의 SC-23으로 간다', async () => {
    teamsApi.joinTeam.mockResolvedValue({ teamId: 34 })
    const { wrapper, router } = await mountList()

    await joinForm(wrapper).find('input[name="inviteCode"]').setValue('k7m2qx9p')
    await joinForm(wrapper).trigger('submit')
    await flushPromises()

    expect(teamsApi.joinTeam).toHaveBeenCalledWith('K7M2QX9P')
    await vi.waitFor(() => expect(router.currentRoute.value.fullPath).toBe('/teams/34'), {
      timeout: 2000,
    })
  })

  it('AC13 입력 제한: 이름 30자·소개 200자·코드 8자이고 코드는 대문자로 보인다', async () => {
    const { wrapper } = await mountList()

    expect(createForm(wrapper).find('input[name="name"]').attributes('maxlength')).toBe('30')
    expect(createForm(wrapper).find('textarea[name="description"]').attributes('maxlength')).toBe(
      '200',
    )
    const code = joinForm(wrapper).find('input[name="inviteCode"]')
    await code.setValue('ab12cd')
    expect(code.element.value).toBe('AB12CD')
    await code.setValue('ab12cd34ef')
    expect(code.element.value).toBe('AB12CD34')
  })

  it('AC13 공백이 섞인 코드를 붙여 넣어도 공백을 빼고 8자를 남긴다', async () => {
    teamsApi.joinTeam.mockResolvedValue({ teamId: 7 })
    const { wrapper } = await mountList()
    const code = joinForm(wrapper).find('input[name="inviteCode"]')

    await code.setValue(' k7m2 qx9p ')
    expect(code.element.value).toBe('K7M2QX9P')
    await joinForm(wrapper).trigger('submit')
    await flushPromises()
    expect(teamsApi.joinTeam).toHaveBeenCalledWith('K7M2QX9P')
  })

  it('AC13 ?code=에 공백이 섞여 있어도 공백을 빼고 채운다', async () => {
    const { wrapper } = await mountList('/teams?code=%20k7m2qx9p%20')
    expect(joinForm(wrapper).find('input[name="inviteCode"]').element.value).toBe('K7M2QX9P')
  })

  it('AC13 이름이 공백뿐이거나 코드가 비면 버튼이 비활성이고 요청 중에도 비활성이다', async () => {
    let resolveJoin
    teamsApi.joinTeam.mockReturnValue(new Promise((r) => (resolveJoin = r)))
    const { wrapper } = await mountList()
    const createButton = () => createForm(wrapper).find('button[type="submit"]')
    const joinButton = () => joinForm(wrapper).find('button[type="submit"]')

    expect(createButton().attributes('disabled')).toBeDefined()
    expect(joinButton().attributes('disabled')).toBeDefined()
    await createForm(wrapper).find('input[name="name"]').setValue('   ')
    expect(createButton().attributes('disabled')).toBeDefined()
    await createForm(wrapper).find('input[name="name"]').setValue('팀')
    expect(createButton().attributes('disabled')).toBeUndefined()

    await joinForm(wrapper).find('input[name="inviteCode"]').setValue('K7M2QX9P')
    expect(joinButton().attributes('disabled')).toBeUndefined()
    await joinForm(wrapper).trigger('submit')
    expect(joinButton().attributes('disabled')).toBeDefined()
    resolveJoin({ teamId: 1 })
    await flushPromises()
  })

  it('AC13 ?code=로 들어오면 코드 입력란을 미리 채우고 자동 가입하지 않는다', async () => {
    const { wrapper } = await mountList('/teams?code=k7m2qx9p')

    expect(joinForm(wrapper).find('input[name="inviteCode"]').element.value).toBe('K7M2QX9P')
    expect(teamsApi.joinTeam).not.toHaveBeenCalled()
  })

  it('AC13 400이면 필드 아래, TEAM_ 코드는 폼 아래에 메시지를 보여 주고 입력값을 유지한다', async () => {
    teamsApi.createTeam.mockRejectedValueOnce(
      apiError(400, {
        code: 'COMMON_400',
        message: '입력값이 올바르지 않습니다.',
        details: { name: '팀 이름은 30자 이하로 입력해 주세요.' },
      }),
    )
    teamsApi.createTeam.mockRejectedValueOnce(
      apiError(409, { code: 'TEAM_003', message: '더 이상 팀에 속할 수 없습니다. (최대 5개)' }),
    )
    teamsApi.joinTeam.mockRejectedValueOnce(
      apiError(404, { code: 'TEAM_001', message: '유효하지 않은 초대 코드입니다.' }),
    )
    const { wrapper } = await mountList()

    await createForm(wrapper).find('input[name="name"]').setValue('스프링')
    await createForm(wrapper).trigger('submit')
    await flushPromises()
    expect(createForm(wrapper).find('[data-error="name"]').text()).toBe(
      '팀 이름은 30자 이하로 입력해 주세요.',
    )

    await createForm(wrapper).trigger('submit')
    await flushPromises()
    expect(createForm(wrapper).find('[data-error="form"]').text()).toBe(
      '더 이상 팀에 속할 수 없습니다. (최대 5개)',
    )
    expect(createForm(wrapper).find('[data-error="name"]').exists()).toBe(false)
    expect(createForm(wrapper).find('input[name="name"]').element.value).toBe('스프링')

    await joinForm(wrapper).find('input[name="inviteCode"]').setValue('ZZZZ2222')
    await joinForm(wrapper).trigger('submit')
    await flushPromises()
    expect(joinForm(wrapper).find('[data-error="form"]').text()).toBe(
      '유효하지 않은 초대 코드입니다.',
    )
    expect(joinForm(wrapper).find('input[name="inviteCode"]').element.value).toBe('ZZZZ2222')
  })
})
