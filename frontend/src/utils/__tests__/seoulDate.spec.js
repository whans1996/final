import { describe, expect, it } from 'vitest'
import { daysBetween, dDayLabel, todayInSeoul } from '@/utils/seoulDate'

describe('seoulDate (F-25 AC16, #99)', () => {
  it('AC16 브라우저 시간이 UTC 2026-11-24 15:00:01이면 서울 오늘은 2026-11-25다', () => {
    expect(todayInSeoul(new Date('2026-11-24T15:00:01Z'))).toBe('2026-11-25')
  })

  it('AC16 UTC 14:59:59까지는 서울 오늘이 그날이다', () => {
    expect(todayInSeoul(new Date('2026-11-24T14:59:59Z'))).toBe('2026-11-24')
  })

  it('AC16 daysBetween은 두 날짜 사이 일수(b - a)다. 월·연도를 넘어도 맞다', () => {
    expect(daysBetween('2026-11-24', '2026-11-24')).toBe(0)
    expect(daysBetween('2026-11-24', '2026-11-30')).toBe(6)
    expect(daysBetween('2026-11-24', '2027-01-01')).toBe(38)
    expect(daysBetween('2026-11-30', '2026-11-24')).toBe(-6)
  })

  it('AC13 D-n은 남은 일수, 0이면 D-day다', () => {
    expect(dDayLabel('2026-11-24', '2026-11-30')).toBe('D-6')
    expect(dDayLabel('2026-11-24', '2026-11-24')).toBe('D-day')
  })
})
