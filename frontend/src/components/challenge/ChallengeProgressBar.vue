<script setup>
// 챌린지 달성도 바 "3 / 10회" (F-25 AC12~AC15). progress는 서버가 goalValue를 넘지 않게 저장한다.
import { computed } from 'vue'

const props = defineProps({
  progress: { type: Number, required: true },
  goalValue: { type: Number, required: true },
})

const percent = computed(() =>
  props.goalValue > 0 ? Math.min(100, Math.round((props.progress / props.goalValue) * 100)) : 0,
)
const text = computed(
  () => `${props.progress.toLocaleString('ko-KR')} / ${props.goalValue.toLocaleString('ko-KR')}회`,
)
</script>

<template>
  <div class="progress">
    <div
      class="track"
      role="progressbar"
      aria-label="달성도"
      aria-valuemin="0"
      :aria-valuemax="goalValue"
      :aria-valuenow="progress"
      :aria-valuetext="text"
    >
      <div class="fill" :class="{ full: percent === 100 }" :style="{ width: `${percent}%` }" />
    </div>
    <span class="count">{{ text }}</span>
  </div>
</template>

<style scoped>
.progress {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
}

.track {
  flex: 1;
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--color-border);
}

.fill {
  height: 100%;
  border-radius: 999px;
  background: var(--color-primary);
}

.fill.full {
  background: #059669;
}

.count {
  flex-shrink: 0;
  color: var(--color-text-muted);
  font-size: 0.85em;
  white-space: nowrap;
}
</style>
