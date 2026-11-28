import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import http from '@/api/http'
import { fetchInterview, requestInterview } from '@/api/interviews'

// API 계약(docs/specs/I-1a-interview-prep.md 3절)과 URL·메서드가 같은지 확인한다
function capture(data = {}) {
  const calls = []
  http.defaults.adapter = async (config) => {
    calls.push(config)
    return { data, status: 200, statusText: 'OK', headers: {}, config }
  }
  return calls
}

describe('api/interviews (I-1a)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('AC9 fetchInterview는 GET /api/analyses/{id}/interview에 signal을 넘긴다', async () => {
    const calls = capture({ analysisId: 41, status: 'NONE' })
    const controller = new AbortController()

    await expect(fetchInterview(41, { signal: controller.signal })).resolves.toEqual({
      analysisId: 41,
      status: 'NONE',
    })

    expect(calls[0].method).toBe('get')
    expect(calls[0].url).toBe('/analyses/41/interview')
    expect(calls[0].signal).toBe(controller.signal)
  })

  it('AC1 requestInterview는 본문 없이 POST /api/analyses/{id}/interview를 보낸다', async () => {
    const calls = capture({ analysisId: 41, status: 'PENDING' })

    await expect(requestInterview(41)).resolves.toEqual({ analysisId: 41, status: 'PENDING' })

    expect(calls[0].method).toBe('post')
    expect(calls[0].url).toBe('/analyses/41/interview')
    expect(calls[0].data).toBeUndefined()
  })
})
