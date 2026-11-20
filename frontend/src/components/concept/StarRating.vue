<script setup>
// 별점 (F-11). readonly면 ★/☆ 표시만, 아니면 별 버튼 5개로 1~5를 고른다 (v-model).
defineProps({
  modelValue: { type: Number, default: null },
  readonly: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])

const SCORES = [1, 2, 3, 4, 5]
</script>

<template>
  <span v-if="readonly" class="stars readonly" :aria-label="`별점 ${modelValue}점`">
    <span v-for="n in SCORES" :key="n" aria-hidden="true" :class="{ on: n <= modelValue }">{{
      n <= modelValue ? '★' : '☆'
    }}</span>
  </span>
  <span v-else class="stars" role="group" aria-label="별점 선택">
    <button
      v-for="n in SCORES"
      :key="n"
      type="button"
      class="star"
      :class="{ on: modelValue !== null && n <= modelValue }"
      :aria-label="`별점 ${n}점`"
      :aria-pressed="modelValue === n"
      :data-score="n"
      @click="emit('update:modelValue', n)"
    >
      {{ modelValue !== null && n <= modelValue ? '★' : '☆' }}
    </button>
  </span>
</template>

<style scoped>
.stars {
  display: inline-flex;
  color: var(--color-text-muted);
}

.on {
  color: #b45309;
}

.star {
  min-width: 32px;
  min-height: 32px;
  padding: 0;
  border: none;
  background: none;
  font-size: 1.4em;
  line-height: 1;
  cursor: pointer;
}

.star:focus-visible {
  outline: 2px solid var(--color-primary);
  border-radius: var(--radius);
}
</style>
