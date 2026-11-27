import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import http from '@/api/http'
import { fetchAdminConcepts } from '@/api/adminConcepts'

function capture(data = {}, status = 200) {
  const calls = []
  http.defaults.adapter = async (config) => {
    calls.push(config)
    return { data, status, statusText: 'OK', headers: {}, config }
  }
  return calls
}

describe('api/adminConcepts signal (V-4)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('AC3 fetchAdminConcepts는 두 번째 인자의 signal을 요청 설정에 싣고, 없으면 지금과 같은 요청을 보낸다', async () => {
    const calls = capture({ items: [], page: 1, size: 20, total: 0 })
    const controller = new AbortController()

    await fetchAdminConcepts({ status: 'DRAFT', keyword: '' }, { signal: controller.signal })
    await fetchAdminConcepts({ status: 'DRAFT', keyword: '' })

    expect(calls[0].method).toBe('get')
    expect(calls[0].url).toBe('/admin/concepts')
    expect(calls[0].signal).toBe(controller.signal)
    expect(calls[0].params).toEqual({ status: 'DRAFT' })
    expect(calls[1].signal).toBeUndefined()
    expect(calls[1].params).toEqual({ status: 'DRAFT' })
  })
})
