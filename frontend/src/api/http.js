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

let unauthorizedHandler = () => {}

/** 재발급까지 실패해 다시 로그인해야 할 때 부를 함수 (main.js가 로그인 화면 이동을 등록한다). */
export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler
}

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

/**
 * Access 만료(401 AUTH_401) → refresh 1회 → 원 요청 재시도 (F-02 AC9, docs/conventions.md 4절).
 * /auth/* 요청(로그인 실패 AUTH_001, 재발급 실패 AUTH_002 등)은 재시도하지 않는다.
 */
http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const { config, response } = error
    const isAuthRequest = config?.url?.startsWith('/auth/')
    if (response?.status !== 401 || errorCode(error) !== 'AUTH_401' || isAuthRequest) {
      return Promise.reject(error)
    }

    const auth = useAuthStore()
    if (!config.retried) {
      try {
        const token = await auth.refresh()
        config.retried = true
        config.headers.Authorization = `Bearer ${token}`
        return http(config)
      } catch {
        // 아래에서 로그인 화면으로 보낸다
      }
    }
    auth.clear()
    unauthorizedHandler()
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

/** 필드별 사유 (COMMON_400, USER_409의 details). 없으면 빈 객체. */
export function errorDetails(error) {
  return error?.response?.data?.details ?? {}
}

export default http
