import { describe, expect, it } from 'vitest'
import { routes } from '@/router'
import { adminNavItems, NAV_GROUPS, navGroups, navItems } from '@/utils/navItems'

const loggedOut = { isLoggedIn: false, isAdmin: false }
const user = { isLoggedIn: true, isAdmin: false }
const admin = { isLoggedIn: true, isAdmin: true }

const labels = (items) => items.map((i) => i.label)

describe('navItems (F-00b AC8)', () => {
  it('AC8 비로그인은 개념 카드·커뮤니티·챌린지·로그인·회원가입을 본다 (F-09 AC9, F-17 AC13, F-25 AC12)', () => {
    expect(labels(navItems(routes, loggedOut))).toEqual([
      '개념 카드',
      '커뮤니티',
      '챌린지',
      '로그인',
      '회원가입',
    ])
  })

  it('AC8 로그인하면 대시보드(F-16 AC9)·개념 카드·커뮤니티(F-17 AC13)·피드(F-04 AC11)·팀(F-20a AC12)·챌린지(F-25 AC12)·분석 요청·내 분석·프로젝트(P-1a)·복습(F-15 AC12)을 본다', () => {
    expect(navItems(routes, user)).toEqual([
      { label: '대시보드', to: '/dashboard' },
      { label: '개념 카드', to: '/concepts' },
      { label: '커뮤니티', to: '/community' },
      { label: '피드', to: '/feed' },
      { label: '팀', to: '/teams' },
      { label: '챌린지', to: '/challenges' },
      { label: '분석 요청', to: '/analyses/new' },
      { label: '내 분석', to: '/analyses' },
      { label: '프로젝트', to: '/projects' },
      { label: '복습', to: '/review-plans' },
    ])
  })

  it('AC8 ADMIN은 여기에 관리자가 더해진다', () => {
    expect(navItems(routes, admin)).toEqual([
      { label: '대시보드', to: '/dashboard' },
      { label: '개념 카드', to: '/concepts' },
      { label: '커뮤니티', to: '/community' },
      { label: '피드', to: '/feed' },
      { label: '팀', to: '/teams' },
      { label: '챌린지', to: '/challenges' },
      { label: '분석 요청', to: '/analyses/new' },
      { label: '내 분석', to: '/analyses' },
      { label: '프로젝트', to: '/projects' },
      { label: '복습', to: '/review-plans' },
      { label: '관리자', to: '/admin' },
    ])
  })

  it('AC8 order 순서로 정렬하고 meta.nav가 없는 route는 뺀다', () => {
    const fake = [
      { path: '/b', meta: { nav: { label: 'B', order: 20, show: 'user' } } },
      { path: '/x' },
      { path: '/a', meta: { nav: { label: 'A', order: 10, show: 'user' } } },
      { path: '/g', meta: { nav: { label: 'G', order: 5, show: 'guest' } } },
    ]

    expect(labels(navItems(fake, user))).toEqual(['A', 'B'])
  })

  it("F-09 AC9 show: 'all'은 비로그인·회원·ADMIN 모두에게 보인다", () => {
    const fake = [{ path: '/c', meta: { nav: { label: 'C', order: 1, show: 'all' } } }]

    expect(labels(navItems(fake, loggedOut))).toEqual(['C'])
    expect(labels(navItems(fake, user))).toEqual(['C'])
    expect(labels(navItems(fake, admin))).toEqual(['C'])
  })
})

describe('adminNavItems (F-00b AC9)', () => {
  it('AC9 자식 route의 meta.adminNav를 order 순서로 메뉴로 만든다', () => {
    const adminRoute = {
      path: '/admin',
      children: [
        { path: '', name: 'admin-home' },
        {
          path: 'challenges',
          name: 'admin-challenges',
          meta: { adminNav: { label: '챌린지', order: 20 } },
        },
        {
          path: 'concepts',
          name: 'admin-concepts',
          meta: { adminNav: { label: '카드', order: 10 } },
        },
      ],
    }

    expect(adminNavItems(adminRoute)).toEqual([
      { label: '카드', to: { name: 'admin-concepts' } },
      { label: '챌린지', to: { name: 'admin-challenges' } },
    ])
  })

  it('AC9 메뉴가 붙은 자식이 없으면 빈 목록', () => {
    expect(adminNavItems({ path: '/admin', children: [{ path: '', name: 'admin-home' }] })).toEqual(
      [],
    )
    expect(adminNavItems(undefined)).toEqual([])
  })
})

describe('navGroups (F-29b AC1)', () => {
  const shape = (groups) => groups.map((g) => [g.key, labels(g.items)])

  it('AC1 그룹 순서는 NAV_GROUPS(dashboard·learn·together·prepare)이고 label을 붙인다', () => {
    expect(NAV_GROUPS.map((g) => g.key)).toEqual(['dashboard', 'learn', 'together', 'prepare'])
    expect(navGroups(routes, user).map(({ key, label }) => ({ key, label }))).toEqual([
      { key: 'dashboard', label: '대시보드' },
      { key: 'learn', label: '학습' },
      { key: 'together', label: '함께' },
    ])
  })

  it('AC1 회원: 그룹 안은 order 순, 아이콘 이름을 함께 돌려준다', () => {
    const groups = navGroups(routes, user)

    expect(shape(groups)).toEqual([
      ['dashboard', ['대시보드']],
      ['learn', ['개념 카드', '분석 요청', '내 분석', '프로젝트', '복습']],
      ['together', ['커뮤니티', '피드', '팀', '챌린지']],
    ])
    expect(groups[0].items[0]).toEqual({
      label: '대시보드',
      to: '/dashboard',
      icon: 'LayoutDashboard',
    })
    expect(groups[1].items.map((i) => i.icon)).toEqual([
      'BookOpen',
      'FilePlus2',
      'FileSearch',
      'FolderGit2',
      'RotateCcw',
    ])
    expect(groups[2].items.map((i) => i.icon)).toEqual(['MessagesSquare', 'Rss', 'Users', 'Trophy'])
  })

  it('AC1 group이 없는 항목(로그인·회원가입·관리자)은 그룹에 들어가지 않는다', () => {
    const all = (auth) => navGroups(routes, auth).flatMap((g) => labels(g.items))

    expect(all(admin)).not.toContain('관리자')
    expect(all(loggedOut)).not.toContain('로그인')
    expect(shape(navGroups(routes, loggedOut))).toEqual([
      ['learn', ['개념 카드']],
      ['together', ['커뮤니티', '챌린지']],
    ])
  })

  it('AC1 보이는 항목이 없는 그룹은 빠지고, 모르는 group 값도 버린다', () => {
    const fake = [
      { path: '/i', meta: { nav: { label: 'I', order: 1, show: 'admin', group: 'prepare' } } },
      { path: '/b', meta: { nav: { label: 'B', order: 20, show: 'user', group: 'learn' } } },
      { path: '/a', meta: { nav: { label: 'A', order: 10, show: 'user', group: 'learn' } } },
      { path: '/z', meta: { nav: { label: 'Z', order: 1, show: 'user', group: 'nope' } } },
    ]

    expect(shape(navGroups(fake, user))).toEqual([['learn', ['A', 'B']]])
    expect(shape(navGroups(fake, admin))).toEqual([
      ['learn', ['A', 'B']],
      ['prepare', ['I']],
    ])
  })
})
