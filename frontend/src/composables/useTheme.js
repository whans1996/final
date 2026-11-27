// 테마(라이트·다크·시스템) 상태 (F-29a AC4·AC5). 앱 전체가 한 상태를 같이 쓴다.
// 첫 칠하기 전 클래스는 index.html 인라인 스크립트가 같은 키·규칙으로 먼저 붙인다 (깜빡임 방지).
import { computed, ref, watch } from 'vue'

export const THEME_KEY = 'vibenote.theme'
export const THEMES = ['light', 'dark', 'system']

function readStored() {
  try {
    const value = localStorage.getItem(THEME_KEY)
    return THEMES.includes(value) ? value : 'system'
  } catch {
    return 'system' // 저장소가 막힌 브라우저
  }
}

const theme = ref(readStored())
const systemDark = ref(false)
const resolved = computed(() =>
  theme.value === 'system' ? (systemDark.value ? 'dark' : 'light') : theme.value,
)

let started = false

/** OS 설정 구독과 <html> 클래스 반영을 시작한다. 여러 번 불러도 한 번만 건다 (main.js가 부른다) */
export function initTheme() {
  if (started) return
  started = true
  const media =
    typeof window.matchMedia === 'function'
      ? window.matchMedia('(prefers-color-scheme: dark)')
      : null
  systemDark.value = Boolean(media?.matches)
  media?.addEventListener('change', (event) => {
    systemDark.value = event.matches
  })
  watch(resolved, (value) => document.documentElement.classList.toggle('dark', value === 'dark'), {
    immediate: true,
    flush: 'sync',
  })
}

function setTheme(value) {
  if (!THEMES.includes(value)) return
  theme.value = value
  try {
    localStorage.setItem(THEME_KEY, value)
  } catch {
    // 저장하지 못해도 이번 방문에서는 반영한다
  }
}

export function useTheme() {
  initTheme()
  return { theme, resolved, setTheme }
}
