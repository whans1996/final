// 개념 카드 카테고리 12종 (K-1 AC5, 결정로그 11-27). BE에는 목록 API가 없어 FE 상수로 둔다 (F-09 2절).
// 값과 순서는 BE ConceptCategories.ORDERED와 같다 (conceptCategories.spec.js가 BE 파일을 읽어 확인한다).
export const CONCEPT_CATEGORIES = [
  { value: 'java', label: 'Java' },
  { value: 'spring', label: 'Spring' },
  { value: 'web', label: 'Web' },
  { value: 'security', label: 'Security' },
  { value: 'mybatis', label: 'MyBatis' },
  { value: 'vue', label: 'Vue' },
  { value: 'database', label: 'Database' },
  { value: 'javascript', label: 'JavaScript' },
  { value: 'tooling', label: 'Tooling' },
  { value: 'typescript', label: 'TypeScript' },
  { value: 'react', label: 'React' },
  { value: 'cs', label: 'CS' },
]

/** 카테고리 값의 화면 표시명. 모르는 값은 원문 그대로 (#204) */
export function categoryLabel(value) {
  return CONCEPT_CATEGORIES.find((c) => c.value === value)?.label ?? value
}
