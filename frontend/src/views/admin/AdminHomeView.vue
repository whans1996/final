<template>
  <!-- 관리자 홈 (F-00b AC9). 실제 관리 화면은 F-13(카드)·F-24(챌린지)가 /admin 자식 route로 붙인다 -->
  <div class="admin-home">
    <h1>관리자</h1>
    <p class="hint">관리할 항목을 고르세요.</p>
  </div>
</template>

<style scoped>
h1 {
  margin: 0;
}

.hint {
  margin-top: var(--space-4);
  color: var(--color-text-muted);
}
</style>
