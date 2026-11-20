<script setup>
// SC-14 게시판 탭 (F-17 AC13). 전체('') + 게시판 3종. 모바일에서는 가로로 스크롤되는 칩이 된다.
import { BOARD_TYPES } from './boardTypes'

defineProps({
  modelValue: { type: String, default: '' },
})

const emit = defineEmits(['change'])

const tabs = [{ value: '', label: '전체' }, ...BOARD_TYPES]
</script>

<template>
  <div class="tabs" role="tablist" aria-label="게시판">
    <button
      v-for="tab in tabs"
      :key="tab.value || 'all'"
      type="button"
      role="tab"
      :data-board="tab.value || 'ALL'"
      :aria-selected="tab.value === modelValue ? 'true' : 'false'"
      :class="{ active: tab.value === modelValue }"
      @click="emit('change', tab.value)"
    >
      {{ tab.label }}
    </button>
  </div>
</template>

<style scoped>
.tabs {
  display: flex;
  gap: var(--space-2);
  overflow-x: auto;
  padding-bottom: var(--space-1);
}

.tabs button {
  flex: none;
  padding: var(--space-2) var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: var(--color-surface);
  color: var(--color-text);
  font: inherit;
  white-space: nowrap;
  cursor: pointer;
}

.tabs button.active {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: #fff;
}
</style>
