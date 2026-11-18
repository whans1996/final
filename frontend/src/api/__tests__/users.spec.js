import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import http from '@/api/http'
import { fetchMe, fetchUser, fetchUserStats, updateMe, withdraw } from '@/api/users'

// API 계약(docs/specs/F-03-profile.md 3절)과 URL·메서드·본문이 같은지 확인한다
function capture(data = {}) {
  const calls = []
  http.defaults.adapter = async (config) => {
    calls.push(config)
    return { data, status: 200, statusText: 'OK', headers: {}, config }
  }
  return calls
}

describe('api/users (F-03)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('AC1·AC2·AC3 조회는 GET /api/users/me, /api/users/{id}, /api/users/{id}/stats', async () => {
    const calls = capture({ id: 3 })

    await expect(fetchMe()).resolves.toEqual({ id: 3 })
    await fetchUser(3)
    await fetchUserStats(3)

    expect(calls.map((c) => `${c.method} ${c.url}`)).toEqual([
      'get /users/me',
      'get /users/3',
      'get /users/3/stats',
    ])
  })

  it('AC4 updateMe는 PATCH /api/users/me에 보낸 필드만 담는다', async () => {
    const calls = capture({ id: 1 })

    await updateMe({ nickname: '주니어개발자', level: 'JUNIOR' })

    expect(calls[0].method).toBe('patch')
    expect(calls[0].url).toBe('/users/me')
    expect(JSON.parse(calls[0].data)).toEqual({ nickname: '주니어개발자', level: 'JUNIOR' })
  })

  it('AC6 withdraw는 DELETE /api/users/me 본문에 비밀번호를 담는다', async () => {
    const calls = capture()

    await withdraw('vibe1234')

    expect(calls[0].method).toBe('delete')
    expect(calls[0].url).toBe('/users/me')
    expect(JSON.parse(calls[0].data)).toEqual({ password: 'vibe1234' })
  })
})
