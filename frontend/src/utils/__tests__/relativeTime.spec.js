import { describe, expect, it } from 'vitest'
import { formatRelativeTime } from '@/utils/relativeTime'

// 서버 시각은 시간대 없는 서울 시각('2026-11-20T13:40:00')이고, 브라우저 시간대로 해석한다
const NOW = new Date('2026-11-20T15:00:00')

describe('formatRelativeTime (F-04 AC11)', () => {
  it('AC11 1분 미만은 "방금 전"', () => {
    expect(formatRelativeTime('2026-11-20T14:59:30', NOW)).toBe('방금 전')
    // 서버와 브라우저 시계가 조금 어긋나 미래 시각이어도 "방금 전"
    expect(formatRelativeTime('2026-11-20T15:00:20', NOW)).toBe('방금 전')
  })

  it('AC11 1시간 미만은 "n분 전", 24시간 미만은 "n시간 전"', () => {
    expect(formatRelativeTime('2026-11-20T14:55:00', NOW)).toBe('5분 전')
    expect(formatRelativeTime('2026-11-20T14:00:01', NOW)).toBe('59분 전')
    expect(formatRelativeTime('2026-11-20T14:00:00', NOW)).toBe('1시간 전')
    expect(formatRelativeTime('2026-11-19T15:00:01', NOW)).toBe('23시간 전')
  })

  it('AC11 24시간 이상 48시간 미만은 "어제", 7일까지는 "n일 전"', () => {
    expect(formatRelativeTime('2026-11-19T15:00:00', NOW)).toBe('어제')
    expect(formatRelativeTime('2026-11-18T15:00:01', NOW)).toBe('어제')
    expect(formatRelativeTime('2026-11-18T15:00:00', NOW)).toBe('2일 전')
    expect(formatRelativeTime('2026-11-13T15:00:00', NOW)).toBe('7일 전')
  })

  it('AC11 7일을 넘으면 YYYY-MM-DD', () => {
    expect(formatRelativeTime('2026-11-13T14:59:59', NOW)).toBe('2026-11-13')
    expect(formatRelativeTime('2025-01-02T09:00:00', NOW)).toBe('2025-01-02')
  })
})
