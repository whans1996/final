// 대시보드 (A F-16)
export default [
  {
    // SC-04 대시보드: 오늘 복습·맞춤 추천·최근 분석. 로그인·가입 후 기본 이동 화면 (redirectTarget)
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('@/views/dashboard/DashboardView.vue'),
    meta: { requiresAuth: true, nav: { label: '대시보드', order: 1, show: 'user' } },
  },
]
