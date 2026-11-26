<script setup>
// 분석·퀴즈·복습을 끝낸 화면의 챌린지 진행 (#211). 진행 중인 내 챌린지 중 같은 목표 유형만 달성도와 상세 링크로 보인다.
// 집계는 서버가 활동과 함께 하므로(F-25) 활동 뒤 refreshKey를 바꿔 다시 부른다.
// 보조 정보라 해당 챌린지가 없거나 조회가 실패하면 아무것도 보이지 않는다.
import { ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchMyChallenges } from '@/api/challenges'
import ChallengeProgressBar from '@/components/challenge/ChallengeProgressBar.vue'

// 서버 page size 상한. 진행 중 참여 챌린지가 이보다 많은 경우는 없다고 본다
const MAX_SIZE = 100

const props = defineProps({
  /** 'REVIEW_COUNT' | 'QUIZ_COUNT' | 'ANALYSIS_COUNT' */
  goalType: { type: String, required: true },
  /** 바뀌면 다시 불러온다 */
  refreshKey: { type: [Number, String], default: 0 },
})

const items = ref([])
let lastRequest = 0

async function load() {
  const request = ++lastRequest
  try {
    const result = await fetchMyChallenges({ status: 'ONGOING', size: MAX_SIZE })
    if (request !== lastRequest) return
    items.value = (result?.items ?? []).filter((c) => c.goalType === props.goalType)
  } catch {
    if (request === lastRequest) items.value = []
  }
}

watch(() => [props.goalType, props.refreshKey], load, { immediate: true })
</script>

<template>
  <section
    v-if="items.length > 0"
    class="challenge-progress"
    data-section="challenge-progress"
    aria-label="참여 중인 챌린지"
  >
    <h2>🏁 참여 중인 챌린지</h2>
    <ul>
      <li v-for="c in items" :key="c.id" :data-challenge-id="c.id">
        <RouterLink :to="{ name: 'challenge-detail', params: { id: c.id } }" class="title">
          {{ c.title }}
        </RouterLink>
        <ChallengeProgressBar :progress="c.progress" :goal-value="c.goalValue" />
        <span v-if="c.completedAt" class="completed" data-badge="completed">달성 완료</span>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.challenge-progress {
  padding: var(--space-3) var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
}

h2 {
  margin: 0 0 var(--space-2);
  font-size: 1em;
}

ul {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin: 0;
  padding: 0;
  list-style: none;
}

li {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 2fr) auto;
  align-items: center;
  gap: var(--space-3);
}

.title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.completed {
  padding: 0 var(--space-2);
  border-radius: 999px;
  color: #047857;
  background: #d1fae5;
  font-size: 0.85em;
  font-weight: 700;
}
</style>
