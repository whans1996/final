import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import http from '@/api/http'
import {
  cancelChallenge,
  fetchChallenge,
  fetchChallenges,
  fetchMyChallenges,
  fetchUserChallenges,
  joinChallenge,
} from '@/api/challenges'

// API 계약(docs/specs/F-25-challenge-participation.md 3절)과 URL·메서드·쿼리가 같은지 확인한다
function capture(data = {}) {
  const calls = []
  http.defaults.adapter = async (config) => {
    calls.push(config)
    return { data, status: 200, statusText: 'OK', headers: {}, config }
  }
  return calls
}

const line = (c) => `${c.method} ${c.url}`

describe('api/challenges (F-25)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('AC12 목록은 GET /api/challenges, 빈 status는 쿼리에서 뺀다', async () => {
    const page = { items: [], page: 1, size: 12, total: 0 }
    const calls = capture(page)

    await expect(fetchChallenges({ status: 'UPCOMING', page: 2, size: 12 })).resolves.toEqual(page)
    await fetchChallenges({ status: '', page: 1 })

    expect(calls.map(line)).toEqual(['get /challenges', 'get /challenges'])
    expect(calls[0].params).toEqual({ status: 'UPCOMING', page: 2, size: 12 })
    expect(calls[1].params).toEqual({ page: 1 })
  })

  it('AC13 상세는 GET, 참여는 POST, 취소는 DELETE /api/challenges/{id}/participation', async () => {
    const result = { joined: true, participantCount: 1, participation: null }
    const calls = capture(result)

    await fetchChallenge(7)
    await expect(joinChallenge(7)).resolves.toEqual(result)
    await cancelChallenge(7)

    expect(calls.map(line)).toEqual([
      'get /challenges/7',
      'post /challenges/7/participation',
      'delete /challenges/7/participation',
    ])
  })

  it('AC14·AC15 내 목록은 /api/users/me/challenges, 회원 목록은 /api/users/{id}/challenges', async () => {
    const calls = capture({ items: [], page: 1, size: 3, total: 0 })

    await fetchMyChallenges({ status: 'ONGOING', page: 1, size: 3 })
    await fetchUserChallenges(5, { page: 2, size: 6 })

    expect(calls.map(line)).toEqual(['get /users/me/challenges', 'get /users/5/challenges'])
    expect(calls[0].params).toEqual({ status: 'ONGOING', page: 1, size: 3 })
    expect(calls[1].params).toEqual({ page: 2, size: 6 })
  })
})
