// 서울 날짜 계산 (F-25 AC16, #99). 챌린지 기간·상태는 서버가 Asia/Seoul 날짜로 정하므로,
// 브라우저 시간대와 상관없이 '오늘'을 서울 기준으로 맞춘다.

// en-CA는 날짜를 'yyyy-MM-dd'로 쓴다
const SEOUL_DATE = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'Asia/Seoul',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})

/** 서울 오늘 'yyyy-MM-dd'. */
export function todayInSeoul(now = new Date()) {
  return SEOUL_DATE.format(now)
}

const toDay = (iso) =>
  Date.UTC(+iso.slice(0, 4), +iso.slice(5, 7) - 1, +iso.slice(8, 10)) / 86400000

/** 두 'yyyy-MM-dd' 사이 일수 (b - a). */
export function daysBetween(a, b) {
  return toDay(b) - toDay(a)
}

/** today부터 target까지 "D-6", 같은 날이면 "D-day". */
export function dDayLabel(today, target) {
  const days = daysBetween(today, target)
  return days === 0 ? 'D-day' : `D-${days}`
}
