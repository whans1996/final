<script setup>
// 팔로워·팔로잉 목록의 한 사람 (F-04 AC10). 이니셜·닉네임(SC-18 링크)·수준 배지.
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { LEVELS } from '@/utils/authValidation'

const props = defineProps({
  user: { type: Object, required: true },
})

const initial = computed(() => ([...(props.user.nickname ?? '')][0] ?? '?').toUpperCase())
const levelLabel = computed(() => LEVELS.find((l) => l.value === props.user.level)?.label ?? '')
</script>

<template>
  <li class="follow-user" :data-follow-user="user.id">
    <RouterLink :to="`/users/${user.id}`" class="who">
      <span class="avatar" aria-hidden="true">{{ initial }}</span>
      <span class="nickname">{{ user.nickname }}</span>
    </RouterLink>
    <span v-if="levelLabel" class="level-badge">{{ levelLabel }}</span>
  </li>
</template>

<style scoped>
.follow-user {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--color-border);
}

.who {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-width: 0;
  color: inherit;
  text-decoration: none;
}

.who:hover .nickname {
  text-decoration: underline;
}

.avatar {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 2.25em;
  height: 2.25em;
  border-radius: 50%;
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 12%, transparent);
  font-weight: 700;
}

.nickname {
  overflow-wrap: anywhere;
  font-weight: 600;
}

.level-badge {
  flex: none;
  padding: var(--space-1) var(--space-3);
  border-radius: 999px;
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 10%, transparent);
  font-size: 0.85em;
  font-weight: 600;
}
</style>
