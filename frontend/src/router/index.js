import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import HomeView from '@/views/HomeView.vue'
import adminRoutes from './routes/admin'
import analysisRoutes from './routes/analysis'
import authRoutes from './routes/auth'
import challengeRoutes from './routes/challenge'
import communityRoutes from './routes/community'
import conceptRoutes from './routes/concept'
import dashboardRoutes from './routes/dashboard'
import reviewPlanRoutes from './routes/reviewplan'
import userRoutes from './routes/user'

// 라우트는 도메인 파일(routes/*.js)에 두고 여기서는 합치기만 한다 (F-00b). 기능 PR은 자기 도메인 파일만 고친다.
// 헤더 메뉴는 route의 meta.nav, 관리자 사이드 메뉴는 /admin 자식의 meta.adminNav로 정한다 (utils/navItems.js)
export const routes = [
  { path: '/', name: 'home', component: HomeView },
  ...authRoutes,
  ...dashboardRoutes,
  ...userRoutes,
  ...analysisRoutes,
  ...conceptRoutes,
  ...reviewPlanRoutes,
  ...communityRoutes,
  ...challengeRoutes,
  ...adminRoutes,
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
 * 로그인·가입 후 돌아갈 주소. 다른 사이트로 나가지 않게 앱 내부 경로만 허용하고,
 * 없거나 허용되지 않으면 대시보드로 보낸다 (F-16 AC9).
 * 브라우저는 URL의 `\`를 `/`로 읽으므로 `/\evil.com`도 `//evil.com`처럼 막는다.
 */
export function redirectTarget(query) {
  const target = query.redirect
  return typeof target === 'string' && target.startsWith('/') && !/^\/[/\\]/.test(target)
    ? target
    : '/dashboard'
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to) => authGuard(to, useAuthStore()))

export default router
