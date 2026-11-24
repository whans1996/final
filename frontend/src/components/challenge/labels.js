// 챌린지 상태·목표 유형 표시 이름과 기간·목표 문구. 관리자(F-24)·회원(F-25) 화면이 함께 쓴다.

export const CHALLENGE_STATUSES = [
  { value: 'UPCOMING', label: '예정' },
  { value: 'ONGOING', label: '진행 중' },
  { value: 'ENDED', label: '종료' },
]

// icon은 이미지가 없는 챌린지의 기본 그림이다 (AC11)
export const GOAL_TYPES = [
  { value: 'REVIEW_COUNT', label: '개념 복습', icon: '📚' },
  { value: 'QUIZ_COUNT', label: '퀴즈', icon: '📝' },
  { value: 'ANALYSIS_COUNT', label: '분석', icon: '🔍' },
]

const find = (list, value) => list.find((item) => item.value === value)

export const statusLabel = (value) => find(CHALLENGE_STATUSES, value)?.label ?? value
export const goalTypeLabel = (value) => find(GOAL_TYPES, value)?.label ?? value
export const goalIcon = (value) => find(GOAL_TYPES, value)?.icon ?? '🏁'

/** "퀴즈 10회" */
export const goalLabel = (goalType, goalValue) =>
  `${goalTypeLabel(goalType)} ${Number(goalValue).toLocaleString('ko-KR')}회`

const dotted = (iso) => iso.replaceAll('-', '.')

/** "2026.12.01 ~ 12.31". 연도가 다르면 끝에도 연도를 붙인다 ("2026.12.15 ~ 2027.01.10") */
export function periodLabel(startDate, endDate) {
  const sameYear = startDate.slice(0, 4) === endDate.slice(0, 4)
  return `${dotted(startDate)} ~ ${sameYear ? dotted(endDate.slice(5)) : dotted(endDate)}`
}

/** 시작한 챌린지 수정 화면의 잠금 안내. 409 CHALLENGE_001 문구와 같다 (AC13) */
export const LOCK_MESSAGE = '이미 시작한 챌린지는 목표와 시작일을 바꿀 수 없습니다.'
