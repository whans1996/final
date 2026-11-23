// 관리자 개념 카드 화면(F-13)의 상태·출처 표시 이름. 목록·편집 화면이 함께 쓴다.
import { LEVELS } from '@/utils/authValidation'
import { CONCEPT_CATEGORIES } from '@/utils/conceptCategories'

export const CONCEPT_STATUSES = [
  { value: 'DRAFT', label: '검수 대기' },
  { value: 'PUBLISHED', label: '공개' },
  { value: 'HIDDEN', label: '숨김' },
]

export const CONCEPT_SOURCES = [
  { value: 'SEED', label: '시드' },
  { value: 'AI', label: 'AI 초안' },
  { value: 'ADMIN', label: '직접 작성' },
]

const labelOf = (list) => (value) => list.find((item) => item.value === value)?.label ?? value

export const statusLabel = labelOf(CONCEPT_STATUSES)
export const sourceLabel = labelOf(CONCEPT_SOURCES)
export const categoryLabel = labelOf(CONCEPT_CATEGORIES)
export const difficultyLabel = labelOf(LEVELS)
