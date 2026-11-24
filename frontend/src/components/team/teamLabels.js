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

// ── 팀 목표 (F-20c) ──

/** 브라우저 날짜(Asia/Seoul 사용자 전제)의 YYYY-MM-DD */
export function todayString(now = new Date()) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}

/** YYYY-MM-DD에 n일을 더한 YYYY-MM-DD */
export function addDays(date, n) {
  const [y, m, d] = date.split('-').map(Number)
  return todayString(new Date(y, m - 1, d + n))
}

/** 두 YYYY-MM-DD 사이 일수 (to - from) */
function daysBetween(from, to) {
  const toUtc = (s) => {
    const [y, m, d] = s.split('-').map(Number)
    return Date.UTC(y, m - 1, d)
  }
  return Math.round((toUtc(to) - toUtc(from)) / 86_400_000)
}

/** YYYY-MM-DD → MM-DD */
export function shortDate(date) {
  return date.slice(5, 10)
}

/**
 * 상태 칩 문구. 상태는 서버 `status`를 쓰고 남은 날만 브라우저 날짜로 센다.
 * 예정 "D-{n}" / 진행 중 "{n}일 남음"(종료일이면 "오늘 마감") / 종료 "종료".
 */
export function challengeStatusLabel(challenge, today = todayString()) {
  if (challenge.status === 'UPCOMING') {
    return `D-${Math.max(daysBetween(today, challenge.startDate), 1)}`
  }
  if (challenge.status === 'ONGOING') {
    const left = daysBetween(today, challenge.endDate)
    return left <= 0 ? '오늘 마감' : `${left}일 남음`
  }
  return '종료'
}

/** 정답률(%) 정수. 반올림은 표시용이고 달성 판정은 서버가 한다. */
export function attemptRate(attempt) {
  return attempt.total > 0 ? Math.round((attempt.score * 100) / attempt.total) : 0
}
