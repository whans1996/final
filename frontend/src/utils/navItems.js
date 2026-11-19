// 헤더·관리자 메뉴를 route 메타로 만든다 (F-00b AC8·AC9). 메뉴를 배열 끝에 이어 붙이면 두 사람의 추가가 같은 줄에서
// 충돌하므로, 각 기능은 자기 route 파일에 meta만 단다.
//   헤더: meta.nav = { label, order, show: 'guest' | 'user' | 'admin' }  (user는 ADMIN에게도 보인다)
//   관리자 사이드 메뉴: /admin 자식 route의 meta.adminNav = { label, order }

const byOrder = (a, b) => a.order - b.order

function visible(show, auth) {
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

/** /admin route의 자식 중 meta.adminNav가 있는 것을 order 순서로 `{ label, to: { name } }`로 만든다. */
export function adminNavItems(adminRoute) {
  return (adminRoute?.children ?? [])
    .filter((c) => c.meta?.adminNav)
    .map((c) => ({ ...c.meta.adminNav, to: { name: c.name } }))
    .sort(byOrder)
    .map(({ label, to }) => ({ label, to }))
}
