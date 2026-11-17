import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// 백엔드(local 프로필, 8080)로 /api와 /uploads를 프록시한다. 브라우저는 같은 출처로 보므로 CORS 설정이 필요 없다.
const BACKEND = 'http://localhost:8080'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), vueDevTools()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': BACKEND,
      '/uploads': BACKEND,
    },
  },
})
