<script setup>
// 목록 화면 공용 페이지 이동 (F-00b AC11). page는 1부터. 한 페이지에 다 들어가면 숨는다.
// (이름이 Pagination이 아닌 것은 eslint vue/multi-word-component-names 때문이다)
import { computed } from 'vue'

const props = defineProps({
  page: { type: Number, required: true },
  size: { type: Number, required: true },
  total: { type: Number, required: true },
})

const emit = defineEmits(['change'])

const lastPage = computed(() => Math.max(1, Math.ceil(props.total / props.size)))
</script>

<template>
  <nav v-if="total > size" class="pager" aria-label="페이지 이동">
    <button
      type="button"
      data-action="prev"
      :disabled="page <= 1"
      @click="emit('change', page - 1)"
    >
      이전
    </button>
    <span>{{ page }} / {{ lastPage }}</span>
    <button
      type="button"
      data-action="next"
      :disabled="page >= lastPage"
      @click="emit('change', page + 1)"
    >
      다음
    </button>
  </nav>
</template>

<style scoped>
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
  margin-top: var(--space-6);
}

.pager button {
  padding: var(--space-2) var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  font: inherit;
  cursor: pointer;
}

.pager button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
