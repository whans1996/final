<script setup>
// 팀 멤버 목록 (F-20a AC14). 닉네임(SC-18 링크), 수준 배지, 팀장 칩, 가입일. 팀장 화면이면 팀원 행에 "내보내기".
import { RouterLink } from 'vue-router'
import { formatDate, levelLabel } from './teamLabels'

defineProps({
  members: { type: Array, required: true },
  canManage: { type: Boolean, default: false },
  busy: { type: Boolean, default: false },
})

const emit = defineEmits(['remove'])
</script>

<template>
  <ul class="members">
    <li
      v-for="member in members"
      :key="member.userId"
      class="member"
      :data-team-member="member.userId"
    >
      <div class="who">
        <RouterLink :to="`/users/${member.userId}`" class="nickname">
          {{ member.nickname }}
        </RouterLink>
        <span v-if="member.role === 'OWNER'" class="chip owner" data-role="owner">팀장</span>
        <span v-if="levelLabel(member.level)" class="chip" data-role="level">
          {{ levelLabel(member.level) }}
        </span>
      </div>
      <div class="side">
        <span class="joined" data-role="joined">{{ formatDate(member.joinedAt) }}</span>
        <button
          v-if="canManage && member.role !== 'OWNER'"
          type="button"
          class="remove"
          data-action="remove-member"
          :disabled="busy"
          @click="emit('remove', member)"
        >
          내보내기
        </button>
      </div>
    </li>
  </ul>
</template>

<style scoped>
.members {
  margin: 0;
  padding: 0;
  list-style: none;
}

.member {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--color-border);
}

.who,
.side {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
}

.nickname {
  overflow-wrap: anywhere;
  font-weight: 600;
  color: inherit;
}

.chip {
  padding: var(--space-1) var(--space-3);
  border-radius: 999px;
  background: var(--color-background);
  font-size: 0.8em;
  font-weight: 600;
}

.chip.owner {
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 10%, transparent);
}

.joined {
  color: var(--color-text-muted);
  font-size: 0.9em;
}

.remove {
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--color-danger);
  border-radius: var(--radius);
  background: transparent;
  color: var(--color-danger);
  font: inherit;
  font-size: 0.9em;
  cursor: pointer;
}

.remove:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
