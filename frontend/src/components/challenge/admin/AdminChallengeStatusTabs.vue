<script setup>
// SC-21 관리자 챌린지 목록의 상태 탭 (F-24 AC11). 상태는 저장하지 않고 서버가 오늘 기준으로 계산한다.
import { CHALLENGE_STATUSES } from './labels'

defineProps({
  /** 'ALL' | 'UPCOMING' | 'ONGOING' | 'ENDED' */
  modelValue: { type: String, required: true },
})

const emit = defineEmits(['update:modelValue'])

const TABS = [{ value: 'ALL', label: '전체' }, ...CHALLENGE_STATUSES]
</script>

<template>
  <div class="tabs" role="tablist" aria-label="챌린지 상태">
    <button
      v-for="tab in TABS"
      :key="tab.value"
      type="button"
      role="tab"
      :data-status-tab="tab.value"
      :aria-selected="modelValue === tab.value ? 'true' : 'false'"
      :class="{ active: modelValue === tab.value }"
      @click="emit('update:modelValue', tab.value)"
    >
      {{ tab.label }}
    </button>
  </div>
</template>

<style scoped>
.tabs {
  display: flex;
  gap: var(--space-1);
  margin-top: var(--space-4);
  border-bottom: 1px solid var(--color-border);
  overflow-x: auto;
}

button {
  padding: var(--space-2) var(--space-3);
  border: 0;
  border-bottom: 2px solid transparent;
  background: none;
  font: inherit;
  color: var(--color-text-muted);
  cursor: pointer;
  white-space: nowrap;
}

button.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
  font-weight: 700;
}
</style>
