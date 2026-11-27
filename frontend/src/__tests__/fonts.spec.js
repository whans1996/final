import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

// 시연장처럼 네트워크가 없어도 폰트가 빌드 결과물에서 로드되어야 한다 (F-29a AC6)
const css = readFileSync(resolve('src/assets/main.css'), 'utf8') // vitest root = frontend
const html = readFileSync(resolve('index.html'), 'utf8')

describe('폰트 (F-29a)', () => {
  it('AC6 Pretendard Variable·JetBrains Mono를 npm 패키지에서 import한다', () => {
    expect(css).toMatch(/@import 'pretendard\/dist\/web\/variable\/[\w-]+\.css';/)
    expect(css).toContain("@import '@fontsource-variable/jetbrains-mono';")
    expect(css).toMatch(/--font-sans:\s*'Pretendard Variable'/)
    expect(css).toMatch(/--font-mono:\s*'JetBrains Mono Variable'/)
  })

  it('AC6 외부 CDN에서 폰트·스타일을 받지 않는다', () => {
    expect(css).not.toMatch(/@import\s+(url\()?['"]?(https?:)?\/\//)
    expect(css).not.toMatch(/url\(\s*['"]?(https?:)?\/\//)
    expect(html).not.toMatch(/<link[^>]+href=["'](https?:)?\/\//)
  })
})
