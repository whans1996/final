import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import http from '@/api/http'
import { fetchRecommendations } from '@/api/recommendations'

// API 계약(docs/specs/F-16-recommendation.md 3절)과 URL·메서드·쿼리 이름이 같은지 확인한다
function capture(data = {}, status = 200) {
  const calls = []
  http.defaults.adapter = async (config) => {
    calls.push(config)
    return { data, status, statusText: 'OK', headers: {}, config }
  }
  return calls
}

describe('api/recommendations (F-16)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('AC1 fetchRecommendations는 size 없이 GET /api/recommendations', async () => {
    const body = { items: [] }
    const calls = capture(body)

    await expect(fetchRecommendations()).resolves.toEqual(body)

    expect(calls[0].method).toBe('get')
    expect(calls[0].url).toBe('/recommendations')
    expect(calls[0].params).toBeUndefined()
  })

  it('AC3 size를 주면 쿼리로 보낸다', async () => {
    const calls = capture({ items: [] })

    await fetchRecommendations({ size: 3 })

    expect(calls[0].params).toEqual({ size: 3 })
  })

  it('AC6 LLM 폴백·재시도 예산(2회 × (Gemini 60초 + Ollama 120초))보다 길게 기다려 RULE 응답을 받는다', async () => {
    const calls = capture({ items: [] })

    await fetchRecommendations()

    // 기본 15초에 끊으면 LLM 장애 때 서버가 200 + RULE로 답하기 전에 화면이 에러가 된다 (PR #62 Codex)
    expect(calls[0].timeout).toBeGreaterThan(2 * (60_000 + 120_000))
  })
})
