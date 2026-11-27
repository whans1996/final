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

const buttonClass =
  'h-8 rounded-sm border border-border bg-surface px-3 text-sm font-semibold text-text hover:bg-bg disabled:cursor-not-allowed disabled:opacity-50'
</script>

<template>
  <nav
    v-if="total > size"
    class="pager mt-6 flex items-center justify-center gap-4 text-sm"
    aria-label="페이지 이동"
  >
    <button
      type="button"
      data-action="prev"
      :class="buttonClass"
      :disabled="page <= 1"
      @click="emit('change', page - 1)"
    >
      이전
    </button>
    <span>{{ page }} / {{ lastPage }}</span>
    <button
      type="button"
      data-action="next"
      :class="buttonClass"
      :disabled="page >= lastPage"
      @click="emit('change', page + 1)"
    >
      다음
    </button>
  </nav>
</template>
