<script setup>
// 분석 상태 배지 + 진행 중일 때의 진행률 막대. 목록(SC-06)과 상세(SC-07)가 같이 쓴다 (F-05a 6절).
import { computed } from 'vue'
import { STATUS_LABELS, isInProgress } from '@/utils/analysisStatus'

const props = defineProps({
  status: { type: String, required: true },
  progress: { type: Number, default: 0 },
})

const label = computed(() => STATUS_LABELS[props.status] ?? props.status)
const inProgress = computed(() => isInProgress(props.status))
const tone = computed(() => {
  if (props.status === 'DONE') return 'done'
  if (props.status === 'FAILED') return 'failed'
  return 'running'
})
</script>

<template>
  <div class="status">
    <span class="badge" :class="tone" data-status="badge">{{ label }}</span>
    <div
      v-if="inProgress"
      class="bar"
      role="progressbar"
      aria-label="분석 진행률"
      aria-valuemin="0"
      aria-valuemax="100"
      :aria-valuenow="progress"
    >
      <span class="fill" :style="{ width: `${progress}%` }"></span>
    </div>
  </div>
</template>

<style scoped>
.status {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.badge {
  align-self: flex-start;
  padding: 2px var(--space-2);
  border-radius: 999px;
  font-size: 0.85em;
  font-weight: 600;
}

.badge.running {
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 12%, transparent);
}

.badge.done {
  color: #047857;
  background: #d1fae5;
}

.badge.failed {
  color: var(--color-danger);
  background: color-mix(in srgb, var(--color-danger) 12%, transparent);
}

.bar {
  height: 8px;
  border-radius: 999px;
  background: var(--color-border);
  overflow: hidden;
}

.fill {
  display: block;
  height: 100%;
  background: var(--color-primary);
  transition: width 0.4s ease;
}
</style>
