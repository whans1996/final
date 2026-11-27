import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

// F-29a 5.2절 토큰 표가 main.css에 그대로 들어 있는지, 기존 변수가 별칭으로 남았는지 본다 (AC2·AC13)
const css = readFileSync(resolve('src/assets/main.css'), 'utf8') // vitest root = frontend

/** `selector { ... }` 블록 하나의 `--name: value` 목록 */
function varsIn(selector) {
  const start = css.indexOf(`${selector} {`)
  expect(start, `${selector} 블록이 없다`).toBeGreaterThanOrEqual(0)
  const body = css.slice(start, css.indexOf('\n}', start))
  return Object.fromEntries(
    [...body.matchAll(/(--[\w*-]+):\s*([^;]+);/g)].map(([, k, v]) => [k, v.trim().toLowerCase()]),
  )
}

const light = varsIn(':root')
const dark = varsIn('.dark')
const theme = varsIn('@theme inline')

// 토큰 이름 → [라이트, 다크] (spec 5.2절 표)
const PALETTE = {
  primary: ['#0f766e', '#2dd4bf'],
  'primary-soft': ['#e6f4f2', '#123a37'],
  accent: ['#f97362', '#fb8b7c'],
  highlight: ['#fde68a', 'rgb(161 98 7 / 0.4)'],
  bg: ['#faf8f5', '#1c1a18'],
  surface: ['#ffffff', '#262321'],
  'surface-raised': ['#ffffff', '#302c29'],
  text: ['#1f1d1b', '#f2eee9'],
  'text-muted': ['#6b645d', '#a69e95'],
  border: ['#e7e2dc', '#3a3532'],
  'code-bg': ['#f6f4f0', '#141211'],
  'code-text': ['#1f1d1b', '#e8e3dc'],
  'on-solid': ['#ffffff', '#1c1a18'],
  success: ['#3a7535', '#8fca86'],
  'success-soft': ['#e8f1e3', '#1f3320'],
  warning: ['#a15c07', '#f0b45a'],
  'warning-soft': ['#fbefd9', '#3a2a10'],
  info: ['#33658a', '#8db8d9'],
  'info-soft': ['#e3edf4', '#17293a'],
  danger: ['#b4232c', '#f2867f'],
  'danger-soft': ['#fbe4e2', '#3d1a1a'],
}

function hexLuminance(hex) {
  const [r, g, b] = [1, 3, 5].map((i) => {
    const v = parseInt(hex.slice(i, i + 2), 16) / 255
    return v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4
  })
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

function contrast(a, b) {
  const [hi, lo] = [hexLuminance(a), hexLuminance(b)].sort((x, y) => y - x)
  return (hi + 0.05) / (lo + 0.05)
}

describe('디자인 토큰 (F-29a)', () => {
  it('AC2 라이트·다크 팔레트가 spec 5.2절 값과 같다', () => {
    for (const [name, [l, d]] of Object.entries(PALETTE)) {
      expect(light[`--vn-${name}`], `라이트 --vn-${name}`).toBe(l)
      expect(dark[`--vn-${name}`], `다크 --vn-${name}`).toBe(d)
    }
  })

  it('AC2 @theme inline이 팔레트를 Tailwind 색 이름(bg-primary, text-danger…)으로 연결한다', () => {
    for (const name of Object.keys(PALETTE)) {
      expect(theme[`--color-${name}`], `--color-${name}`).toBe(`var(--vn-${name})`)
    }
  })

  it('AC2 글자·반경·그림자·반응형 토큰이 spec 값이다', () => {
    expect(theme['--text-xs']).toBe('0.75rem')
    expect(theme['--text-sm']).toBe('0.875rem')
    expect(theme['--text-base']).toBe('1rem')
    expect(theme['--text-lg']).toBe('1.25rem')
    expect(theme['--text-xl']).toBe('1.75rem')
    expect(theme['--text-hero']).toBe('var(--vn-text-hero)')
    expect(light['--vn-text-hero']).toBe('2rem')
    expect(css).toMatch(/@media \(width >= 40rem\) \{\s*:root \{\s*--vn-text-hero: 3rem;/)
    expect(theme['--radius-sm']).toBe('6px')
    expect(theme['--radius-md']).toBe('12px')
    expect(theme['--shadow-1']).toBe('var(--vn-shadow-1)')
    expect(theme['--shadow-2']).toBe('var(--vn-shadow-2)')
    expect(theme['--breakpoint-*']).toBe('initial')
    expect(theme['--breakpoint-sm']).toBe('40rem')
    expect(theme['--breakpoint-lg']).toBe('64rem')
    expect(theme['--font-weight-normal']).toBe('400')
    expect(theme['--font-weight-semibold']).toBe('600')
    expect(theme['--font-weight-bold']).toBe('700')
  })

  it('AC2 기존 변수는 새 토큰을 가리키는 별칭으로 남는다', () => {
    const aliases = varsIn(':root,\n.dark')
    expect(aliases['--color-primary']).toBe('var(--vn-primary)')
    expect(aliases['--color-text']).toBe('var(--vn-text)')
    expect(aliases['--color-text-muted']).toBe('var(--vn-text-muted)')
    expect(aliases['--color-border']).toBe('var(--vn-border)')
    expect(aliases['--color-surface']).toBe('var(--vn-surface)')
    expect(aliases['--color-background']).toBe('var(--vn-bg)')
    expect(aliases['--color-danger']).toBe('var(--vn-danger)')
    expect(aliases['--color-warning']).toBe('var(--vn-warning)')
    expect(aliases['--radius']).toBe('6px')
    for (const n of [1, 2, 3, 4, 6, 8]) expect(aliases[`--space-${n}`]).toBe(`${n * 4}px`)
  })

  it('AC3 preflight 보정이 @layer base에 있다 (제목·링크·목록·클래스 없는 버튼)', () => {
    const base = css.slice(css.indexOf('@layer base {'))
    for (const selector of [
      'h1 {',
      'h2 {',
      'h3 {',
      'a {',
      'ul {',
      'ol {',
      'button:not([class]) {',
    ]) {
      expect(base).toContain(selector)
    }
    expect(base).toMatch(
      /button:not\(\[class\]\) \{[^}]*border: revert;[^}]*background-color: revert;/,
    )
  })

  it('AC3 h4~h6도 굵기를 되돌린다 (개념 카드 본문 소제목)', () => {
    const base = css.slice(css.indexOf('@layer base {'))
    expect(base).toMatch(/h4,\s*h5,\s*h6 \{[^}]*font-weight: 700;/)
  })

  it('AC2 다크 전환은 html.dark 클래스로 한다', () => {
    expect(css).toContain('@custom-variant dark (&:where(.dark, .dark *));')
  })

  it('AC13 글자/바탕 쌍이 라이트·다크 모두 WCAG AA 4.5:1을 넘는다', () => {
    const pairs = [
      ['text', 'bg'],
      ['text', 'surface'],
      ['text-muted', 'bg'],
      ['text-muted', 'surface'],
      ['text-muted', 'surface-raised'],
      ['text', 'highlight'],
      ['primary', 'bg'],
      ['primary', 'surface'],
      ['primary', 'primary-soft'],
      ['on-solid', 'primary'],
      ['on-solid', 'danger'],
      ['code-text', 'code-bg'],
      ['success', 'success-soft'],
      ['warning', 'warning-soft'],
      ['info', 'info-soft'],
      ['danger', 'danger-soft'],
      ['danger', 'surface'],
    ]
    for (const [mode, vars] of [
      ['라이트', light],
      ['다크', dark],
    ]) {
      for (const [fg, bg] of pairs) {
        const a = vars[`--vn-${fg}`]
        const b = vars[`--vn-${bg}`]
        if (!a.startsWith('#') || !b.startsWith('#')) continue // 투명 형광펜(다크)은 화면에서 본다
        expect(contrast(a, b), `${mode} ${fg}/${bg}`).toBeGreaterThanOrEqual(4.5)
      }
    }
  })
})
