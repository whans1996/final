// 로그아웃 후 home으로 (F-29b AC11, 기존 App 동작 유지). 닉네임 메뉴와 모바일 드로어가 같이 쓴다.
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

export function useLogout() {
  const auth = useAuthStore()
  const router = useRouter()
  return async function logout() {
    await auth.logout()
    await router.push({ name: 'home' })
  }
}
