<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <header class="app-header">
    <RouterLink to="/" class="brand">바이브노트</RouterLink>
  </header>
  <main class="app-main">
    <RouterView />
  </main>
</template>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 var(--space-4);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface);
}

.brand {
  font-weight: 700;
  color: var(--color-primary);
  text-decoration: none;
}

.app-main {
  max-width: 1080px;
  margin: 0 auto;
  padding: 0 var(--space-4);
}
</style>
