// 퀴즈 화면의 시각·날짜 표시 (F-14 AC9·AC10). 서버는 Asia/Seoul(+09:00) 시각을 보내므로 문자열에서 바로 꺼낸다.

/** '2026-11-19T17:30:12+09:00' → '11/19 17:30' */
export function attemptTime(value) {
  if (!value) return ''
  return `${Number(value.slice(5, 7))}/${Number(value.slice(8, 10))} ${value.slice(11, 16)}`
}

/** '2026-11-20' → '11월 20일' */
export function planDateLabel(value) {
  if (!value) return ''
  return `${Number(value.slice(5, 7))}월 ${Number(value.slice(8, 10))}일`
}
