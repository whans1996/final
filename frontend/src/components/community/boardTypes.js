// 게시판 이름·칩 라벨과 글 표시 규칙 (F-17 5절). 화면들은 여기서만 가져다 쓴다.

export const BOARD_TYPES = [
  { value: 'QNA', label: '코드 이해 Q&A', chip: 'Q&A' },
  { value: 'REVIEW', label: '학습 후기', chip: '후기' },
  { value: 'FREE', label: '자유', chip: '자유' },
]

export const DEFAULT_BOARD_TYPE = 'QNA'

export const WITHDRAWN_NICKNAME = '탈퇴한 사용자'

export const ATTACHMENT_ICONS = { ANALYSIS: '📎', CONCEPT: '🃏' }

export function boardChip(value) {
  return BOARD_TYPES.find((b) => b.value === value)?.chip ?? value
}

export function isBoardType(value) {
  return BOARD_TYPES.some((b) => b.value === value)
}

/** 탈퇴한 작성자(authorId null)는 닉네임 대신 "탈퇴한 사용자" */
export function authorName(post) {
  return post.authorId === null ? WITHDRAWN_NICKNAME : post.authorNickname
}

// 서버는 항상 서울 시각(+09:00) ISO 문자열을 보낸다. 브라우저 시간대와 상관없이 그 글자를 그대로 쓴다

/** '2026-11-20T14:02:11+09:00' → '11/20' */
export function formatShortDate(iso) {
  return `${iso.slice(5, 7)}/${iso.slice(8, 10)}`
}

/** '2026-11-20T14:02:11+09:00' → '2026-11-20 14:02' */
export function formatDateTime(iso) {
  return `${iso.slice(0, 10)} ${iso.slice(11, 16)}`
}

/** 첨부 칩에 보일 분석 이름: 제목 → 프로젝트명 → "분석 #id" */
export function analysisLabel(analysis) {
  return analysis.title || analysis.projectName || `분석 #${analysis.id}`
}
