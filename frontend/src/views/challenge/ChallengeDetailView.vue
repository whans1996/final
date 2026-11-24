<script setup>
// SC-13 챌린지 상세 (F-25 AC13). 누구나 볼 수 있다. 참여·취소 버튼은 ChallengeJoinPanel이 맡고, 결과로 참여자 수·달성도를 바꾼다.
// D-n은 서울 오늘 기준이다: 진행 중은 종료일까지, 예정은 시작일까지, 종료는 없다.
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { fetchChallenge } from '@/api/challenges'
import { errorCode, errorMessage } from '@/api/http'
import ChallengeJoinPanel from '@/components/challenge/ChallengeJoinPanel.vue'
import { goalIcon, goalLabel, periodLabel, statusLabel } from '@/components/challenge/labels'
import { dDayLabel, todayInSeoul } from '@/utils/seoulDate'

const route = useRoute()

const challenge = ref(null)
const loading = ref(true)
const notFound = ref(false)
const loadError = ref('')

const challengeId = computed(() => Number(route.params.id))

const dDay = computed(() => {
  const c = challenge.value
  if (c?.status === 'ONGOING') return dDayLabel(todayInSeoul(), c.endDate)
  if (c?.status === 'UPCOMING') return dDayLabel(todayInSeoul(), c.startDate)
  return ''
})

// 요청마다 번호를 붙여, 늦게 도착한 이전 요청의 응답은 버린다
let lastRequest = 0

async function load(id) {
  // 상세가 아닌 주소로 나가는 중이면 params.id가 없어 NaN이 된다
  if (!Number.isInteger(id)) return
  const request = ++lastRequest
  loading.value = true
  notFound.value = false
  loadError.value = ''
  challenge.value = null
  try {
    const result = await fetchChallenge(id)
    if (request !== lastRequest) return
    challenge.value = result
  } catch (error) {
    if (request !== lastRequest) return
    if (errorCode(error) === 'CHALLENGE_404') notFound.value = true
    else loadError.value = errorMessage(error)
  } finally {
    if (request === lastRequest) loading.value = false
  }
}

watch(challengeId, load, { immediate: true })

/** 참여·취소 응답(ParticipationResponse)을 반영한다 */
function applyParticipation({ participantCount, participation }) {
  challenge.value = { ...challenge.value, participantCount, participation }
}
</script>

<template>
  <section class="challenge">
    <p v-if="loading" class="hint">불러오는 중…</p>
    <p v-else-if="notFound" class="hint">챌린지를 찾을 수 없습니다.</p>
    <p v-else-if="loadError" class="form-error" role="alert">{{ loadError }}</p>

    <template v-else-if="challenge">
      <img v-if="challenge.imageUrl" class="image" :src="challenge.imageUrl" alt="" />
      <div v-else class="image placeholder" :data-thumb="challenge.goalType" aria-hidden="true">
        {{ goalIcon(challenge.goalType) }}
      </div>

      <header class="head">
        <span class="status" :class="challenge.status.toLowerCase()" data-field="status">{{
          statusLabel(challenge.status)
        }}</span>
        <h1>{{ challenge.title }}</h1>
      </header>

      <dl class="facts">
        <div>
          <dt>기간</dt>
          <dd>
            <span data-field="period">{{
              periodLabel(challenge.startDate, challenge.endDate)
            }}</span>
            <span v-if="dDay" class="dday" data-field="dday">{{ dDay }}</span>
          </dd>
        </div>
        <div>
          <dt>목표</dt>
          <dd data-field="goal">{{ goalLabel(challenge.goalType, challenge.goalValue) }}</dd>
        </div>
        <div>
          <dt>참여</dt>
          <dd data-field="participants">참여자 {{ challenge.participantCount }}명</dd>
        </div>
      </dl>

      <ChallengeJoinPanel :challenge="challenge" @change="applyParticipation" />

      <!-- pre-wrap이라 템플릿 줄바꿈이 보이지 않게 v-text로 넣는다 -->
      <p
        v-if="challenge.description"
        class="description"
        data-field="description"
        v-text="challenge.description"
      />
    </template>
  </section>
</template>

<style scoped>
.challenge {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  max-width: 720px;
  margin: 0 auto;
  padding: var(--space-8) 0;
}

.image {
  display: block;
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: var(--radius);
  object-fit: cover;
}

.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-background);
  font-size: 4em;
}

.head {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-2);
}

h1 {
  margin: 0;
  overflow-wrap: anywhere;
}

.status {
  padding: 2px var(--space-2);
  border-radius: 999px;
  font-size: 0.85em;
  background: var(--color-background);
  color: var(--color-text-muted);
}

.status.upcoming {
  color: #1e40af;
  background: #dbeafe;
}

.status.ongoing {
  color: #047857;
  background: #d1fae5;
}

.facts {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin: 0;
}

.facts div {
  display: flex;
  gap: var(--space-4);
}

.facts dt {
  flex-shrink: 0;
  width: 3em;
  color: var(--color-text-muted);
}

.facts dd {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin: 0;
}

.dday {
  color: var(--color-primary);
  font-weight: 700;
}

.description {
  margin: 0;
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border);
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.hint {
  color: var(--color-text-muted);
}

.form-error {
  color: var(--color-danger);
}
</style>
