import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import http from '@/api/http'
import {
  createAdminChallenge,
  deleteAdminChallenge,
  fetchAdminChallenge,
  fetchAdminChallenges,
  updateAdminChallenge,
} from '@/api/adminChallenges'

// API 계약(docs/specs/F-24-challenge-admin.md 3절)과 URL·메서드·파트 이름이 같은지 확인한다
function capture(data = {}) {
  const calls = []
  http.defaults.adapter = async (config) => {
    calls.push(config)
    return { data, status: 200, statusText: 'OK', headers: {}, config }
  }
  return calls
}

const form = {
  title: '12월 퀴즈 챌린지',
  description: '',
  startDate: '2026-12-01',
  endDate: '2026-12-31',
  goalType: 'QUIZ_COUNT',
  goalValue: 10,
}

const entries = (data) => Object.fromEntries(data.entries())

describe('api/adminChallenges (F-24)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('AC11 fetchAdminChallenges는 빈 조건을 빼고 GET /api/admin/challenges를 부른다', async () => {
    const calls = capture({ items: [], page: 1, size: 20, total: 0 })

    await fetchAdminChallenges({ status: '', page: 2, size: 20 })

    expect(calls[0].method).toBe('get')
    expect(calls[0].url).toBe('/admin/challenges')
    expect(calls[0].params).toEqual({ page: 2, size: 20 })
  })

  it('AC13 fetchAdminChallenge는 GET /api/admin/challenges/{id}', async () => {
    const calls = capture({ id: 7 })

    await expect(fetchAdminChallenge(7)).resolves.toEqual({ id: 7 })
    expect(calls[0].url).toBe('/admin/challenges/7')
  })

  it('AC13 createAdminChallenge는 POST에 폼 필드와 이미지를 FormData로 보내고 removeImage는 넣지 않는다', async () => {
    const calls = capture({ id: 7 })
    const image = new File(['png'], 'cover.png', { type: 'image/png' })

    await expect(createAdminChallenge({ ...form, image })).resolves.toEqual({ id: 7 })

    expect(calls[0].method).toBe('post')
    expect(calls[0].url).toBe('/admin/challenges')
    expect(calls[0].data).toBeInstanceOf(FormData)
    const parts = entries(calls[0].data)
    expect(parts).toMatchObject({
      title: '12월 퀴즈 챌린지',
      description: '',
      startDate: '2026-12-01',
      endDate: '2026-12-31',
      goalType: 'QUIZ_COUNT',
      goalValue: '10',
    })
    expect(parts.image).toBeInstanceOf(File)
    expect(parts.image.name).toBe('cover.png')
    expect(parts).not.toHaveProperty('removeImage')
  })

  it('AC13 updateAdminChallenge는 PUT에 removeImage를 함께 보내고 이미지가 없으면 image 파트를 뺀다', async () => {
    const calls = capture({ id: 7 })

    await updateAdminChallenge(7, { ...form, removeImage: true })

    expect(calls[0].method).toBe('put')
    expect(calls[0].url).toBe('/admin/challenges/7')
    const parts = entries(calls[0].data)
    expect(parts.removeImage).toBe('true')
    expect(parts).not.toHaveProperty('image')

    await updateAdminChallenge(7, form)
    expect(entries(calls[1].data).removeImage).toBe('false')
  })

  it('AC12 deleteAdminChallenge는 DELETE /api/admin/challenges/{id}', async () => {
    const calls = capture()

    await expect(deleteAdminChallenge(7)).resolves.toBeUndefined()
    expect(calls[0].method).toBe('delete')
    expect(calls[0].url).toBe('/admin/challenges/7')
  })
})
