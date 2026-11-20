// 회원 프로필·설정 (공통 F-03). 헤더의 닉네임·로그아웃은 App.vue가 고정으로 붙인다
export default [
  {
    path: '/users/:id(\\d+)',
    name: 'user-profile',
    component: () => import('@/views/UserProfileView.vue'),
  },
  {
    // F-04 팔로워·팔로잉 목록 (?tab=followers|followings)
    path: '/users/:id(\\d+)/follows',
    name: 'user-follows',
    component: () => import('@/views/UserFollowsView.vue'),
  },
  {
    path: '/me/settings',
    name: 'my-settings',
    component: () => import('@/views/MySettingsView.vue'),
    meta: { requiresAuth: true },
  },
]
