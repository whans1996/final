<script setup>
// SC-12 챌린지 카드 (F-25 AC12). 공개 목록 항목(ChallengeSummaryResponse)과 내 챌린지 항목(MyChallengeResponse)을 함께 받는다.
// 내 챌린지 항목은 progress가 있으므로 참여자 수 대신 달성도 바를 보여 준다.
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { goalIcon, goalLabel, periodLabel } from '@/components/challenge/labels'
import ChallengeProgressBar from '@/components/challenge/ChallengeProgressBar.vue'

const props = defineProps({
  challenge: { type: Object, required: true },
})

const isMine = computed(() => typeof props.challenge.progress === 'number')
</script>

<template>
  <RouterLink :to="`/challenges/${challenge.id}`" class="card" :data-challenge-id="challenge.id">
    <img v-if="challenge.imageUrl" class="image" :src="challenge.imageUrl" alt="" loading="lazy" />
    <span v-else class="image placeholder" :data-thumb="challenge.goalType" aria-hidden="true">{{
      goalIcon(challenge.goalType)
    }}</span>

    <div class="body">
      <div class="badges">
        <span v-if="challenge.joined" class="badge joined" data-badge="joined">참여 중</span>
        <span v-if="challenge.completedAt" class="badge completed" data-badge="completed"
          >달성</span
        >
      </div>
      <strong class="title" data-field="title">{{ challenge.title }}</strong>
      <span class="meta" data-field="period">{{
        periodLabel(challenge.startDate, challenge.endDate)
      }}</span>
      <span class="meta">
        <span data-field="goal">{{ goalLabel(challenge.goalType, challenge.goalValue) }}</span>
        <template v-if="!isMine">
          · <span data-field="participants">참여자 {{ challenge.participantCount }}명</span>
        </template>
      </span>
      <ChallengeProgressBar
        v-if="isMine"
        :progress="challenge.progress"
        :goal-value="challenge.goalValue"
      />
    </div>
  </RouterLink>
</template>

<style scoped>
.card {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  color: inherit;
  text-decoration: none;
}

.card:hover {
  border-color: var(--color-primary);
}

.image {
  display: block;
  width: 100%;
  aspect-ratio: 16 / 9;
  object-fit: cover;
}

.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-background);
  font-size: 2.5em;
}

.body {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding: var(--space-3) var(--space-4) var(--space-4);
}

.badges {
  display: flex;
  gap: var(--space-1);
}

.badges:empty {
  display: none;
}

.badge {
  padding: 2px var(--space-2);
  border-radius: 999px;
  font-size: 0.8em;
  font-weight: 700;
}

.joined {
  color: var(--color-primary);
  background: #eef2ff;
}

.completed {
  color: #047857;
  background: #d1fae5;
}

.title {
  overflow-wrap: anywhere;
}

.meta {
  color: var(--color-text-muted);
  font-size: 0.9em;
}
</style>
