import { describe, expect, it } from 'vitest'
import { overflowLastPage } from '@/utils/pagination'

describe('overflowLastPage (#46·#76)', () => {
  it('빈 목록인데 total이 있고 page가 마지막 page를 넘으면 마지막 page를 준다', () => {
    expect(overflowLastPage({ page: 99, size: 20, total: 41, itemCount: 0 })).toBe(3)
    expect(overflowLastPage({ page: 3, size: 20, total: 40, itemCount: 0 })).toBe(2)
  })

  it('결과가 0건이면 null이다', () => {
    expect(overflowLastPage({ page: 5, size: 20, total: 0, itemCount: 0 })).toBeNull()
  })

  it('목록에 항목이 있으면 null이다', () => {
    expect(overflowLastPage({ page: 2, size: 20, total: 40, itemCount: 20 })).toBeNull()
  })

  it('page가 범위 안인데 빈 목록이면(조회 사이 삭제 경합) null이다', () => {
    expect(overflowLastPage({ page: 2, size: 20, total: 40, itemCount: 0 })).toBeNull()
  })
})
