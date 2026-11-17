import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import HomeView from '@/views/HomeView.vue'

export const routes = [
  { path: '/', name: 'home', component: HomeView },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
  },
]

/**
 * 인증 가드. 라우트에 meta.requiresAuth / meta.requiresAdmin을 붙여 쓴다.
 * 로그인 화면(F-02)이 생기면 비로그인 이동 대상을 { name: 'login' }으로 바꾼다.
 */
export function authGuard(to, auth) {
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'home', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { name: 'home' }
  }
  return true
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to) => authGuard(to, useAuthStore()))

export default router
