import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

/**
 * 모든 HTTP 호출이 쓰는 Axios 인스턴스. 컴포넌트와 store는 axios를 직접 import하지 않고
 * src/api/<도메인>.js의 함수를 통해 호출한다 (frontend/CLAUDE.md).
 */
const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    // TODO(F-02): 401 AUTH_401이면 /api/auth/refresh로 재발급 후 1회 재시도 (docs/conventions.md 4절)
    if (error.response?.status === 401) {
      useAuthStore().clear()
    }
    return Promise.reject(error)
  },
)

export const NETWORK_ERROR_MESSAGE = '서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.'

/** 사용자에게 보여 줄 메시지. 백엔드가 보낸 한국어 message를 우선한다. */
export function errorMessage(error) {
  return error?.response?.data?.message ?? NETWORK_ERROR_MESSAGE
}

/** 분기용 에러 코드 (예: 'CONCEPT_404'). 없으면 null. */
export function errorCode(error) {
  return error?.response?.data?.code ?? null
}

export default http
