import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import http from '@/api/http'
import {
  createAnalysis,
  fetchAnalyses,
  fetchAnalysis,
  fetchAnalysisFile,
  fetchAnalysisFiles,
  fetchAnalysisStatus,
  fetchAnalysisFlows,
  requestAnalysisFlows,
  fetchAnalysisConcepts,
} from '@/api/analyses'

// API 계약(docs/specs/F-05a-analysis-request-zip.md 3절)과 URL·메서드·파트 이름이 같은지 확인한다
function capture(data = {}) {
  const calls = []
  http.defaults.adapter = async (config) => {
    calls.push(config)
    return { data, status: 200, statusText: 'OK', headers: {}, config }
  }
  return calls
}

describe('api/analyses (F-05a)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('AC14 createAnalysis는 POST /api/analyses에 file·level·consent 파트를 담은 FormData를 보낸다', async () => {
    const calls = capture({ analysisId: 42, status: 'FETCHING' })
    const file = new File(['PK'], 'todo-app.zip', { type: 'application/zip' })

    await expect(createAnalysis({ file, level: 'SENIOR', consent: true })).resolves.toEqual({
      analysisId: 42,
      status: 'FETCHING',
    })

    expect(calls[0].method).toBe('post')
    expect(calls[0].url).toBe('/analyses')
    const form = calls[0].data
    expect(form).toBeInstanceOf(FormData)
    expect(form.get('file').name).toBe('todo-app.zip')
    expect(form.get('level')).toBe('SENIOR')
    expect(form.get('consent')).toBe('true')
  })

  it('AC14 level이 없으면 level 파트를 보내지 않는다 (서버가 내 수준을 쓴다)', async () => {
    const calls = capture({ analysisId: 1, status: 'FETCHING' })

    await createAnalysis({ file: new File(['PK'], 'a.zip'), consent: true })

    expect(calls[0].data.has('level')).toBe(false)
  })

  it('F-05b AC13 file이 없으면 sourceUrl 파트를 보내고 file 파트는 보내지 않는다', async () => {
    const calls = capture({ analysisId: 57, status: 'FETCHING' })

    await createAnalysis({
      sourceUrl: 'https://github.com/octo/todo-app',
      level: 'SENIOR',
      consent: true,
    })

    expect(calls[0].method).toBe('post')
    expect(calls[0].url).toBe('/analyses')
    const form = calls[0].data
    expect(form.get('sourceUrl')).toBe('https://github.com/octo/todo-app')
    expect(form.has('file')).toBe(false)
    expect(form.get('level')).toBe('SENIOR')
    expect(form.get('consent')).toBe('true')
  })

  it('F-05b AC13 file이 있으면 sourceUrl 파트를 보내지 않는다', async () => {
    const calls = capture({ analysisId: 1, status: 'FETCHING' })

    await createAnalysis({ file: new File(['PK'], 'a.zip'), consent: true })

    expect(calls[0].data.has('file')).toBe(true)
    expect(calls[0].data.has('sourceUrl')).toBe(false)
  })

  it('AC16 fetchAnalyses는 GET /api/analyses에 page·size 쿼리를 붙인다', async () => {
    const calls = capture({ items: [], page: 2, size: 20, total: 0 })

    await expect(fetchAnalyses({ page: 2, size: 20 })).resolves.toEqual({
      items: [],
      page: 2,
      size: 20,
      total: 0,
    })

    expect(calls[0].method).toBe('get')
    expect(calls[0].url).toBe('/analyses')
    expect(calls[0].params).toEqual({ page: 2, size: 20 })
  })

  it('AC15 fetchAnalysisStatus는 GET /api/analyses/{id}/status', async () => {
    const calls = capture({ analysisId: 42, status: 'FILTERING', progress: 15 })

    await expect(fetchAnalysisStatus(42)).resolves.toMatchObject({ status: 'FILTERING' })

    expect(`${calls[0].method} ${calls[0].url}`).toBe('get /analyses/42/status')
  })
})

describe('api/analyses (F-06)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('AC13 fetchAnalysis는 GET /api/analyses/{id}', async () => {
    const calls = capture({ id: 42, title: 'todo-app', status: 'DONE', overview: null })

    await expect(fetchAnalysis(42)).resolves.toMatchObject({ id: 42, title: 'todo-app' })

    expect(`${calls[0].method} ${calls[0].url}`).toBe('get /analyses/42')
  })

  it('AC14 fetchAnalysisFiles는 GET /api/analyses/{id}/files', async () => {
    const files = [{ id: 101, path: 'A.java', layer: 'controller', role: null, summary: null }]
    const calls = capture(files)

    await expect(fetchAnalysisFiles(42)).resolves.toEqual(files)

    expect(`${calls[0].method} ${calls[0].url}`).toBe('get /analyses/42/files')
  })

  it('AC14 fetchAnalysisFile은 GET /api/analyses/{id}/files/{fileId}', async () => {
    const calls = capture({ id: 101, path: 'A.java', content: 'class A {}' })

    await expect(fetchAnalysisFile(42, 101)).resolves.toMatchObject({ content: 'class A {}' })

    expect(`${calls[0].method} ${calls[0].url}`).toBe('get /analyses/42/files/101')
  })
})

describe('api/analyses (F-07)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('AC14 fetchAnalysisFlows는 GET /api/analyses/{id}/flows', async () => {
    const flows = [{ id: 7, method: 'POST', path: '/api/todos', status: 'PENDING' }]
    const calls = capture(flows)

    await expect(fetchAnalysisFlows(42)).resolves.toEqual(flows)

    expect(`${calls[0].method} ${calls[0].url}`).toBe('get /analyses/42/flows')
  })

  it('AC14 requestAnalysisFlows는 POST /api/analyses/{id}/flows에 endpoints를 보낸다', async () => {
    const calls = capture([])
    const endpoints = [{ method: 'POST', path: '/api/todos' }]

    await expect(requestAnalysisFlows(42, endpoints)).resolves.toEqual([])

    expect(`${calls[0].method} ${calls[0].url}`).toBe('post /analyses/42/flows')
    expect(JSON.parse(calls[0].data)).toEqual({ endpoints })
  })
})

describe('api/analyses (F-08)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('AC8 fetchAnalysisConcepts는 GET /api/analyses/{id}/concepts', async () => {
    const body = { items: [], pendingCount: 1 }
    const calls = capture(body)

    await expect(fetchAnalysisConcepts(42)).resolves.toEqual(body)

    expect(`${calls[0].method} ${calls[0].url}`).toBe('get /analyses/42/concepts')
  })
})
