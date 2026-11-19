import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import MarkdownBody from '@/components/concept/MarkdownBody.vue'

describe('MarkdownBody (F-10 AC14 렌더 결과)', () => {
  it('AC14 본문의 HTML은 DOM 요소가 아니라 글자로 보인다', () => {
    const source = [
      '<script>alert(1)</script>',
      '',
      '- <img src=x onerror=alert(2)> **<b>굵게</b>** `<i>코드</i>`',
      '',
      '```',
      '<iframe src="x"></iframe>',
      '```',
    ].join('\n')

    const wrapper = mount(MarkdownBody, { props: { source } })

    for (const tag of ['script', 'img', 'b', 'i', 'iframe']) {
      expect(wrapper.find(tag).exists()).toBe(false)
    }
    expect(wrapper.text()).toContain('<script>alert(1)</script>')
    expect(wrapper.text()).toContain('<img src=x onerror=alert(2)>')
    expect(wrapper.find('li strong').text()).toBe('<b>굵게</b>')
    expect(wrapper.find('li code').text()).toBe('<i>코드</i>')
    expect(wrapper.find('pre code').text()).toBe('<iframe src="x"></iframe>')
  })
})
