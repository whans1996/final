<script setup>
// SC-24 팀 목표 하나의 멤버별 진행 (F-20c AC12). 달성 판정은 서버 값을 그대로 쓴다.
import { attemptRate, challengeStatusLabel, shortDate } from './teamLabels'

defineProps({
  challenge: { type: Object, required: true },
})
</script>

<template>
  <div class="challenge-progress" :data-challenge="challenge.challengeId">
    <p class="summary">
      <strong data-role="goal">
        정답률 {{ challenge.targetRate }}% 이상 · {{ shortDate(challenge.startDate) }} ~
        {{ shortDate(challenge.endDate) }}
      </strong>
      <span class="chip" :class="challenge.status.toLowerCase()" data-role="status">
        {{ challengeStatusLabel(challenge) }}
      </span>
    </p>
    <p class="summary">
      <span data-role="progress"
        >{{ challenge.achievedMemberCount }}/{{ challenge.memberCount }}명 달성</span
      >
      <span v-if="challenge.achieved" class="chip achieved" data-role="achieved">달성</span>
      <span v-else-if="challenge.status === 'ENDED'" class="chip missed" data-role="missed"
        >미달성</span
      >
    </p>
    <ul class="members">
      <li
        v-for="m in challenge.members"
        :key="m.userId"
        class="member"
        :data-challenge-member="m.userId"
      >
        <span class="nickname">{{ m.nickname }}</span>
        <span class="result" data-role="result">
          <template v-if="m.attempt">
            {{ m.attempt.score }}/{{ m.attempt.total }} ({{ attemptRate(m.attempt) }}%)
          </template>
          <template v-else>기간 안 풀이 없음</template>
        </span>
        <span v-if="m.achieved" class="chip achieved" data-role="member-achieved">달성</span>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.challenge-progress {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  min-width: 0;
}

.summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  margin: 0;
}

.members {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  margin: 0;
  padding: 0;
  list-style: none;
}

.member {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
}

.nickname {
  overflow-wrap: anywhere;
}

.result {
  color: var(--color-text-muted);
  font-size: 0.9em;
}

.chip {
  padding: 0 var(--space-2);
  border: 1px solid var(--color-border);
  border-radius: 999px;
  font-size: 0.85em;
}

.chip.ongoing,
.chip.achieved {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.chip.missed {
  border-color: var(--color-danger);
  color: var(--color-danger);
}
</style>
