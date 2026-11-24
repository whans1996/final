<script setup>
// 참여한 챌린지 짧은 목록 (F-25 AC14 대시보드 "참여 중 챌린지", AC15 프로필 "참여 챌린지"). 불러오기는 각 절이 한다.
// today를 주면 기간 대신 종료까지 남은 D-n을 보여 준다 (대시보드는 진행 중만 보이므로).
import { RouterLink } from 'vue-router'
import { periodLabel } from '@/components/challenge/labels'
import ChallengeProgressBar from '@/components/challenge/ChallengeProgressBar.vue'
import { dDayLabel } from '@/utils/seoulDate'

defineProps({
  items: { type: Array, required: true },
  /** 서울 오늘 'yyyy-MM-dd' */
  today: { type: String, default: '' },
})
</script>

<template>
  <ul class="challenges">
    <li v-for="c in items" :key="c.id">
      <RouterLink :to="`/challenges/${c.id}`" class="challenge" :data-challenge-id="c.id">
        <span class="head">
          <strong class="title" data-field="title">{{ c.title }}</strong>
          <span v-if="c.completedAt" class="badge" data-badge="completed">달성</span>
        </span>
        <span v-if="today" class="meta" data-field="dday">{{ dDayLabel(today, c.endDate) }}</span>
        <span v-else class="meta" data-field="period">{{
          periodLabel(c.startDate, c.endDate)
        }}</span>
        <ChallengeProgressBar :progress="c.progress" :goal-value="c.goalValue" />
      </RouterLink>
    </li>
  </ul>
</template>

<style scoped>
.challenges {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin: 0;
  padding: 0;
  list-style: none;
}

.challenge {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  color: inherit;
  text-decoration: none;
}

.challenge:hover {
  border-color: var(--color-primary);
}

.head {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.title {
  min-width: 0;
  overflow-wrap: anywhere;
}

.badge {
  flex-shrink: 0;
  padding: 2px var(--space-2);
  border-radius: 999px;
  color: #047857;
  background: #d1fae5;
  font-size: 0.8em;
  font-weight: 700;
}

.meta {
  color: var(--color-text-muted);
  font-size: 0.9em;
}
</style>
