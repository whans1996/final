import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import http from '@/api/http'
import {
  createComment,
  createPost,
  deleteComment,
  deletePost,
  fetchComments,
  fetchPost,
  fetchPosts,
  updateComment,
  updatePost,
} from '@/api/posts'

// API 계약(docs/specs/F-17-community-board.md 3절)과 URL·메서드·필드 이름이 같은지 확인한다
function capture(data = {}, status = 200) {
  const calls = []
  http.defaults.adapter = async (config) => {
    calls.push(config)
    return { data, status, statusText: 'OK', headers: {}, config }
  }
  return calls
}

describe('api/posts (F-17)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('AC13 fetchPosts는 GET /api/posts에 빈 값을 뺀 조건을 쿼리로 보낸다', async () => {
    const page = { items: [], page: 2, size: 20, total: 0 }
    const calls = capture(page)

    await expect(
      fetchPosts({ boardType: 'QNA', keyword: '트랜잭션', page: 2, size: 20 }),
    ).resolves.toEqual(page)
    await fetchPosts({ boardType: '', keyword: '', page: 1, size: 20 })

    expect(calls[0].method).toBe('get')
    expect(calls[0].url).toBe('/posts')
    expect(calls[0].params).toEqual({ boardType: 'QNA', keyword: '트랜잭션', page: 2, size: 20 })
    expect(calls[1].params).toEqual({ page: 1, size: 20 })
  })

  it('AC15 fetchPost는 GET /api/posts/{id}', async () => {
    const calls = capture({ id: 42 })

    await expect(fetchPost(42)).resolves.toEqual({ id: 42 })
    expect(calls[0].method).toBe('get')
    expect(calls[0].url).toBe('/posts/42')
    expect(calls[0].params).toBeUndefined()
  })

  it('AC18 #209 fetchPost(id, { countView: false })는 ?countView=false로 조회수를 올리지 않는다', async () => {
    const calls = capture({ id: 42 })

    await fetchPost(42, { countView: false })
    expect(calls[0].url).toBe('/posts/42')
    expect(calls[0].params).toEqual({ countView: false })
  })

  it('AC17 createPost는 POST /api/posts에 PostRequest 본문을 보내고 {id}를 돌려준다', async () => {
    const calls = capture({ id: 42 }, 201)
    const body = {
      boardType: 'QNA',
      title: '제목',
      content: '본문',
      analysisId: 15,
      conceptId: null,
    }

    await expect(createPost(body)).resolves.toEqual({ id: 42 })
    expect(calls[0].method).toBe('post')
    expect(calls[0].url).toBe('/posts')
    expect(JSON.parse(calls[0].data)).toEqual(body)
  })

  it('AC18 updatePost는 PUT /api/posts/{id}로 통째로 보낸다', async () => {
    const calls = capture({ id: 42 })
    const body = {
      boardType: 'FREE',
      title: '제목',
      content: '본문',
      analysisId: null,
      conceptId: 12,
    }

    await updatePost(42, body)
    expect(calls[0].method).toBe('put')
    expect(calls[0].url).toBe('/posts/42')
    expect(JSON.parse(calls[0].data)).toEqual(body)
  })

  it('AC16 deletePost는 DELETE /api/posts/{id}', async () => {
    const calls = capture('', 204)

    await deletePost(42)
    expect(calls[0].method).toBe('delete')
    expect(calls[0].url).toBe('/posts/42')
  })
})

// F-18 댓글 (docs/specs/F-18-comments.md 3절)
describe('api/posts 댓글 (F-18)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('AC11 fetchComments는 GET /api/posts/{postId}/comments이고 {items, total}을 돌려준다', async () => {
    const list = { items: [], total: 0 }
    const calls = capture(list)

    await expect(fetchComments(42)).resolves.toEqual(list)
    expect(calls[0].method).toBe('get')
    expect(calls[0].url).toBe('/posts/42/comments')
  })

  it('AC12 createComment는 POST /api/posts/{postId}/comments에 content만 보낸다', async () => {
    const calls = capture({ id: 101 }, 201)

    await expect(createComment(42, { content: '답글', extra: 1 })).resolves.toEqual({ id: 101 })
    expect(calls[0].method).toBe('post')
    expect(calls[0].url).toBe('/posts/42/comments')
    expect(JSON.parse(calls[0].data)).toEqual({ content: '답글' })
  })

  it('AC13 updateComment는 PUT, deleteComment는 DELETE /api/posts/{postId}/comments/{commentId}', async () => {
    const calls = capture({ id: 101 })

    await updateComment(42, 101, { content: '고침' })
    expect(calls[0].method).toBe('put')
    expect(calls[0].url).toBe('/posts/42/comments/101')
    expect(JSON.parse(calls[0].data)).toEqual({ content: '고침' })

    await expect(deleteComment(42, 101)).resolves.toBeUndefined()
    expect(calls[1].method).toBe('delete')
    expect(calls[1].url).toBe('/posts/42/comments/101')
  })
})
