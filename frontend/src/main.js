import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { setUnauthorizedHandler } from './api/http'
import { useAuthStore } from './stores/auth'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)

// 재발급까지 실패하면 로그인 화면으로 보내고, 로그인 후 지금 주소로 돌아온다 (F-02 AC9)
setUnauthorizedHandler(() => {
  const current = router.currentRoute.value
  if (current.name !== 'login') {
    router.push({ name: 'login', query: { redirect: current.fullPath } })
  }
})

// 첫 화면의 인증 가드가 로그인 상태를 알 수 있도록 세션 복원(refresh 쿠키) 후에 라우터를 붙인다 (F-02 AC8)
useAuthStore(pinia)
  .restore()
  .finally(() => {
    app.use(router)
    app.mount('#app')
  })
