// 헤더·관리자 메뉴를 route 메타로 만든다 (F-00b AC8·AC9). 메뉴를 배열 끝에 이어 붙이면 두 사람의 추가가 같은 줄에서
// 충돌하므로, 각 기능은 자기 route 파일에 meta만 단다.
//   헤더: meta.nav = { label, order, show: 'all' | 'guest' | 'user' | 'admin', group?, icon? }
//         (all은 로그인 여부와 상관없이 모두에게, user는 ADMIN에게도 보인다)
//         group은 NAV_GROUPS의 key, icon은 utils/navIcons.js의 Lucide 이름 (F-29b 옆 메뉴)
//   관리자 사이드 메뉴: /admin 자식 route의 meta.adminNav = { label, order }

const byOrder = (a, b) => a.order - b.order

function visible(show, auth) {
  if (show === 'all') return true
  if (show === 'guest') return !auth.isLoggedIn
  if (show === 'user') return auth.isLoggedIn
  if (show === 'admin') return auth.isAdmin
  return false
}

/** 최상위 route 중 meta.nav가 있고 지금 사용자에게 보이는 것을 order 순서로 `{ label, to }`로 만든다. */
export function navItems(routes, auth) {
  return routes
    .filter((r) => r.meta?.nav && visible(r.meta.nav.show, auth))
    .map((r) => ({ ...r.meta.nav, to: r.path }))
    .sort(byOrder)
    .map(({ label, to }) => ({ label, to }))
}

/** 옆 메뉴 그룹 순서·제목 (F-29b 5.1). 프로젝트(P-1)·면접 준비(I-1)는 route가 생길 때 group만 단다 */
export const NAV_GROUPS = [
  { key: 'dashboard', label: '대시보드' },
  { key: 'learn', label: '학습' },
  { key: 'together', label: '함께' },
  { key: 'prepare', label: '준비' },
]

/**
 * meta.nav.group이 있는 최상위 route를 NAV_GROUPS 순서로 묶어 `[{ key, label, items: [{ label, to, icon }] }]`로 만든다.
 * 그룹 안은 order 순이고, 보이는 항목이 없는 그룹·group이 없는 항목(로그인·관리자 등)은 뺀다.
 */
export function navGroups(routes, auth) {
  const entries = routes
    .filter((r) => r.meta?.nav?.group && visible(r.meta.nav.show, auth))
    .map((r) => ({ ...r.meta.nav, to: r.path }))
    .sort(byOrder)
  return NAV_GROUPS.map(({ key, label }) => ({
    key,
    label,
    items: entries
      .filter((e) => e.group === key)
      .map(({ label: itemLabel, to, icon }) => ({ label: itemLabel, to, icon })),
  })).filter((g) => g.items.length > 0)
}

/** /admin route의 자식 중 meta.adminNav가 있는 것을 order 순서로 `{ label, to: { name } }`로 만든다. */
export function adminNavItems(adminRoute) {
  return (adminRoute?.children ?? [])
    .filter((c) => c.meta?.adminNav)
    .map((c) => ({ ...c.meta.adminNav, to: { name: c.name } }))
    .sort(byOrder)
    .map(({ label, to }) => ({ label, to }))
}

/**
 * 지금 경로에 맞는 메뉴의 `to`를 고른다 (F-29b AC2). 같거나 하위 경로(`/analyses/12` → `/analyses`)면 맞고,
 * 여럿이 맞으면 가장 긴 것(`/analyses/new`는 `분석 요청`). 맞는 메뉴가 없으면 null.
 */
export function activeNavPath(path, items) {
  let best = null
  for (const { to } of items) {
    if (typeof to !== 'string') continue
    const hit = path === to || path.startsWith(to.endsWith('/') ? to : `${to}/`)
    if (hit && (best === null || to.length > best.length)) best = to
  }
  return best
}
