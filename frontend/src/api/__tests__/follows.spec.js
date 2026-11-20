import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import http from '@/api/http'
import { fetchFollowStatus, fetchFollowers, fetchFollowings, follow, unfollow } from '@/api/follows'
import { fetchFeed } from '@/api/feed'

// API 계약(docs/specs/F-04-follow-feed.md 3절)과 URL·메서드·쿼리가 같은지 확인한다
function capture(data = {}) {
  const calls = []
  http.defaults.adapter = async (config) => {
    calls.push(config)
    return { data, status: 200, statusText: 'OK', headers: {}, config }
  }
  return calls
}

describe('api/follows·feed (F-04)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('AC1·AC2·AC4 팔로우는 POST, 언팔로우는 DELETE, 상태는 GET /api/users/{id}/follow', async () => {
    const status = { following: true, followerCount: 2, followingCount: 1 }
    const calls = capture(status)

    await expect(follow(7)).resolves.toEqual(status)
    await unfollow(7)
    await fetchFollowStatus(7)

    expect(calls.map((c) => `${c.method} ${c.url}`)).toEqual([
      'post /users/7/follow',
      'delete /users/7/follow',
      'get /users/7/follow',
    ])
  })

  it('AC5 팔로워·팔로잉 목록은 page·size 쿼리로 GET한다', async () => {
    const calls = capture({ items: [], page: 2, size: 20, total: 0 })

    await fetchFollowers(7, 2)
    await fetchFollowings(7, 1)

    expect(calls.map((c) => `${c.method} ${c.url}`)).toEqual([
      'get /users/7/followers',
      'get /users/7/followings',
    ])
    expect(calls[0].params).toEqual({ page: 2, size: 20 })
    expect(calls[1].params).toEqual({ page: 1, size: 20 })
  })

  it('AC6 피드는 GET /api/feed?page=&size=20', async () => {
    const calls = capture({ items: [], page: 3, size: 20, total: 0 })

    await expect(fetchFeed(3)).resolves.toEqual({ items: [], page: 3, size: 20, total: 0 })

    expect(calls[0].method).toBe('get')
    expect(calls[0].url).toBe('/feed')
    expect(calls[0].params).toEqual({ page: 3, size: 20 })
  })
})
