import { describe, expect, it } from 'vitest'
import { routes } from '@/router'

// F-00b AC7: 라우트를 도메인 파일(router/routes/*.js)로 나눠도 기존 경로·이름이 그대로다
describe('routes (F-00b AC7)', () => {
  const names = routes.flatMap((r) => [r.name, ...(r.children ?? []).map((c) => c.name)])

  it('AC7 기존 경로·이름이 그대로 있다', () => {
    const byName = Object.fromEntries(routes.filter((r) => r.name).map((r) => [r.name, r.path]))

    expect(byName).toMatchObject({
      home: '/',
      login: '/login',
      signup: '/signup',
      'user-profile': '/users/:id(\\d+)',
      'my-settings': '/me/settings',
      analyses: '/analyses',
      'analysis-new': '/analyses/new',
      'analysis-detail': '/analyses/:id(\\d+)',
      'not-found': '/:pathMatch(.*)*',
    })
  })

  it('AC7 라우트 이름이 겹치지 않는다', () => {
    const named = names.filter(Boolean)
    expect(new Set(named).size).toBe(named.length)
  })

  it('AC7 not-found가 마지막이다', () => {
    expect(routes.at(-1).name).toBe('not-found')
  })

  it('AC9 /admin은 관리자 전용이고 자식 route를 가진다', () => {
    const admin = routes.find((r) => r.path === '/admin')

    expect(admin.meta).toMatchObject({ requiresAuth: true, requiresAdmin: true })
    expect(admin.children.find((c) => c.path === '').name).toBe('admin-home')
  })
})
