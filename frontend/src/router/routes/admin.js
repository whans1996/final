// 관리자 (공통 F-00b). 자식 route는 각 도메인 파일의 adminChildren에서 가져온다.
// 부모의 requiresAuth·requiresAdmin은 vue-router가 자식의 to.meta에 합쳐 주므로 자식에 다시 달지 않는다.
// 사이드 메뉴는 자식의 meta.adminNav = { label, order }로 만든다 (utils/navItems.js adminNavItems)
import { adminChildren as challengeAdmin } from './challenge'
import { adminChildren as conceptAdmin } from './concept'

export default [
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: {
      requiresAuth: true,
      requiresAdmin: true,
      nav: { label: '관리자', order: 90, show: 'admin' },
    },
    children: [
      {
        path: '',
        name: 'admin-home',
        component: () => import('@/views/admin/AdminHomeView.vue'),
      },
      ...conceptAdmin,
      ...challengeAdmin,
    ],
  },
]
