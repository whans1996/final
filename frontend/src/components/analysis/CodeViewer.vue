<script setup>
// 줄 번호가 붙은 코드 상자 (F-06 6절). 하이라이트 라이브러리는 쓰지 않는다.
// 긴 줄은 이 상자 안에서만 가로 스크롤한다 (375px에서 페이지 가로 스크롤 없음, NF-02).
import { computed } from 'vue'

const props = defineProps({
  content: { type: String, default: '' },
  label: { type: String, default: '코드' },
})

const lines = computed(() => {
  const all = props.content.split('\n')
  // 파일 끝 줄바꿈 때문에 생기는 마지막 빈 줄은 보여 주지 않는다
  if (all.length > 1 && all[all.length - 1] === '') all.pop()
  return all
})
</script>

<template>
  <div class="code" role="region" :aria-label="label" tabindex="0">
    <pre><code><span
      v-for="(text, index) in lines"
      :key="index"
      class="line"
      :data-line="index + 1"
    ><span class="ln" aria-hidden="true">{{ index + 1 }}</span>{{ text || ' ' }}</span></code></pre>
  </div>
</template>

<style scoped>
.code {
  max-width: 100%;
  overflow-x: auto;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: #f8fafc;
}

pre {
  margin: 0;
  padding: var(--space-3) 0;
  font-family: ui-monospace, SFMono-Regular, Consolas, 'D2Coding', monospace;
  font-size: 0.85em;
  line-height: 1.55;
  white-space: pre;
  word-break: normal;
  overflow-wrap: normal;
}

.line {
  display: block;
  padding-right: var(--space-4);
}

.ln {
  display: inline-block;
  min-width: 3.5em;
  margin-right: var(--space-3);
  padding-right: var(--space-2);
  border-right: 1px solid var(--color-border);
  color: var(--color-text-muted);
  text-align: right;
  user-select: none;
}
</style>
