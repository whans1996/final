import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import http, { NETWORK_ERROR_MESSAGE, errorCode, errorMessage } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

// 실제 요청을 보내지 않도록 adapter를 바꿔 요청 설정만 확인한다
function captureRequest() {
  let captured
  http.defaults.adapter = async (config) => {
    captured = config
    return { data: {}, status: 200, statusText: 'OK', headers: {}, config }
  }
  return () => captured
}

function rejectWith(status, data) {
  http.defaults.adapter = async (config) => {
    const error = new Error(`status ${status}`)
    error.config = config
    error.response = { status, data, headers: {}, config }
    throw error
  }
}

describe('http', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('/api를 기준 주소로 쓴다', async () => {
    const lastRequest = captureRequest()
    await http.get('/concepts')
    expect(lastRequest().baseURL).toBe('/api')
  })

  it('로그인 상태면 Authorization 헤더에 Bearer 토큰을 붙인다', async () => {
    useAuthStore().setSession({ accessToken: 'token-123', user: null })
    const lastRequest = captureRequest()
    await http.get('/users/me')
    expect(lastRequest().headers.Authorization).toBe('Bearer token-123')
  })

  it('비로그인 상태면 Authorization 헤더를 붙이지 않는다', async () => {
    const lastRequest = captureRequest()
    await http.get('/concepts')
    expect(lastRequest().headers.Authorization).toBeUndefined()
  })

  it('401을 받으면 로그인 상태를 지운다', async () => {
    const auth = useAuthStore()
    auth.setSession({ accessToken: 'expired', user: null })
    rejectWith(401, { code: 'AUTH_401', message: '로그인이 필요합니다.', details: null })

    await expect(http.get('/users/me')).rejects.toThrow('status 401')
    expect(auth.isLoggedIn).toBe(false)
  })
})

describe('errorMessage / errorCode', () => {
  it('백엔드가 보낸 한국어 메시지와 코드를 꺼낸다', () => {
    const error = {
      response: { data: { code: 'CONCEPT_404', message: '개념 카드를 찾을 수 없습니다.' } },
    }
    expect(errorMessage(error)).toBe('개념 카드를 찾을 수 없습니다.')
    expect(errorCode(error)).toBe('CONCEPT_404')
  })

  it('응답이 없으면(네트워크 오류) 기본 안내 문구와 null', () => {
    const error = new Error('Network Error')
    expect(errorMessage(error)).toBe(NETWORK_ERROR_MESSAGE)
    expect(errorCode(error)).toBeNull()
  })
})
