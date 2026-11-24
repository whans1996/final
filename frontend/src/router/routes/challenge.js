// 학습 챌린지 (A F-24·F-25). 관리자 화면(F-24)은 adminChildren에 넣으면 admin.js가 /admin 아래로 합친다
// ── A F-25 회원 챌린지 목록·상세 (SC-12·13). 둘 다 공개, 참여는 화면 안에서 로그인으로 보낸다 ──
export default [
  {
    path: '/challenges',
    name: 'challenge-list',
    component: () => import('@/views/challenge/ChallengeListView.vue'),
    meta: { nav: { label: '챌린지', order: 9, show: 'all' } },
  },
  {
    path: '/challenges/:id(\\d+)',
    name: 'challenge-detail',
    component: () => import('@/views/challenge/ChallengeDetailView.vue'),
  },
]

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
