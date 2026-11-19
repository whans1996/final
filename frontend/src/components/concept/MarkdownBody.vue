<script setup>
// 개념 카드 본문 (F-10 AC9·AC14). renderMarkdown의 블록 배열을 텍스트 보간·h()로 그린다. v-html을 쓰지 않는다.
import { computed, h } from 'vue'
import { renderMarkdown } from './markdown'

const props = defineProps({
  /** Markdown 원문 */
  source: { type: String, default: '' },
})

const blocks = computed(() => renderMarkdown(props.source))

function renderToken(token) {
  if (token.type === 'code') return h('code', token.text)
  if (token.type === 'strong') return h('strong', token.children.map(renderToken))
  return token.text
}

/** 인라인 토큰(text·code·strong) 묶음. 문자열 자식은 텍스트 노드가 되어 HTML로 해석되지 않는다. */
const Inline = (inlineProps) => inlineProps.tokens.map(renderToken)
Inline.props = ['tokens']
</script>

<template>
  <div class="markdown">
    <template v-for="(block, index) in blocks" :key="index">
      <h3 v-if="block.type === 'h3'"><Inline :tokens="block.inline" /></h3>
      <h4 v-else-if="block.type === 'h4'"><Inline :tokens="block.inline" /></h4>
      <pre v-else-if="block.type === 'pre'"><code>{{ block.code }}</code></pre>
      <ul v-else-if="block.type === 'ul'">
        <li v-for="(item, itemIndex) in block.items" :key="itemIndex">
          <Inline :tokens="item.inline" />
          <ul v-if="item.children.length">
            <li v-for="(child, childIndex) in item.children" :key="childIndex">
              <Inline :tokens="child.inline" />
            </li>
          </ul>
        </li>
      </ul>
      <p v-else><Inline :tokens="block.inline" /></p>
    </template>
  </div>
</template>

<style scoped>
.markdown {
  line-height: 1.7;
  overflow-wrap: anywhere;
}

.markdown h3 {
  margin: var(--space-6) 0 var(--space-2);
  font-size: 1.1em;
}

.markdown h4 {
  margin: var(--space-4) 0 var(--space-2);
  font-size: 1em;
}

.markdown > :first-child {
  margin-top: 0;
}

.markdown p {
  margin: var(--space-2) 0;
  white-space: pre-line;
}

.markdown ul {
  margin: var(--space-2) 0;
  padding-left: var(--space-6);
}

.markdown :deep(code) {
  padding: 0 var(--space-1);
  border-radius: 4px;
  background: var(--color-background);
  font-size: 0.9em;
}

.markdown pre {
  margin: var(--space-3) 0;
  padding: var(--space-3);
  overflow-x: auto;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-background);
}

.markdown pre code {
  padding: 0;
}
</style>
