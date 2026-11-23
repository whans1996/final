<script setup>
// SC-24 모듈별 이해도 (F-20b AC12). 모듈마다 레이어 라벨·문항 수·정답률 막대.
// 정답률이 가장 낮은 모듈(같으면 앞 순서) 하나에 "가장 약한 모듈" 칩. null(풀이 없음)은 막대 없이 문구만.
import { computed } from 'vue'
import { layerLabel } from '@/utils/layers'

const props = defineProps({
  modules: { type: Array, required: true },
})

const weakest = computed(() => {
  let found = null
  for (const m of props.modules) {
    if (m.correctRate === null) continue
    if (found === null || m.correctRate < found.correctRate) found = m
  }
  return found?.layer ?? null
})
</script>

<template>
  <ul class="modules">
    <li v-for="m in modules" :key="m.layer" class="module" :data-module="m.layer">
      <div class="label">
        <strong>{{ layerLabel(m.layer) }}</strong>
        <span class="count">{{ m.questionCount }}문항</span>
        <span v-if="m.layer === weakest" class="chip" data-role="weakest">가장 약한 모듈</span>
      </div>
      <div v-if="m.correctRate !== null" class="rate">
        <div class="track">
          <div class="bar" data-role="bar" :style="{ width: `${m.correctRate}%` }"></div>
        </div>
        <span class="percent">{{ m.correctRate }}%</span>
      </div>
      <span v-else class="none">풀이 없음</span>
    </li>
  </ul>
</template>

<style scoped>
.modules {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin: 0;
  padding: 0;
  list-style: none;
}

.module {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.label {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
}

.count,
.none {
  color: var(--color-text-muted);
  font-size: 0.9em;
}

.chip {
  padding: 0 var(--space-2);
  border: 1px solid var(--color-danger);
  border-radius: 999px;
  color: var(--color-danger);
  font-size: 0.8em;
}

.rate {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.track {
  flex: 1;
  height: 0.75rem;
  border-radius: 999px;
  background: var(--color-surface);
  overflow: hidden;
}

.bar {
  height: 100%;
  background: var(--color-primary);
}

.percent {
  min-width: 3em;
  text-align: right;
}
</style>
