// 복습 계획 (A F-15)
export default [
  {
    // SC-11 복습 계획
    path: '/review-plans',
    name: 'review-plans',
    component: () => import('@/views/reviewplan/ReviewPlanView.vue'),
    meta: { requiresAuth: true, nav: { label: '복습', order: 30, show: 'user' } },
  },
]
