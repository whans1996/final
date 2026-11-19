<script setup>
// 관리자 공통 레이아웃 (F-00b AC9). 사이드 메뉴는 /admin 자식 route의 meta.adminNav로 만든다.
// 375px 같은 좁은 폭에서는 메뉴가 본문 위 가로 메뉴로 접힌다 (AC12).
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { adminNavItems } from '@/utils/navItems'

const route = useRoute()

// matched[0]은 /admin 부모 route 레코드다 (children에 자식 route 정의가 들어 있다)
const items = computed(() => adminNavItems(route.matched[0]))
</script>

<template>
  <div class="admin">
    <nav class="admin-nav" aria-label="관리 메뉴">
      <ul v-if="items.length > 0">
        <li v-for="item in items" :key="item.to.name">
          <RouterLink :to="item.to">{{ item.label }}</RouterLink>
        </li>
      </ul>
      <p v-else class="hint">아직 관리 메뉴가 없습니다.</p>
    </nav>
    <section class="admin-body">
      <RouterView />
    </section>
  </div>
</template>

<style scoped>
.admin {
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  gap: var(--space-6);
  padding: var(--space-8) 0;
}

.admin-nav {
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  align-self: start;
}

.admin-nav ul {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin: 0;
  padding: 0;
  list-style: none;
}

.admin-nav a {
  display: block;
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius);
  color: inherit;
  text-decoration: none;
  white-space: nowrap;
}

.admin-nav a.router-link-active {
  color: var(--color-primary);
  background: var(--color-background);
  font-weight: 700;
}

.hint {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.9em;
}

.admin-body {
  min-width: 0;
}

@media (max-width: 640px) {
  .admin {
    grid-template-columns: minmax(0, 1fr);
    gap: var(--space-4);
    padding-top: var(--space-4);
  }

  .admin-nav {
    padding: var(--space-2);
  }

  .admin-nav ul {
    flex-direction: row;
    overflow-x: auto;
  }
}
</style>
