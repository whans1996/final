// 개념 카드 (A F-08 · B F-09~13). 관리자 화면(F-13)은 adminChildren에 넣으면 admin.js가 /admin 아래로 합친다
export default [
  // ── B F-09~13 ──
  {
    path: '/concepts',
    name: 'concepts',
    component: () => import('@/views/concept/ConceptListView.vue'),
    meta: { nav: { label: '개념 카드', order: 5, show: 'all' } },
  },
]

export const adminChildren = []
