import { describe, expect, it } from 'vitest'
import {
  categoryLabel,
  formatLines,
  groupByCategory,
  lineLabel,
  parseLines,
} from '@/utils/interviewFormat'

describe('utils/interviewFormat (I-1a)', () => {
  it('AC13 범주 라벨', () => {
    expect(categoryLabel('STRUCTURE')).toBe('구조')
    expect(categoryLabel('TECH_CHOICE')).toBe('기술 선택 이유')
    expect(categoryLabel('FLOW')).toBe('핵심 흐름')
    expect(categoryLabel('LIMITATION')).toBe('한계와 개선')
  })

  it('AC13 구조 → 기술 선택 이유 → 핵심 흐름 → 한계와 개선 순으로 묶고 빈 범주는 뺀다', () => {
    const questions = [
      { no: 1, category: 'FLOW' },
      { no: 2, category: 'STRUCTURE' },
      { no: 3, category: 'LIMITATION' },
      { no: 4, category: 'FLOW' },
    ]

    const groups = groupByCategory(questions)

    expect(groups.map((g) => g.category)).toEqual(['STRUCTURE', 'FLOW', 'LIMITATION'])
    expect(groups[1].label).toBe('핵심 흐름')
    expect(groups[1].items.map((q) => q.no)).toEqual([1, 4])
  })

  it('AC15 parseLines는 범위와 한 줄을 읽고 틀린 형식·0·역순은 null이다', () => {
    expect(parseLines('12-30')).toEqual({ start: 12, end: 30 })
    expect(parseLines('7')).toEqual({ start: 7, end: 7 })
    for (const bad of ['abc', '30-12', '0', '0-3', '', '1-', '-3', '1.5', undefined, null]) {
      expect(parseLines(bad)).toBeNull()
    }
  })

  it('AC13 근거 줄 범위를 주소 값과 칩 표시로 바꾼다', () => {
    expect(formatLines({ startLine: 12, endLine: 30 })).toBe('12-30')
    expect(formatLines({ startLine: 7, endLine: 7 })).toBe('7')
    expect(lineLabel({ startLine: 12, endLine: 30 })).toBe('L12-L30')
    expect(lineLabel({ startLine: 7, endLine: 7 })).toBe('L7')
  })
})
