// 개념 카드 본문용 최소 Markdown 렌더러 (F-10 AC14, docs/specs/F-10-concept-detail.md 5절).
// HTML 문자열을 만들지 않고 블록 배열을 돌려준다. MarkdownBody.vue가 텍스트 보간으로 그리므로 HTML은 글자 그대로 보인다 (v-html 금지).
// 지원: ## → h3, ### → h4, - 목록(두 칸 들여쓴 - 한 단계 중첩), ``` 코드 블록, 빈 줄로 나눈 문단, 인라인 `코드`·**굵게**.
// 나머지(링크·표·번호 목록·인용·# 제목·HTML)는 문단 글자로 남는다. 문단 안 줄바꿈은 유지한다 (white-space: pre-line).

const INLINE = /`([^`\n]+)`|\*\*(.+?)\*\*/g
const CODE = /`([^`\n]+)`/g

/** 문자열을 regex 매치 기준으로 text·토큰으로 나눈다. */
function split(source, regex, toToken) {
  const tokens = []
  let last = 0
  for (const match of source.matchAll(regex)) {
    if (match.index > last) tokens.push({ type: 'text', text: source.slice(last, match.index) })
    tokens.push(toToken(match))
    last = match.index + match[0].length
  }
  if (last < source.length) tokens.push({ type: 'text', text: source.slice(last) })
  return tokens
}

const codeOnly = (source) => split(source, CODE, (m) => ({ type: 'code', text: m[1] }))

/** 인라인 토큰: text / code / strong(children은 text·code). */
export function renderInline(source) {
  return split(source, INLINE, (m) =>
    m[1] !== undefined
      ? { type: 'code', text: m[1] }
      : { type: 'strong', children: codeOnly(m[2]) },
  )
}

/**
 * @param {string | null | undefined} text Markdown 원문
 * @returns {Array<
 *   | { type: 'h3' | 'h4' | 'p', inline: object[] }
 *   | { type: 'ul', items: { inline: object[], children: { inline: object[] }[] }[] }
 *   | { type: 'pre', code: string }
 * >}
 */
export function renderMarkdown(text) {
  if (!text) return []
  const blocks = []
  let paragraph = null // 모으는 중인 문단 줄
  let list = null // 열린 ul 블록
  let code = null // 모으는 중인 코드 줄

  const flush = () => {
    if (paragraph) blocks.push({ type: 'p', inline: renderInline(paragraph.join('\n')) })
    paragraph = null
    list = null
  }

  for (const line of text.replace(/\r\n?/g, '\n').split('\n')) {
    if (code) {
      if (line.trimStart().startsWith('```')) {
        blocks.push({ type: 'pre', code: code.join('\n') })
        code = null
      } else {
        code.push(line)
      }
      continue
    }
    if (line.trimStart().startsWith('```')) {
      flush()
      code = []
      continue
    }
    if (line.trim() === '') {
      flush()
      continue
    }

    const heading = /^(###|##) (.*)$/.exec(line)
    if (heading) {
      flush()
      blocks.push({ type: heading[1] === '##' ? 'h3' : 'h4', inline: renderInline(heading[2]) })
      continue
    }

    const nested = /^ {2}- (.*)$/.exec(line)
    const item = /^- (.*)$/.exec(line)
    if (nested && list?.items.length) {
      list.items.at(-1).children.push({ inline: renderInline(nested[1]) })
      continue
    }
    if (item || nested) {
      if (paragraph) flush()
      if (!list) {
        list = { type: 'ul', items: [] }
        blocks.push(list)
      }
      list.items.push({ inline: renderInline((item ?? nested)[1]), children: [] })
      continue
    }

    list = null
    paragraph ??= []
    paragraph.push(line)
  }

  if (code) blocks.push({ type: 'pre', code: code.join('\n') })
  flush()
  return blocks
}
