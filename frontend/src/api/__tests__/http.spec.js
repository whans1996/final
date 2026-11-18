import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import http, {
  NETWORK_ERROR_MESSAGE,
  errorCode,
  errorMessage,
  setUnauthorizedHandler,
} from '@/api/http'
import * as authApi from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/auth')

// 실제 요청을 보내지 않도록 adapter를 바꿔 요청 설정만 확인한다
function captureRequest() {
  let captured
  http.defaults.adapter = async (config) => {
    captured = config
    return { data: {}, status: 200, statusText: 'OK', headers: {}, config }
  }
  return () => captured
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
})

// F-02 AC9: Access 만료(AUTH_401) → refresh 1회 → 원 요청 재시도
describe('http 401 처리 (F-02 AC9)', () => {
  let onUnauthorized

  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(authApi.refresh).mockReset()
    onUnauthorized = vi.fn()
    setUnauthorizedHandler(onUnauthorized)
  })

  /** 요청마다 응답을 정한다. handler(config, n)이 { status, data }를 돌려준다. */
  function respondWith(handler) {
    const calls = []
    http.defaults.adapter = async (config) => {
      calls.push(config)
      const { status, data } = handler(config, calls.length)
      if (status >= 400) {
        const error = new Error(`status ${status}`)
        error.config = config
        error.response = { status, data, headers: {}, config }
        throw error
      }
      return { data, status, statusText: 'OK', headers: {}, config }
    }
    return calls
  }

  const AUTH_401 = { code: 'AUTH_401', message: '로그인이 필요합니다.', details: null }
  const session = (token) => ({
    accessToken: token,
    expiresIn: 1800,
    user: { id: 1, nickname: '코딩초보', level: 'BEGINNER', role: 'USER' },
  })

  it('AC9 AUTH_401이면 refresh 후 새 토큰으로 1회 재시도한다', async () => {
    useAuthStore().setSession(session('expired'))
    authApi.refresh.mockResolvedValue(session('fresh'))
    const calls = respondWith((config) =>
      config.headers.Authorization === 'Bearer fresh'
        ? { status: 200, data: { ok: true } }
        : { status: 401, data: AUTH_401 },
    )

    const response = await http.get('/users/me')

    expect(response.data).toEqual({ ok: true })
    expect(authApi.refresh).toHaveBeenCalledTimes(1)
    expect(calls).toHaveLength(2)
    expect(useAuthStore().accessToken).toBe('fresh')
    expect(onUnauthorized).not.toHaveBeenCalled()
  })

  it('AC9 동시에 여러 요청이 401이어도 refresh는 한 번만 부른다', async () => {
    useAuthStore().setSession(session('expired'))
    authApi.refresh.mockResolvedValue(session('fresh'))
    respondWith((config) =>
      config.headers.Authorization === 'Bearer fresh'
        ? { status: 200, data: {} }
        : { status: 401, data: AUTH_401 },
    )

    await Promise.all([http.get('/a'), http.get('/b'), http.get('/c')])

    expect(authApi.refresh).toHaveBeenCalledTimes(1)
  })

  it('AC9 refresh가 실패하면 로그인 상태를 지우고 로그인 화면으로 보낸다', async () => {
    useAuthStore().setSession(session('expired'))
    authApi.refresh.mockRejectedValue(new Error('AUTH_002'))
    respondWith(() => ({ status: 401, data: AUTH_401 }))

    await expect(http.get('/users/me')).rejects.toThrow('status 401')

    expect(useAuthStore().isLoggedIn).toBe(false)
    expect(onUnauthorized).toHaveBeenCalledTimes(1)
  })

  it('AC9 재시도한 요청도 401이면 더 재시도하지 않고 로그인 화면으로 보낸다', async () => {
    useAuthStore().setSession(session('expired'))
    authApi.refresh.mockResolvedValue(session('fresh'))
    const calls = respondWith(() => ({ status: 401, data: AUTH_401 }))

    await expect(http.get('/users/me')).rejects.toThrow('status 401')

    expect(calls).toHaveLength(2)
    expect(authApi.refresh).toHaveBeenCalledTimes(1)
    expect(useAuthStore().isLoggedIn).toBe(false)
    expect(onUnauthorized).toHaveBeenCalledTimes(1)
  })

  it('AC9 AUTH_401이 아닌 401과 /auth 요청의 401은 재시도하지 않는다', async () => {
    useAuthStore().setSession(session('token'))
    respondWith(() => ({
      status: 401,
      data: { code: 'AUTH_001', message: '이메일 또는 비밀번호가 올바르지 않습니다.' },
    }))
    await expect(http.post('/auth/login', {})).rejects.toThrow('status 401')

    respondWith(() => ({ status: 401, data: AUTH_401 }))
    await expect(http.post('/auth/logout')).rejects.toThrow('status 401')

    expect(authApi.refresh).not.toHaveBeenCalled()
    expect(onUnauthorized).not.toHaveBeenCalled()
  })

  it.each([
    ['다른 코드', { code: 'AUTH_002', message: '로그인이 만료되었습니다. 다시 로그인하세요.' }],
    ['코드 없음', {}],
  ])('AC9 /auth 밖의 요청이라도 AUTH_401이 아닌 401(%s)은 재시도하지 않는다', async (_, data) => {
    useAuthStore().setSession(session('token'))
    const calls = respondWith(() => ({ status: 401, data }))

    await expect(http.get('/users/me')).rejects.toThrow('status 401')

    expect(calls).toHaveLength(1)
    expect(authApi.refresh).not.toHaveBeenCalled()
    expect(onUnauthorized).not.toHaveBeenCalled()
    expect(useAuthStore().isLoggedIn).toBe(true)
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
