<script setup>
// SC-20 관리자 카드 목록의 상태 탭 (F-13 AC13). 검수 대기·공개·숨김에는 상태별 개수(AC2)를 붙이고, 전체(ALL)는 개수 없이 둔다.
import { CONCEPT_STATUSES } from './labels'

defineProps({
  /** 'DRAFT' | 'PUBLISHED' | 'HIDDEN' | 'ALL' */
  modelValue: { type: String, required: true },
  /** { DRAFT, PUBLISHED, HIDDEN } — 아직 못 받았으면 null */
  counts: { type: Object, default: null },
})

const emit = defineEmits(['update:modelValue'])

const TABS = [...CONCEPT_STATUSES, { value: 'ALL', label: '전체' }]
</script>

<template>
  <div class="tabs" role="tablist" aria-label="카드 상태">
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
      <span v-if="counts && tab.value !== 'ALL'" class="count">{{ counts[tab.value] ?? 0 }}</span>
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

.count {
  margin-left: var(--space-1);
  padding: 0 var(--space-2);
  border-radius: 999px;
  background: var(--color-background);
  font-size: 0.85em;
}
</style>
