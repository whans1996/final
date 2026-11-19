import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import http from '@/api/http'
import { fetchConcepts } from '@/api/concepts'

// API 계약(docs/specs/F-09-concept-list.md 3절)과 URL·파라미터 이름이 같은지 확인한다
function capture(data = {}) {
  const calls = []
  http.defaults.adapter = async (config) => {
    calls.push(config)
    return { data, status: 200, statusText: 'OK', headers: {}, config }
  }
  return calls
}

describe('api/concepts (F-09)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('AC10 fetchConcepts는 GET /api/concepts에 검색 조건을 쿼리로 보내고 페이지 응답을 돌려준다', async () => {
    const page = { items: [], page: 2, size: 20, total: 0 }
    const calls = capture(page)

    await expect(
      fetchConcepts({
        keyword: 'jwt',
        category: 'security',
        difficulty: 'JUNIOR',
        sort: 'LATEST',
        page: 2,
        size: 20,
      }),
    ).resolves.toEqual(page)

    expect(calls[0].method).toBe('get')
    expect(calls[0].url).toBe('/concepts')
    expect(calls[0].params).toEqual({
      keyword: 'jwt',
      category: 'security',
      difficulty: 'JUNIOR',
      sort: 'LATEST',
      page: 2,
      size: 20,
    })
  })

  it('AC10 빈 값(빈 문자열·null·undefined)은 쿼리에서 뺀다', async () => {
    const calls = capture({ items: [], page: 1, size: 20, total: 0 })

    await fetchConcepts({ keyword: '', category: null, difficulty: undefined, page: 1, size: 20 })

    expect(calls[0].params).toEqual({ page: 1, size: 20 })
  })
})
