// 피드 시각 표시 (F-04 AC11). 서버 시각은 서울 오프셋이 붙은 ISO-8601('+09:00')이라 브라우저 시간대와 상관없이 같은 순간으로 해석한다.

const MINUTE = 60 * 1000
const HOUR = 60 * MINUTE
const DAY = 24 * HOUR

/**
 * "방금 전"·"n분 전"·"n시간 전"·"어제"(24~48시간)·"n일 전"(7일까지), 그보다 오래되면 'YYYY-MM-DD'.
 * @param {string} iso '2026-11-20T13:40:00+09:00'
 * @param {Date} [now] 테스트에서 고정한다
 */
export function formatRelativeTime(iso, now = new Date()) {
  const diff = now.getTime() - new Date(iso).getTime()
  if (diff < MINUTE) return '방금 전'
  if (diff < HOUR) return `${Math.floor(diff / MINUTE)}분 전`
  if (diff < DAY) return `${Math.floor(diff / HOUR)}시간 전`
  if (diff < 2 * DAY) return '어제'
  if (diff <= 7 * DAY) return `${Math.floor(diff / DAY)}일 전`
  return iso.slice(0, 10) // 서버(서울) 날짜
}
