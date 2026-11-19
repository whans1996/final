<script setup>
// 내 코드에서 이 개념이 등장한 위치 (F-10 AC10). 파일이 있으면 분석의 파일 탭, 없으면 분석 상세로 간다.
import { RouterLink } from 'vue-router'

defineProps({
  /** [{ analysisId, analysisTitle, fileId, filePath, lineHint, contextNote }] */
  items: { type: Array, required: true },
})

function target(item) {
  return item.fileId == null
    ? `/analyses/${item.analysisId}`
    : `/analyses/${item.analysisId}?tab=files&file=${item.fileId}`
}

/** "분석 제목 · 파일 경로 · 줄 힌트 · 맥락"에서 빈 값은 건너뛴다 */
function label(item) {
  return [item.analysisTitle, item.filePath, item.lineHint, item.contextNote]
    .filter((part) => part)
    .join(' · ')
}
</script>

<template>
  <ul class="appearances">
    <li v-for="item in items" :key="item.analysisId">
      <RouterLink :to="target(item)">{{ label(item) }}</RouterLink>
    </li>
  </ul>
</template>

<style scoped>
.appearances {
  display: grid;
  gap: var(--space-2);
  margin: 0;
  padding: 0;
  list-style: none;
}

.appearances a {
  display: block;
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  color: inherit;
  text-decoration: none;
  overflow-wrap: anywhere;
}

.appearances a:hover {
  border-color: var(--color-primary);
}
</style>
