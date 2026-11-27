import { describe, expect, it } from 'vitest'
import { Circle, LayoutDashboard } from 'lucide-vue-next'
import { routes } from '@/router'
import { navIcon } from '@/utils/navIcons'

describe('navIcon (F-29b 5.1)', () => {
  it('AC1 아는 이름은 Lucide 컴포넌트로, 모르는 이름·빈 값은 Circle로 그린다', () => {
    expect(navIcon('LayoutDashboard')).toBe(LayoutDashboard)
    expect(navIcon('Nope')).toBe(Circle)
    expect(navIcon(undefined)).toBe(Circle)
  })

  it('AC1 route에 단 icon 이름은 모두 맵에 있다 (오타면 Circle로 조용히 바뀌지 않게)', () => {
    const names = routes.map((r) => r.meta?.nav?.icon).filter(Boolean)

    expect(names.length).toBeGreaterThan(0)
    expect(names.filter((name) => navIcon(name) === Circle)).toEqual([])
  })
})
