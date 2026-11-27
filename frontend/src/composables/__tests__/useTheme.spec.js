import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { nextTick } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

// OS 다크 설정(matchMedia)을 흉내 낸다. setDark()로 OS 설정이 바뀐 것처럼 change를 보낸다.
function mockMatchMedia(initialDark) {
  const listeners = new Set()
  const mql = {
    matches: initialDark,
    media: '(prefers-color-scheme: dark)',
    addEventListener: (_, fn) => listeners.add(fn),
    removeEventListener: (_, fn) => listeners.delete(fn),
  }
  window.matchMedia = vi.fn(() => mql)
  return {
    setDark(value) {
      mql.matches = value
      listeners.forEach((fn) => fn({ matches: value }))
    },
  }
}

/** 모듈 상태(싱글턴)를 매번 새로 받는다 */
async function loadTheme() {
  vi.resetModules()
  return import('@/composables/useTheme')
}

const isDark = () => document.documentElement.classList.contains('dark')

beforeEach(() => {
  localStorage.clear()
  document.documentElement.className = ''
})

afterEach(() => {
  delete window.matchMedia
})

describe('useTheme (F-29a)', () => {
  it('AC4 저장된 테마가 없고 OS가 다크면 html에 dark가 붙는다', async () => {
    mockMatchMedia(true)
    const { initTheme, useTheme } = await loadTheme()
    initTheme()
    expect(useTheme().theme.value).toBe('system')
    expect(useTheme().resolved.value).toBe('dark')
    expect(isDark()).toBe(true)
  })

  it('AC4 OS가 라이트면 dark가 붙지 않는다', async () => {
    mockMatchMedia(false)
    const { initTheme } = await loadTheme()
    initTheme()
    expect(isDark()).toBe(false)
  })

  it('AC4 system일 때 OS 설정이 바뀌면 새로고침 없이 따라간다', async () => {
    const os = mockMatchMedia(false)
    const { initTheme } = await loadTheme()
    initTheme()
    os.setDark(true)
    await nextTick()
    expect(isDark()).toBe(true)
    os.setDark(false)
    await nextTick()
    expect(isDark()).toBe(false)
  })

  it('AC5 setTheme은 즉시 반영하고 vibenote.theme에 저장한다', async () => {
    const os = mockMatchMedia(false)
    const { useTheme } = await loadTheme()
    const { theme, setTheme, resolved } = useTheme()
    setTheme('dark')
    await nextTick()
    expect(theme.value).toBe('dark')
    expect(resolved.value).toBe('dark')
    expect(isDark()).toBe(true)
    expect(localStorage.getItem('vibenote.theme')).toBe('dark')

    // 직접 고른 테마는 OS 설정 변화를 따라가지 않는다
    os.setDark(false)
    await nextTick()
    expect(isDark()).toBe(true)

    setTheme('light')
    await nextTick()
    expect(isDark()).toBe(false)
    expect(localStorage.getItem('vibenote.theme')).toBe('light')
  })

  it('AC5 새로 불러와도(새로고침) 저장한 테마가 유지된다', async () => {
    mockMatchMedia(false)
    localStorage.setItem('vibenote.theme', 'dark')
    const { initTheme, useTheme } = await loadTheme()
    initTheme()
    expect(useTheme().theme.value).toBe('dark')
    expect(isDark()).toBe(true)
  })

  it('AC5 저장값이 세 값이 아니면 system으로 보고, 잘못된 setTheme 값은 무시한다', async () => {
    mockMatchMedia(true)
    localStorage.setItem('vibenote.theme', 'purple')
    const { initTheme, useTheme } = await loadTheme()
    initTheme()
    const { theme, setTheme } = useTheme()
    expect(theme.value).toBe('system')
    expect(isDark()).toBe(true)
    setTheme('sepia')
    expect(theme.value).toBe('system')
  })

  it('AC5 matchMedia·localStorage가 없거나 막혀도 라이트로 동작한다', async () => {
    const getItem = vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('blocked')
    })
    const setItem = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('blocked')
    })
    const { initTheme, useTheme } = await loadTheme()
    initTheme()
    expect(isDark()).toBe(false)
    useTheme().setTheme('dark')
    await nextTick()
    expect(isDark()).toBe(true)
    getItem.mockRestore()
    setItem.mockRestore()
  })
})

describe('index.html 테마 스크립트 (F-29a AC5 깜빡임 방지)', () => {
  const html = readFileSync(resolve('index.html'), 'utf8') // vitest root = frontend
  const head = html.slice(0, html.indexOf('</head>'))
  const inline = head.match(/<script>([\s\S]*?)<\/script>/)

  function runInline() {
    new Function(inline[1])()
  }

  it('AC5 head에 앱보다 먼저 도는 인라인 스크립트가 10줄 이내로 있다', () => {
    expect(inline).not.toBeNull()
    expect(inline[1].trim().split('\n').length).toBeLessThanOrEqual(10)
  })

  it('AC5 저장값 dark면 dark를 붙이고 light면 붙이지 않는다', () => {
    mockMatchMedia(true)
    localStorage.setItem('vibenote.theme', 'dark')
    runInline()
    expect(isDark()).toBe(true)
    document.documentElement.className = ''
    localStorage.setItem('vibenote.theme', 'light')
    runInline()
    expect(isDark()).toBe(false)
  })

  it('AC5 저장값이 없거나 잘못되면 OS 설정을 따른다', () => {
    mockMatchMedia(true)
    localStorage.setItem('vibenote.theme', 'purple')
    runInline()
    expect(isDark()).toBe(true)
    document.documentElement.className = ''
    mockMatchMedia(false)
    localStorage.clear()
    runInline()
    expect(isDark()).toBe(false)
  })
})
