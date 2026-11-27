// 팀 (B F-20a·F-20b): SC-22 내 팀 목록, SC-23 팀 상세, SC-24 팀 이해도
export default [
  {
    path: '/teams',
    name: 'teams',
    component: () => import('@/views/team/TeamListView.vue'),
    meta: {
      requiresAuth: true,
      nav: { label: '팀', order: 8, show: 'user', group: 'together', icon: 'Users' },
    },
  },
  {
    // 숫자가 아닌 id는 여기 걸리지 않고 not-found로 간다
    path: '/teams/:id(\\d+)',
    name: 'team-detail',
    component: () => import('@/views/team/TeamDetailView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/teams/:id(\\d+)/analyses/:analysisId(\\d+)',
    name: 'team-understanding',
    component: () => import('@/views/team/TeamUnderstandingView.vue'),
    meta: { requiresAuth: true },
  },
]
