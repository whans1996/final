// 팀 화면 공용 표시 문구 (F-20a)
import { LEVELS } from '@/utils/authValidation'

export function roleLabel(role) {
  return role === 'OWNER' ? '팀장' : '팀원'
}

export function levelLabel(level) {
  return LEVELS.find((l) => l.value === level)?.label ?? ''
}

/** ISO-8601 시각에서 날짜 YYYY-MM-DD만 */
export function formatDate(iso) {
  return iso.slice(0, 10)
}
