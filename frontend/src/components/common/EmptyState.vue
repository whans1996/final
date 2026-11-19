<script setup>
// 목록 화면 공용 빈 상태 (F-00b AC11). 다음 행동 버튼은 action 슬롯으로 넣는다.
defineProps({
  message: { type: String, required: true },
})
</script>

<template>
  <div class="empty">
    <p>{{ message }}</p>
    <slot name="action" />
  </div>
</template>

<style scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-4);
  margin-top: var(--space-6);
  padding: var(--space-8) var(--space-4);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius);
  text-align: center;
}

.empty p {
  margin: 0;
}
</style>
