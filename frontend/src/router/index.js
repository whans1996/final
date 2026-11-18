import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import HomeView from '@/views/HomeView.vue'

export const routes = [
  { path: '/', name: 'home', component: HomeView },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { guestOnly: true },
  },
  {
    path: '/signup',
    name: 'signup',
    component: () => import('@/views/SignupView.vue'),
    meta: { guestOnly: true },
  },
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
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
  },
]

/**
 * 인증 가드. 라우트에 meta.requiresAuth / meta.requiresAdmin / meta.guestOnly를 붙여 쓴다.
 * 비로그인으로 보호된 화면에 오면 로그인 화면으로 보내고, 로그인 후 원래 주소(redirect)로 돌아온다.
 */
export function authGuard(to, auth) {
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { name: 'home' }
  }
  if (to.meta.guestOnly && auth.isLoggedIn) {
    return { name: 'home' }
  }
  return true
}

/**
 * 로그인·가입 후 돌아갈 주소. 다른 사이트로 나가지 않게 앱 내부 경로만 허용한다.
 * 브라우저는 URL의 `\`를 `/`로 읽으므로 `/\evil.com`도 `//evil.com`처럼 막는다.
 */
export function redirectTarget(query) {
  const target = query.redirect
  return typeof target === 'string' && target.startsWith('/') && !/^\/[/\\]/.test(target)
    ? target
    : '/'
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to) => authGuard(to, useAuthStore()))

export default router
