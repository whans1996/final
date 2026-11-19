// 분석 (A F-05~07·F-19)
export default [
  {
    path: '/analyses',
    name: 'analyses',
    component: () => import('@/views/AnalysisListView.vue'),
    meta: { requiresAuth: true, nav: { label: '내 분석', order: 20, show: 'user' } },
  },
  {
    path: '/analyses/new',
    name: 'analysis-new',
    component: () => import('@/views/AnalysisRequestView.vue'),
    meta: { requiresAuth: true, nav: { label: '분석 요청', order: 10, show: 'user' } },
  },
  {
    path: '/analyses/:id(\\d+)',
    name: 'analysis-detail',
    component: () => import('@/views/AnalysisDetailView.vue'),
    meta: { requiresAuth: true },
  },
]
