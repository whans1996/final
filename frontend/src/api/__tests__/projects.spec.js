import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import http from '@/api/http'
import { createAnalysis } from '@/api/analyses'
import { fetchLatestCommit, fetchProject, fetchProjects, updateProject } from '@/api/projects'

// API 계약(docs/specs/P-1a-project-versions.md 3절)과 URL·메서드·필드 이름이 같은지 확인한다
function capture(data = {}) {
  const calls = []
  http.defaults.adapter = async (config) => {
    calls.push(config)
    return { data, status: 200, statusText: 'OK', headers: {}, config }
  }
  return calls
}

describe('api/projects (P-1a)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('AC14 fetchProjects는 GET /api/projects에 page·size를 보내고 signal을 넘긴다', async () => {
    const page = { items: [], page: 2, size: 20, total: 21 }
    const calls = capture(page)
    const controller = new AbortController()

    await expect(
      fetchProjects({ page: 2, size: 20 }, { signal: controller.signal }),
    ).resolves.toEqual(page)

    expect(calls[0].method).toBe('get')
    expect(calls[0].url).toBe('/projects')
    expect(calls[0].params).toEqual({ page: 2, size: 20 })
    expect(calls[0].signal).toBe(controller.signal)
  })

  it('AC15 fetchProject는 GET /api/projects/{id}', async () => {
    const calls = capture({ id: 12, name: 'todo-app', versions: [], analyses: [] })

    await expect(fetchProject(12)).resolves.toMatchObject({ id: 12 })

    expect(calls[0].method).toBe('get')
    expect(calls[0].url).toBe('/projects/12')
  })

  it('AC15 fetchLatestCommit은 GET /api/projects/{id}/latest-commit', async () => {
    const calls = capture({ status: 'UP_TO_DATE' })

    await expect(fetchLatestCommit(12)).resolves.toEqual({ status: 'UP_TO_DATE' })

    expect(calls[0].method).toBe('get')
    expect(calls[0].url).toBe('/projects/12/latest-commit')
  })

  it('AC15 updateProject는 PATCH /api/projects/{id}에 name을 보낸다', async () => {
    const calls = capture({ id: 12, name: '할 일 앱' })

    await expect(updateProject(12, { name: '할 일 앱' })).resolves.toEqual({
      id: 12,
      name: '할 일 앱',
    })

    expect(calls[0].method).toBe('patch')
    expect(calls[0].url).toBe('/projects/12')
    expect(JSON.parse(calls[0].data)).toEqual({ name: '할 일 앱' })
  })

  it('AC15 createAnalysis는 projectId가 있으면 projectId 파트를 보내고, 없으면 보내지 않는다', async () => {
    const calls = capture({ analysisId: 128, status: 'FETCHING', projectId: 12 })

    await createAnalysis({
      sourceUrl: 'https://github.com/octo/todo-app',
      consent: true,
      projectId: 12,
    })
    await createAnalysis({ sourceUrl: 'https://github.com/octo/todo-app', consent: true })

    expect(calls[0].data.get('projectId')).toBe('12')
    expect(calls[1].data.has('projectId')).toBe(false)
  })
})
