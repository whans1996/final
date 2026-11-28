import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import { CONCEPT_CATEGORIES, categoryLabel } from '@/utils/conceptCategories'

// vitest root = frontend
const java = readFileSync(
  resolve('../backend/src/main/java/com/vibenote/analysis/concept/ConceptCategories.java'),
  'utf8',
)

describe('conceptCategories (K-1)', () => {
  it('AC5 값과 순서가 BE ConceptCategories.ORDERED 12종과 같다', () => {
    const ordered = /ORDERED\s*=\s*List\.of\(([^)]*)\)/.exec(java)[1]
    const be = [...ordered.matchAll(/"([^"]+)"/g)].map((m) => m[1])

    expect(CONCEPT_CATEGORIES.map((c) => c.value)).toEqual(be)
    expect(be).toHaveLength(12)
  })

  it('AC12 categoryLabel은 표시명을 주고 모르는 값은 원문 그대로다', () => {
    expect(categoryLabel('mybatis')).toBe('MyBatis')
    expect(categoryLabel('cs')).toBe('CS')
    expect(categoryLabel('javascript')).toBe('JavaScript')
    expect(categoryLabel('typescript')).toBe('TypeScript')
    expect(categoryLabel('kotlin')).toBe('kotlin')
    expect(categoryLabel(null)).toBe(null)
  })
})
