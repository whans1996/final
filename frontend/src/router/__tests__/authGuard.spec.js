import { describe, expect, it } from 'vitest'
import { authGuard } from '@/router'

const loggedOut = { isLoggedIn: false, isAdmin: false }
const user = { isLoggedIn: true, isAdmin: false }
const admin = { isLoggedIn: true, isAdmin: true }

const route = (meta, fullPath = '/somewhere') => ({ meta, fullPath })

describe('authGuard', () => {
  it('공개 화면은 누구나 들어간다', () => {
    expect(authGuard(route({}), loggedOut)).toBe(true)
  })

  it('F-02 AC9 requiresAuth 화면에 비로그인으로 들어가면 로그인 화면으로 원래 주소를 넘긴다', () => {
    expect(authGuard(route({ requiresAuth: true }, '/review-plans'), loggedOut)).toEqual({
      name: 'login',
      query: { redirect: '/review-plans' },
    })
  })

  it('requiresAuth 화면은 로그인하면 들어간다', () => {
    expect(authGuard(route({ requiresAuth: true }), user)).toBe(true)
  })

  it('requiresAdmin 화면은 관리자만 들어간다', () => {
    const meta = { requiresAuth: true, requiresAdmin: true }
    expect(authGuard(route(meta), user)).toEqual({ name: 'home' })
    expect(authGuard(route(meta), admin)).toBe(true)
  })

  it('F-02 AC8 guestOnly 화면(로그인·회원가입)은 로그인 상태면 홈으로 보낸다', () => {
    expect(authGuard(route({ guestOnly: true }), user)).toEqual({ name: 'home' })
    expect(authGuard(route({ guestOnly: true }), loggedOut)).toBe(true)
  })
})
