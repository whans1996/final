// 면접 질문 화면 표시용 (I-1a AC13·AC15).

/** 화면에 묶어 보이는 범주 순서. */
export const CATEGORY_ORDER = ['STRUCTURE', 'TECH_CHOICE', 'FLOW', 'LIMITATION']

const CATEGORY_LABELS = {
  STRUCTURE: '구조',
  TECH_CHOICE: '기술 선택 이유',
  FLOW: '핵심 흐름',
  LIMITATION: '한계와 개선',
}

export function categoryLabel(category) {
  return CATEGORY_LABELS[category] ?? category
}

/**
 * 질문을 범주 순서로 묶는다. 질문이 없는 범주는 뺀다. 묶음 안은 받은 순서(no 오름차순) 그대로.
 * @returns {{ category: string, label: string, items: object[] }[]}
 */
export function groupByCategory(questions) {
  return CATEGORY_ORDER.map((category) => ({
    category,
    label: categoryLabel(category),
    items: questions.filter((q) => q.category === category),
  })).filter((group) => group.items.length > 0)
}

/**
 * 주소의 줄 범위(`12-30`·`7`)를 읽는다. 형식이 틀리거나 0·역순이면 null.
 * @returns {{ start: number, end: number } | null}
 */
export function parseLines(value) {
  if (typeof value !== 'string') return null
  const match = /^(\d{1,9})(?:-(\d{1,9}))?$/.exec(value)
  if (!match) return null
  const start = Number(match[1])
  const end = match[2] === undefined ? start : Number(match[2])
  return start >= 1 && start <= end ? { start, end } : null
}

/** 근거의 줄 범위를 주소 값으로 (`12-30`, 한 줄이면 `7`). */
export function formatLines({ startLine, endLine }) {
  return startLine === endLine ? String(startLine) : `${startLine}-${endLine}`
}

/** 근거 칩 뒤에 붙이는 줄 표시 (`L12-L30`, 한 줄이면 `L7`). */
export function lineLabel({ startLine, endLine }) {
  return startLine === endLine ? `L${startLine}` : `L${startLine}-L${endLine}`
}
