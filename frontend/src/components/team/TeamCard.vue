<script setup>
// 내 팀 목록 카드 (F-20a AC12). 이름, 역할 칩, 인원, 소개(없으면 생략). 누르면 SC-23.
import { RouterLink } from 'vue-router'
import { roleLabel } from './teamLabels'

defineProps({
  team: { type: Object, required: true },
})
</script>

<template>
  <RouterLink :to="`/teams/${team.id}`" class="team-card" :data-team-card="team.id">
    <div class="head">
      <strong class="name" data-role="name">{{ team.name }}</strong>
      <span class="chip" :class="{ owner: team.myRole === 'OWNER' }" data-role="role">
        {{ roleLabel(team.myRole) }}
      </span>
    </div>
    <p class="count" data-role="count">{{ team.memberCount }}명</p>
    <p v-if="team.description" class="description" data-role="description">
      {{ team.description }}
    </p>
  </RouterLink>
</template>

<style scoped>
.team-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  color: inherit;
  text-decoration: none;
}

.team-card:hover {
  border-color: var(--color-primary);
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.name {
  min-width: 0;
  overflow-wrap: anywhere;
}

.chip {
  flex: none;
  padding: var(--space-1) var(--space-3);
  border-radius: 999px;
  background: var(--color-background);
  font-size: 0.85em;
  font-weight: 600;
}

.chip.owner {
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 10%, transparent);
}

.count,
.description {
  margin: 0;
}

.count {
  color: var(--color-text-muted);
  font-size: 0.9em;
}

.description {
  overflow-wrap: anywhere;
}
</style>
