<script setup>
// 홈 화면 자리. 실제 화면 구성은 화면 설계서(docs/산출물/06-화면설계서.md)와 기능 spec에서 정한다.
</script>

<template>
  <section class="home">
    <h1>바이브노트</h1>
    <p class="tagline">AI가 쓴 코드를 수준별 설명 → 개념 카드 → 퀴즈·복습으로 이해합니다.</p>
  </section>
</template>

<style scoped>
.home {
  padding: var(--space-8) 0;
}

.tagline {
  margin-top: var(--space-3);
  color: var(--color-text-muted);
}
</style>
