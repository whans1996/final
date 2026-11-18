import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import http from '@/api/http'
import { login, logout, refresh, signup } from '@/api/auth'

// API 계약(docs/specs/F-01-signup.md, F-02-login-jwt.md 3절)과 URL·메서드·본문이 같은지 확인한다
function capture(data = {}) {
  const calls = []
  http.defaults.adapter = async (config) => {
    calls.push(config)
    return { data, status: 200, statusText: 'OK', headers: {}, config }
  }
  return calls
}

describe('api/auth', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('F-01 AC1 signup은 POST /api/auth/signup에 가입 정보를 보내고 응답 본문을 돌려준다', async () => {
    const calls = capture({ id: 7 })
    const body = {
      email: 'user@example.com',
      password: 'vibe1234',
      nickname: '코딩초보',
      level: 'BEGINNER',
    }

    await expect(signup(body)).resolves.toEqual({ id: 7 })
    expect(calls[0].method).toBe('post')
    expect(calls[0].url).toBe('/auth/signup')
    expect(JSON.parse(calls[0].data)).toEqual(body)
  })

  it('F-02 AC8 login·refresh·logout은 POST /api/auth/* 를 부른다', async () => {
    const calls = capture({ accessToken: 't' })

    await login({ email: 'user@example.com', password: 'vibe1234' })
    await refresh()
    await logout()

    expect(calls.map((c) => `${c.method} ${c.url}`)).toEqual([
      'post /auth/login',
      'post /auth/refresh',
      'post /auth/logout',
    ])
  })
})
