// 학습 챌린지 (A F-24·F-25). 관리자 화면(F-24)은 adminChildren에 넣으면 admin.js가 /admin 아래로 합친다
export default []

// ── A F-24 관리자 챌린지 관리 (/admin 자식) ──
export const adminChildren = [
  {
    path: 'challenges',
    name: 'admin-challenges',
    component: () => import('@/views/admin/challenge/AdminChallengeListView.vue'),
    meta: { adminNav: { label: '챌린지', order: 20 } },
  },
  {
    path: 'challenges/new',
    name: 'admin-challenge-new',
    component: () => import('@/views/admin/challenge/AdminChallengeEditView.vue'),
  },
  {
    path: 'challenges/:id(\\d+)/edit',
    name: 'admin-challenge-edit',
    component: () => import('@/views/admin/challenge/AdminChallengeEditView.vue'),
  },
]
