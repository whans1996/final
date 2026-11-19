import { describe, expect, it } from 'vitest'
import { renderMarkdown } from '@/components/concept/markdown'

const text = (t) => ({ type: 'text', text: t })
const code = (t) => ({ type: 'code', text: t })

// F-10 AC14: 지원 문법(5절)만 블록·인라인으로 바꾸고 나머지는 글자 그대로 둔다
describe('renderMarkdown (F-10 AC14)', () => {
  it('AC14 빈 값이면 빈 배열이다', () => {
    expect(renderMarkdown('')).toEqual([])
    expect(renderMarkdown(null)).toEqual([])
    expect(renderMarkdown(undefined)).toEqual([])
  })

  it('AC14 ## 는 h3, ### 는 h4, 빈 줄로 나눈 글은 문단이다', () => {
    expect(renderMarkdown('## 핵심\n첫 문단\n이어지는 줄\n\n둘째 문단\n### 세부')).toEqual([
      { type: 'h3', inline: [text('핵심')] },
      { type: 'p', inline: [text('첫 문단\n이어지는 줄')] },
      { type: 'p', inline: [text('둘째 문단')] },
      { type: 'h4', inline: [text('세부')] },
    ])
  })

  it('AC14 - 목록과 두 칸 들여쓴 - 한 단계 중첩', () => {
    expect(renderMarkdown('- 하나\n  - 하나의 하위\n  - 둘째 하위\n- 둘')).toEqual([
      {
        type: 'ul',
        items: [
          {
            inline: [text('하나')],
            children: [{ inline: [text('하나의 하위')] }, { inline: [text('둘째 하위')] }],
          },
          { inline: [text('둘')], children: [] },
        ],
      },
    ])
  })

  it('AC14 목록 다음 줄이 목록이 아니면 목록을 닫고 문단을 시작한다', () => {
    expect(renderMarkdown('- 항목\n그다음 글')).toEqual([
      { type: 'ul', items: [{ inline: [text('항목')], children: [] }] },
      { type: 'p', inline: [text('그다음 글')] },
    ])
  })

  it('AC14 ``` 코드 블록은 안의 줄을 그대로 담고, 언어 표시는 버린다', () => {
    expect(
      renderMarkdown('설명\n```java\nint a = 1;\n\n## 제목 아님\n- 목록 아님\n```\n끝'),
    ).toEqual([
      { type: 'p', inline: [text('설명')] },
      { type: 'pre', code: 'int a = 1;\n\n## 제목 아님\n- 목록 아님' },
      { type: 'p', inline: [text('끝')] },
    ])
  })

  it('AC14 닫히지 않은 코드 블록은 끝까지 코드다', () => {
    expect(renderMarkdown('```\na\nb')).toEqual([{ type: 'pre', code: 'a\nb' }])
  })

  it('AC14 인라인 `코드`와 **굵게**, 굵게 안의 코드', () => {
    expect(
      renderMarkdown('서버는 **서명**과 만료(`exp`)를 본다. **`jakarta.validation`** 끝'),
    ).toEqual([
      {
        type: 'p',
        inline: [
          text('서버는 '),
          { type: 'strong', children: [text('서명')] },
          text('과 만료('),
          code('exp'),
          text(')를 본다. '),
          { type: 'strong', children: [code('jakarta.validation')] },
          text(' 끝'),
        ],
      },
    ])
  })

  it('AC14 코드 안의 ** 는 굵게가 아니다', () => {
    expect(renderMarkdown('`a**b**c`')).toEqual([{ type: 'p', inline: [code('a**b**c')] }])
  })

  it('AC14 링크·표·번호 목록·인용·# 제목은 글자 그대로 둔다', () => {
    const source = '# 큰 제목\n[문서](https://x.y)\n| a | b |\n1. 첫째\n> 인용'
    expect(renderMarkdown(source)).toEqual([{ type: 'p', inline: [text(source)] }])
  })

  it('AC14 HTML은 태그로 바꾸지 않고 글자로 둔다', () => {
    const source = '<script>alert(1)</script> <img src=x onerror="alert(2)">'
    expect(renderMarkdown(source)).toEqual([{ type: 'p', inline: [text(source)] }])
  })

  it('AC14 줄 끝 CRLF도 같은 결과다', () => {
    expect(renderMarkdown('## 제목\r\n- 항목\r\n')).toEqual(renderMarkdown('## 제목\n- 항목\n'))
  })
})
