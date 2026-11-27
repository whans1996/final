// 640 미만 회원 하단 탭 4곳 (F-29b AC5). 순서·경로는 spec 고정(대시보드·분석=내 분석·복습·카드)이라
// meta.nav가 아니라 여기 둔다. 더보기 드로어는 이 경로를 뺀 나머지 메뉴를 보여 준다.
export const BOTTOM_TABS = [
  { label: '대시보드', to: '/dashboard', icon: 'LayoutDashboard' },
  { label: '분석', to: '/analyses', icon: 'FileSearch' },
  { label: '복습', to: '/review-plans', icon: 'RotateCcw' },
  { label: '카드', to: '/concepts', icon: 'BookOpen' },
]

/** 메뉴 그룹에서 하단 탭에 이미 있는 경로를 빼고, 빈 그룹도 뺀다 */
export function withoutTabs(groups) {
  const tabPaths = new Set(BOTTOM_TABS.map((t) => t.to))
  return groups
    .map((g) => ({ ...g, items: g.items.filter((i) => !tabPaths.has(i.to)) }))
    .filter((g) => g.items.length > 0)
}
