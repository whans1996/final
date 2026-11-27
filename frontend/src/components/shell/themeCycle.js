// 헤더 테마 토글 순서 (F-29b AC10, 11-27 결정). FOLLOW_SYSTEM이 false인 동안 system은 라이트로 보이므로
// 토글은 라이트 ↔ 다크 2단계이고, 저장된 system은 라이트로 표시한다. FOLLOW_SYSTEM을 켜면 라이트 → 다크 → OS 3단계.
export const THEME_LABELS = { light: '라이트', dark: '다크', system: 'OS 설정' }

export function themeCycle(followSystem) {
  return followSystem ? ['light', 'dark', 'system'] : ['light', 'dark']
}

/** 토글에 보일 지금 값 */
export function shownTheme(theme, followSystem) {
  return themeCycle(followSystem).includes(theme) ? theme : 'light'
}

/** 한 번 눌렀을 때 바뀔 값 */
export function nextTheme(theme, followSystem) {
  const cycle = themeCycle(followSystem)
  return cycle[(cycle.indexOf(shownTheme(theme, followSystem)) + 1) % cycle.length]
}
