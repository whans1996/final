// 회원 프로필·설정 (공통 F-03). 헤더의 닉네임·로그아웃은 App.vue가 고정으로 붙인다
export default [
  {
    path: '/users/:id(\\d+)',
    name: 'user-profile',
    component: () => import('@/views/UserProfileView.vue'),
  },
  {
    path: '/me/settings',
    name: 'my-settings',
    component: () => import('@/views/MySettingsView.vue'),
    meta: { requiresAuth: true },
  },
]
