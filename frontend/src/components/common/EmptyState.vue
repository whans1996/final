<script setup>
// 목록 화면 공용 빈 상태 (F-00b AC11). 다음 행동 버튼은 action 슬롯으로 넣는다.
defineProps({
  message: { type: String, required: true },
})
</script>

<template>
  <div
    class="empty mt-6 flex flex-col items-center gap-4 rounded-md border border-dashed border-border px-4 py-8 text-center text-text-muted"
  >
    <p>{{ message }}</p>
    <slot name="action" />
  </div>
</template>
