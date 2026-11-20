// 커뮤니티 게시판·댓글 (B F-17·F-18), 활동 피드 (B F-04)
export default [
  {
    path: '/community',
    name: 'community',
    component: () => import('@/views/community/PostListView.vue'),
    meta: { nav: { label: '커뮤니티', order: 6, show: 'all' } },
  },
  {
    // 숫자가 아닌 id는 여기 걸리지 않고 not-found로 간다
    path: '/community/:id(\\d+)',
    name: 'post-detail',
    component: () => import('@/views/community/PostDetailView.vue'),
  },
  {
    path: '/community/write',
    name: 'post-write',
    component: () => import('@/views/community/PostWriteView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/community/:id(\\d+)/edit',
    name: 'post-edit',
    component: () => import('@/views/community/PostWriteView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/feed',
    name: 'feed',
    component: () => import('@/views/community/FeedView.vue'),
    meta: { requiresAuth: true, nav: { label: '피드', order: 7, show: 'user' } },
  },
]
