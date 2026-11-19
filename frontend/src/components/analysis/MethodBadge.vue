<script setup>
// HTTP 메서드 배지. 개요 탭 API 목록과 실행 흐름 탭이 같은 색을 쓴다 (F-06, F-07).
defineProps({
  method: { type: String, required: true },
})
</script>

<template>
  <span class="method" :data-method="method">{{ method }}</span>
</template>

<style scoped>
.method {
  display: inline-block;
  min-width: 4.2em;
  padding: 0 var(--space-2);
  border-radius: 4px;
  color: #fff;
  background: #6b7280;
  font-size: 0.8em;
  font-weight: 700;
  text-align: center;
}

.method[data-method='GET'] {
  background: #2563eb;
}

.method[data-method='POST'] {
  background: #059669;
}

.method[data-method='PUT'],
.method[data-method='PATCH'] {
  background: #d97706;
}

.method[data-method='DELETE'] {
  background: #dc2626;
}
</style>
