// 프로젝트·버전 (A P-1a): SC-25 내 프로젝트 목록, SC-26 프로젝트 상세
export default [
  {
    path: '/projects',
    name: 'projects',
    component: () => import('@/views/project/ProjectListView.vue'),
    meta: {
      requiresAuth: true,
      nav: { label: '프로젝트', order: 25, show: 'user', group: 'learn', icon: 'FolderGit2' },
    },
  },
  {
    // 숫자가 아닌 id는 여기 걸리지 않고 not-found로 간다
    path: '/projects/:id(\\d+)',
    name: 'project-detail',
    component: () => import('@/views/project/ProjectDetailView.vue'),
    meta: { requiresAuth: true },
  },
]
