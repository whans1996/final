<script setup>
// 분석·퀴즈·복습을 끝낸 화면의 챌린지 진행 (#211). 진행 중인 내 챌린지 중 같은 목표 유형만 달성도와 상세 링크로 보인다.
// 집계는 서버가 활동과 함께 하므로(F-25) 활동 뒤 refreshKey를 바꿔 다시 부른다.
// 진행 중 참여 챌린지가 size 상한(50)을 넘으면 다음 페이지를 이어 받는다 (#225, BE 필터 없이 FE에서 거른다).
// 보조 정보라 해당 챌린지가 없거나 조회가 실패하면 아무것도 보이지 않는다.
// 분석은 완료 커밋 뒤 별도 트랜잭션으로 집계하므로(F-25 spec 4절) DONE 직후 값이 1 늦을 수 있다. 다시 부르지는 않는다.
// 활동 직후 나타나는 블록이라 role="status"로 낭독한다.
import { ref, useId, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { JOINED_CHALLENGE_MAX_SIZE, fetchMyChallenges } from '@/api/challenges'
import ChallengeProgressBar from '@/components/challenge/ChallengeProgressBar.vue'

const props = defineProps({
  /** 'REVIEW_COUNT' | 'QUIZ_COUNT' | 'ANALYSIS_COUNT' */
  goalType: { type: String, required: true },
  /** 바뀌면 다시 불러온다 */
  refreshKey: { type: [Number, String], default: 0 },
})

const items = ref([])
const headingId = useId()
let lastRequest = 0
/** 페이지를 이어 받는 상한 (50 × 20 = 1000개). 서버 total이 잘못되어도 끝없이 부르지 않는다 */
const MAX_PAGES = 20

/**
 * 진행 중 참여 챌린지를 total까지 페이지를 이어 받는다 (#225). 빈 페이지가 오면 멈춘다.
 * 서버는 참여 시각 역순 OFFSET이라 받는 사이에 새로 참여하면 항목이 밀려 겹칠 수 있어 id로 거른다.
 */
async function fetchAllOngoing(request) {
  const byId = new Map()
  let received = 0
  for (let page = 1; page <= MAX_PAGES; page++) {
    const result = await fetchMyChallenges({
      status: 'ONGOING',
      size: JOINED_CHALLENGE_MAX_SIZE,
      page,
    })
    if (request !== lastRequest) return null
    const batch = result?.items ?? []
    for (const c of batch) byId.set(c.id, c)
    received += batch.length
    if (batch.length === 0 || received >= (result?.total ?? 0)) break
  }
  return [...byId.values()]
}

async function load() {
  const request = ++lastRequest
  try {
    const all = await fetchAllOngoing(request)
    if (all === null) return
    items.value = all.filter((c) => c.goalType === props.goalType)
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
    role="status"
    :aria-labelledby="headingId"
  >
    <h2 :id="headingId"><span aria-hidden="true">🏁 </span>참여 중인 챌린지</h2>
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

/* 좁은 화면에서는 제목을 위 줄에 두고 달성도 바·배지를 아래 줄에 둔다 */
@media (max-width: 480px) {
  li {
    grid-template-columns: minmax(0, 1fr) auto;
    gap: var(--space-1) var(--space-3);
  }

  .title {
    grid-column: 1 / -1;
    white-space: normal;
    overflow-wrap: anywhere;
  }
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
