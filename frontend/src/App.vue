<script setup>
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

async function onLogout() {
  await auth.logout()
  await router.push({ name: 'home' })
}
</script>

<template>
  <header class="app-header">
    <RouterLink to="/" class="brand">바이브노트</RouterLink>
    <nav class="account">
      <template v-if="auth.isLoggedIn">
        <RouterLink :to="{ name: 'analysis-new' }">분석 요청</RouterLink>
        <RouterLink :to="{ name: 'analyses' }">내 분석</RouterLink>
        <RouterLink
          v-if="auth.user"
          :to="{ name: 'user-profile', params: { id: auth.user.id } }"
          class="nickname"
        >
          {{ auth.user.nickname }}
        </RouterLink>
        <button type="button" class="link-button" @click="onLogout">로그아웃</button>
      </template>
      <template v-else>
        <RouterLink :to="{ name: 'login' }">로그인</RouterLink>
        <RouterLink :to="{ name: 'signup' }">회원가입</RouterLink>
      </template>
    </nav>
  </header>
  <main class="app-main">
    <RouterView />
  </main>
</template>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  min-height: 56px;
  padding: var(--space-2) var(--space-4);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface);
}

.brand {
  font-weight: 700;
  color: var(--color-primary);
  text-decoration: none;
}

.account {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  column-gap: var(--space-4);
  row-gap: var(--space-1);
}

.nickname {
  font-weight: 600;
}

.link-button {
  padding: 0;
  border: none;
  background: none;
  color: var(--color-primary);
  font: inherit;
  cursor: pointer;
  text-decoration: underline;
}

.app-main {
  max-width: 1080px;
  margin: 0 auto;
  padding: 0 var(--space-4);
}
</style>
