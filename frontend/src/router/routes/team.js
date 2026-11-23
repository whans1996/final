// 팀 (B F-20a): SC-22 내 팀 목록, SC-23 팀 상세
export default [
  {
    path: '/teams',
    name: 'teams',
    component: () => import('@/views/team/TeamListView.vue'),
    meta: { requiresAuth: true, nav: { label: '팀', order: 8, show: 'user' } },
  },
  {
    // 숫자가 아닌 id는 여기 걸리지 않고 not-found로 간다
    path: '/teams/:id(\\d+)',
    name: 'team-detail',
    component: () => import('@/views/team/TeamDetailView.vue'),
    meta: { requiresAuth: true },
  },
]
