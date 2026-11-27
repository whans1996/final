// 프로젝트 화면(SC-25·SC-26)의 표시 라벨 (P-1a 5절)
import { LEVELS } from '@/utils/authValidation'
import { STATUS_LABELS, isInProgress } from '@/utils/analysisStatus'

export const SOURCE_LABELS = { URL: '저장소', ZIP: 'ZIP' }

export function levelLabel(level) {
  return LEVELS.find((l) => l.value === level)?.label ?? level
}

export function statusLabel(status) {
  return STATUS_LABELS[status] ?? status
}

/** BaseBadge tone: 완료 success, 실패 danger, 진행 중 primary */
export function statusTone(status) {
  if (status === 'DONE') return 'success'
  if (status === 'FAILED') return 'danger'
  return isInProgress(status) ? 'primary' : 'neutral'
}

/** 40자 SHA → 앞 7자 */
export function shortSha(sha) {
  return typeof sha === 'string' ? sha.slice(0, 7) : ''
}

/** 서버 주소는 https만 저장하지만 href에 넣으므로 한 번 더 거른다 (F-05b AC14와 같은 규칙) */
export function safeSourceLink(url) {
  return typeof url === 'string' && url.startsWith('https://') ? url : null
}
