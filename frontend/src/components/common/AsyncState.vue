<script setup>
// 로딩·에러·빈 상태를 한곳에서 그린다 (F-29a AC9). 우선순위 loading > error > empty, 그 밖에는 기본 슬롯.
// error는 사용자에게 보일 문구(errorMessage(e))를 준다.
import EmptyState from './EmptyState.vue'

defineProps({
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
  empty: { type: Boolean, default: false },
  emptyMessage: { type: String, default: '아직 내용이 없습니다.' },
})

const emit = defineEmits(['retry'])
</script>

<template>
  <template v-if="loading">
    <slot name="skeleton">
      <div role="status" class="flex items-center justify-center gap-2 py-8 text-text-muted">
        <span
          data-spinner
          aria-hidden="true"
          class="size-5 animate-spin rounded-full border-2 border-current border-t-transparent"
        />
        불러오는 중…
      </div>
    </slot>
  </template>
  <div
    v-else-if="error"
    role="alert"
    class="mt-6 flex flex-col items-center gap-3 rounded-md border border-danger/30 bg-danger-soft px-4 py-6 text-center text-danger"
  >
    <p>{{ error }}</p>
    <button
      type="button"
      class="h-8 rounded-sm border border-border bg-surface px-3 text-sm font-semibold text-text hover:bg-bg"
      @click="emit('retry')"
    >
      다시 시도
    </button>
  </div>
  <EmptyState v-else-if="empty" :message="emptyMessage">
    <template v-if="$slots['empty-action']" #action>
      <slot name="empty-action" />
    </template>
  </EmptyState>
  <slot v-else />
</template>
