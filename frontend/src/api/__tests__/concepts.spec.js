import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import http from '@/api/http'
import {
  createReview,
  deleteReview,
  fetchConcept,
  fetchConcepts,
  fetchMyReview,
  fetchReviews,
  fetchUserWishes,
  unwishConcept,
  updateReview,
  wishConcept,
} from '@/api/concepts'

// API 계약(docs/specs/F-09-concept-list.md 3절)과 URL·파라미터 이름이 같은지 확인한다
function capture(data = {}, status = 200) {
  const calls = []
  http.defaults.adapter = async (config) => {
    calls.push(config)
    return { data, status, statusText: 'OK', headers: {}, config }
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

describe('api/concepts (F-10)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('fetchConcept는 GET /api/concepts/{id}로 상세를 받는다', async () => {
    const detail = { id: 57, name: 'JWT', appearances: [], related: [] }
    const calls = capture(detail)

    await expect(fetchConcept(57)).resolves.toEqual(detail)

    expect(calls[0].method).toBe('get')
    expect(calls[0].url).toBe('/concepts/57')
  })
})

describe('api/concepts (F-11)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  const review = {
    id: 301,
    rating: 5,
    content: '좋아요',
    authorId: 7,
    authorNickname: '감자',
    createdAt: '2026-12-02T21:14:03+09:00',
  }

  it('fetchReviews는 GET /api/concepts/{id}/reviews에 page·size를 보낸다', async () => {
    const page = { items: [review], page: 2, size: 10, total: 11 }
    const calls = capture(page)

    await expect(fetchReviews(57, { page: 2, size: 10 })).resolves.toEqual(page)

    expect(calls[0].method).toBe('get')
    expect(calls[0].url).toBe('/concepts/57/reviews')
    expect(calls[0].params).toEqual({ page: 2, size: 10 })
  })

  it('fetchMyReview는 GET .../reviews/me로 내 리뷰를 받고 204면 null이다', async () => {
    const calls = capture(review)
    await expect(fetchMyReview(57)).resolves.toEqual(review)
    expect(calls[0].method).toBe('get')
    expect(calls[0].url).toBe('/concepts/57/reviews/me')

    capture('', 204)
    await expect(fetchMyReview(57)).resolves.toBeNull()
  })

  it('createReview는 POST .../reviews에 rating·content를 보낸다', async () => {
    const saved = { review, avgRating: 4.3, reviewCount: 13 }
    const calls = capture(saved, 201)

    await expect(createReview(57, { rating: 5, content: '좋아요' })).resolves.toEqual(saved)

    expect(calls[0].method).toBe('post')
    expect(calls[0].url).toBe('/concepts/57/reviews')
    expect(JSON.parse(calls[0].data)).toEqual({ rating: 5, content: '좋아요' })
  })

  it('updateReview는 PUT .../reviews/me에 rating·content를 보낸다', async () => {
    const saved = { review, avgRating: 4.5, reviewCount: 2 }
    const calls = capture(saved)

    await expect(updateReview(57, { rating: 5, content: '좋아요' })).resolves.toEqual(saved)

    expect(calls[0].method).toBe('put')
    expect(calls[0].url).toBe('/concepts/57/reviews/me')
    expect(JSON.parse(calls[0].data)).toEqual({ rating: 5, content: '좋아요' })
  })

  it('deleteReview는 DELETE .../reviews/me로 바뀐 평점을 받는다', async () => {
    const calls = capture({ avgRating: 4.2, reviewCount: 12 })

    await expect(deleteReview(57)).resolves.toEqual({ avgRating: 4.2, reviewCount: 12 })

    expect(calls[0].method).toBe('delete')
    expect(calls[0].url).toBe('/concepts/57/reviews/me')
  })
})

describe('api/concepts (F-12)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('wishConcept는 POST /api/concepts/{id}/wish를 본문 없이 보내고 찜 상태를 받는다', async () => {
    const result = { wished: true, wishCount: 13, reviewPlanDate: '2026-12-04' }
    const calls = capture(result)

    await expect(wishConcept(57)).resolves.toEqual(result)

    expect(calls[0].method).toBe('post')
    expect(calls[0].url).toBe('/concepts/57/wish')
    expect(calls[0].data).toBeUndefined()
  })

  it('unwishConcept는 DELETE /api/concepts/{id}/wish로 바뀐 찜 수를 받는다', async () => {
    const result = { wished: false, wishCount: 12, reviewPlanDate: null }
    const calls = capture(result)

    await expect(unwishConcept(57)).resolves.toEqual(result)

    expect(calls[0].method).toBe('delete')
    expect(calls[0].url).toBe('/concepts/57/wish')
  })

  it('fetchUserWishes는 GET /api/users/{id}/wishes에 page·size를 보낸다', async () => {
    const page = { items: [], page: 2, size: 12, total: 13 }
    const calls = capture(page)

    await expect(fetchUserWishes(3, { page: 2, size: 12 })).resolves.toEqual(page)

    expect(calls[0].method).toBe('get')
    expect(calls[0].url).toBe('/users/3/wishes')
    expect(calls[0].params).toEqual({ page: 2, size: 12 })
  })
})
