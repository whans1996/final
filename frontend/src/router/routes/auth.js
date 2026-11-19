// 로그인·회원가입 (공통 F-01·F-02). meta.nav 규칙은 utils/navItems.js
export default [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { guestOnly: true, nav: { label: '로그인', order: 10, show: 'guest' } },
  },
  {
    path: '/signup',
    name: 'signup',
    component: () => import('@/views/SignupView.vue'),
    meta: { guestOnly: true, nav: { label: '회원가입', order: 20, show: 'guest' } },
  },
]
