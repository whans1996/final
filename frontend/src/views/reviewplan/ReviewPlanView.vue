<script setup>
// SC-11 복습 계획 (F-15 AC9·AC10). 오늘부터 7일의 복습 카드와 밀린 복습을 보고 완료를 체크한다.
// 기간 이동은 서버가 준 today·from을 기준으로 ±7일을 계산한다 (브라우저 시계를 쓰지 않는다).
// 빈 상태 아래에는 맞춤 추천 3개를 보여 준다 (F-16 AC10). 체크에 성공하면 진행 중인 복습 챌린지 달성도를 위에 보인다 (#211).
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { deleteReviewPlan, fetchReviewPlans, updateReviewPlanStatus } from '@/api/reviewPlans'
import { errorMessage } from '@/api/http'
import ChallengeProgressNotice from '@/components/challenge/ChallengeProgressNotice.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ReviewPlanItem from '@/components/reviewplan/ReviewPlanItem.vue'
import RecommendationList from '@/components/recommendation/RecommendationList.vue'

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']
const PERIOD_DAYS = 7

const data = ref(null)
const loading = ref(true)
const loadError = ref('')

/** 계획 id → 요청 중 표시할 체크 값 */
const pendingChecked = ref({})
/** 계획 id → 요청 중 여부 */
const busy = ref({})
/** 계획 id → 항목 아래 에러 */
const itemErrors = ref({})
/** 체크에 성공할 때마다 올린다. 0이면 챌린지 달성도를 숨긴다 (#211) */
const statusChanges = ref(0)

function toUtc(date) {
  const [y, m, d] = date.split('-').map(Number)
  return new Date(Date.UTC(y, m - 1, d))
}

function addDays(date, days) {
  const value = toUtc(date)
  value.setUTCDate(value.getUTCDate() + days)
  return value.toISOString().slice(0, 10)
}

/** '2026-11-20' → '11월 20일' */
function monthDay(date) {
  const value = toUtc(date)
  return `${value.getUTCMonth() + 1}월 ${value.getUTCDate()}일`
}

/** '2026-11-20' → '11월 20일 (금)' */
function dayLabel(date) {
  return `${monthDay(date)} (${WEEKDAYS[toUtc(date).getUTCDay()]})`
}

const periodLabel = computed(() =>
  data.value ? `${monthDay(data.value.from)} ~ ${monthDay(data.value.to)}` : '',
)

/** 밀린 복습은 기간이 오늘부터 시작할 때만 보인다 (AC9) */
const overdue = computed(() =>
  data.value && data.value.from === data.value.today ? data.value.overdue : [],
)

const groups = computed(() => {
  const byDate = new Map()
  for (const item of data.value?.items ?? []) {
    if (!byDate.has(item.planDate)) byDate.set(item.planDate, [])
    byDate.get(item.planDate).push(item)
  }
  return [...byDate].map(([date, items]) => ({ date, items }))
})

const isEmpty = computed(() => overdue.value.length === 0 && groups.value.length === 0)

/** 빈 상태가 보이는 중인지 (불러오기·에러가 아닐 때) */
const showingEmpty = computed(
  () => !loading.value && !loadError.value && !!data.value && isEmpty.value,
)

// 추천은 처음 빈 상태가 될 때 한 번만 부른다. 이후에는 숨기기만 해서 기간을 옮겨도 다시 부르지 않는다 (F-16 AC10)
const recommendationsStarted = ref(false)
watch(showingEmpty, (empty) => {
  if (empty) recommendationsStarted.value = true
})

// 다시 시도가 실패한 그 기간을 다시 부르도록 마지막 요청 기간을 기억한다 (undefined = 오늘부터)
let lastPeriod

async function load(period) {
  lastPeriod = period
  loading.value = true
  loadError.value = ''
  try {
    data.value = await fetchReviewPlans(period)
    itemErrors.value = {}
  } catch (error) {
    loadError.value = errorMessage(error)
  } finally {
    loading.value = false
  }
}

function move(days) {
  const from = addDays(data.value.from, days)
  load({ from, to: addDays(from, PERIOD_DAYS - 1) })
}

function checkedOf(item) {
  return pendingChecked.value[item.id] ?? item.status === 'DONE'
}

function replace(updated) {
  const items = data.value.items.map((item) => (item.id === updated.id ? updated : item))
  const overdueItems = data.value.overdue
    .map((item) => (item.id === updated.id ? updated : item))
    .filter((item) => item.status === 'TODO')
  data.value = { ...data.value, items, overdue: overdueItems }
}

function removeFromLists(id) {
  data.value = {
    ...data.value,
    items: data.value.items.filter((item) => item.id !== id),
    overdue: data.value.overdue.filter((item) => item.id !== id),
  }
}

async function run(item, request, onSuccess) {
  busy.value = { ...busy.value, [item.id]: true }
  itemErrors.value = { ...itemErrors.value, [item.id]: '' }
  try {
    onSuccess(await request())
  } catch (error) {
    itemErrors.value = { ...itemErrors.value, [item.id]: errorMessage(error) }
  } finally {
    busy.value = { ...busy.value, [item.id]: false }
  }
}

async function toggle(item, checked) {
  pendingChecked.value = { ...pendingChecked.value, [item.id]: checked }
  await run(
    item,
    () => updateReviewPlanStatus(item.id, checked ? 'DONE' : 'TODO'),
    (updated) => {
      replace(updated)
      statusChanges.value += 1
    },
  )
  const rest = { ...pendingChecked.value }
  delete rest[item.id]
  pendingChecked.value = rest
}

function remove(item) {
  return run(
    item,
    () => deleteReviewPlan(item.id),
    () => removeFromLists(item.id),
  )
}

load()
</script>

<template>
  <section class="review-plans">
    <h1>복습 계획</h1>

    <div v-if="data" class="period">
      <strong data-message="period">{{ periodLabel }}</strong>
      <div class="period-nav">
        <button type="button" data-action="prev" :disabled="loading" @click="move(-PERIOD_DAYS)">
          ◀ 이전 7일
        </button>
        <button type="button" data-action="today" :disabled="loading" @click="load()">오늘</button>
        <button type="button" data-action="next" :disabled="loading" @click="move(PERIOD_DAYS)">
          다음 7일 ▶
        </button>
      </div>
    </div>

    <ChallengeProgressNotice
      v-if="statusChanges > 0"
      class="challenges"
      goal-type="REVIEW_COUNT"
      :refresh-key="statusChanges"
    />

    <p v-if="loading" class="hint">불러오는 중…</p>

    <div v-else-if="loadError" class="error-box">
      <p class="form-error" role="alert">{{ loadError }}</p>
      <button
        type="button"
        class="outline-button"
        data-action="retry-load"
        @click="load(lastPeriod)"
      >
        다시 시도
      </button>
    </div>

    <template v-else-if="data">
      <EmptyState v-if="isEmpty" message="이 기간에 복습할 카드가 없습니다.">
        <template #action>
          <RouterLink to="/concepts" class="link-button" data-action="browse-concepts">
            개념 카드 둘러보기
          </RouterLink>
        </template>
      </EmptyState>

      <section v-if="overdue.length > 0" class="day overdue" data-section="overdue">
        <h2>밀린 복습 {{ overdue.length }}개</h2>
        <ul class="plans">
          <ReviewPlanItem
            v-for="item in overdue"
            :key="item.id"
            :item="item"
            :checked="checkedOf(item)"
            :busy="!!busy[item.id]"
            :error="itemErrors[item.id]"
            @toggle="toggle(item, $event)"
            @remove="remove(item)"
          />
        </ul>
      </section>

      <section v-for="group in groups" :key="group.date" class="day" :data-date="group.date">
        <h2>{{ dayLabel(group.date) }}<span v-if="group.date === data.today"> · 오늘</span></h2>
        <ul class="plans">
          <ReviewPlanItem
            v-for="item in group.items"
            :key="item.id"
            :item="item"
            :checked="checkedOf(item)"
            :busy="!!busy[item.id]"
            :error="itemErrors[item.id]"
            @toggle="toggle(item, $event)"
            @remove="remove(item)"
          />
        </ul>
      </section>
    </template>

    <div
      v-if="recommendationsStarted"
      v-show="showingEmpty"
      class="empty-recommendations"
      data-section="empty-recommendations"
    >
      <RecommendationList title="이런 카드부터 시작해 보세요" :size="3" hide-on-empty-or-error />
    </div>
  </section>
</template>

<style scoped>
.review-plans {
  max-width: 760px;
  margin: 0 auto;
}

.challenges {
  margin-bottom: var(--space-4);
}

h1 {
  margin-bottom: var(--space-4);
}

.period {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.period-nav {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.period-nav button,
.outline-button {
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  color: var(--color-text);
  cursor: pointer;
}

.period-nav button:disabled {
  cursor: default;
  opacity: 0.6;
}

.hint {
  color: var(--color-text-muted);
}

.form-error {
  color: var(--color-danger);
}

.day {
  margin-top: var(--space-6);
}

.day h2 {
  margin: 0 0 var(--space-3);
  font-size: 1.05em;
}

.overdue h2 {
  color: var(--color-danger);
}

.plans {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin: 0;
  padding: 0;
}

.empty-recommendations {
  margin-top: var(--space-6);
}

.link-button {
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius);
  background: var(--color-primary);
  color: #fff;
  text-decoration: none;
}
</style>
