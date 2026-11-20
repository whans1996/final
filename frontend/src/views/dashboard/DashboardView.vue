<script setup>
// SC-04 대시보드 (F-16 AC8). 오늘 복습할 카드·맞춤 추천·최근 분석을 절마다 따로 불러온다 (한 절이 실패해도 나머지는 보인다).
// "오늘"은 복습 계획 응답의 today로 판단한다 (브라우저 시계를 쓰지 않는다).
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchReviewPlans } from '@/api/reviewPlans'
import { fetchAnalyses } from '@/api/analyses'
import { errorMessage } from '@/api/http'
import ConceptCard from '@/components/concept/ConceptCard.vue'
import RecommendationList from '@/components/recommendation/RecommendationList.vue'
import { STATUS_LABELS } from '@/utils/analysisStatus'

const TODAY_LIMIT = 5
const RECENT_SIZE = 3

/** 절 하나의 로딩·에러·데이터 상태 */
function useSection(loader) {
  const data = ref(null)
  const loading = ref(true)
  const error = ref('')
  async function load() {
    loading.value = true
    error.value = ''
    try {
      data.value = await loader()
    } catch (e) {
      error.value = errorMessage(e)
    } finally {
      loading.value = false
    }
  }
  load()
  return { data, loading, error, load }
}

const today = useSection(() => fetchReviewPlans())
const recent = useSection(() => fetchAnalyses({ page: 1, size: RECENT_SIZE }))

/** 밀린 복습 + 오늘 날짜의 TODO */
const todayPlans = computed(() => {
  const data = today.data.value
  if (!data) return []
  const todays = data.items.filter((p) => p.planDate === data.today && p.status === 'TODO')
  return [...data.overdue, ...todays]
})
const todayShown = computed(() => todayPlans.value.slice(0, TODAY_LIMIT))
const todayMore = computed(() => todayPlans.value.length - todayShown.value.length)
const todayTitle = computed(() =>
  today.data.value ? `오늘 복습할 카드 ${todayPlans.value.length}개` : '오늘 복습할 카드',
)

const analyses = computed(() => recent.data.value?.items ?? [])

/** '2026-11-19T15:30:00+09:00' → '11월 19일' (서버가 준 날짜 그대로) */
function monthDay(value) {
  const [, m, d] = value.slice(0, 10).split('-').map(Number)
  return `${m}월 ${d}일`
}

function analysisTitle(a) {
  return a.title?.trim() ? a.title : a.projectName
}
</script>

<template>
  <section class="dashboard">
    <h1>대시보드</h1>

    <div class="row">
      <section class="panel" data-section="today">
        <div class="panel-head">
          <h2>{{ todayTitle }}</h2>
          <RouterLink to="/review-plans" class="more-link">복습 계획 보기</RouterLink>
        </div>
        <p v-if="today.loading.value" class="hint">불러오는 중…</p>
        <div v-else-if="today.error.value">
          <p class="form-error" role="alert">{{ today.error.value }}</p>
          <button
            type="button"
            class="outline-button"
            data-action="retry-today"
            @click="today.load"
          >
            다시 시도
          </button>
        </div>
        <p v-else-if="todayPlans.length === 0" class="hint">오늘 복습할 카드가 없습니다.</p>
        <template v-else>
          <ul class="cards">
            <li v-for="item in todayShown" :key="item.id">
              <ConceptCard :concept="item.concept" />
            </li>
          </ul>
          <p v-if="todayMore > 0" class="hint" data-message="more">외 {{ todayMore }}개</p>
        </template>
      </section>

      <section class="panel" data-section="recent">
        <div class="panel-head">
          <h2>최근 분석</h2>
          <RouterLink to="/analyses" class="more-link">전체 보기</RouterLink>
        </div>
        <p v-if="recent.loading.value" class="hint">불러오는 중…</p>
        <div v-else-if="recent.error.value">
          <p class="form-error" role="alert">{{ recent.error.value }}</p>
          <button
            type="button"
            class="outline-button"
            data-action="retry-recent"
            @click="recent.load"
          >
            다시 시도
          </button>
        </div>
        <div v-else-if="analyses.length === 0" class="empty">
          <p class="hint">아직 분석한 코드가 없습니다.</p>
          <RouterLink to="/analyses/new" class="link-button">분석 요청</RouterLink>
        </div>
        <ul v-else class="analyses">
          <li v-for="a in analyses" :key="a.id">
            <RouterLink :to="`/analyses/${a.id}`" class="analysis" :data-analysis-id="a.id">
              <strong class="title">{{ analysisTitle(a) }}</strong>
              <span class="meta">
                <span class="status" :data-status="a.status">{{
                  STATUS_LABELS[a.status] ?? a.status
                }}</span>
                · {{ monthDay(a.createdAt) }}
              </span>
            </RouterLink>
          </li>
        </ul>
      </section>
    </div>

    <section class="panel" data-section="recommendations">
      <RecommendationList title="맞춤 추천" :size="5" />
    </section>
  </section>
</template>

<style scoped>
.dashboard {
  max-width: 960px;
  margin: 0 auto;
}

h1 {
  margin-bottom: var(--space-4);
}

.row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
}

@media (max-width: 720px) {
  .row {
    grid-template-columns: minmax(0, 1fr);
  }
}

.panel {
  min-width: 0;
  margin-bottom: var(--space-4);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
}

.panel-head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}

.panel h2,
.panel :deep(h2) {
  margin: 0;
  font-size: 1.1em;
}

.panel :deep(.recommendations h2) {
  margin-bottom: var(--space-3);
}

.more-link {
  font-size: 0.9em;
}

.cards,
.analyses {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin: 0;
  padding: 0;
  list-style: none;
}

.analysis {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  color: inherit;
  text-decoration: none;
}

.analysis:hover {
  border-color: var(--color-primary);
}

.title {
  overflow-wrap: anywhere;
}

.meta {
  color: var(--color-text-muted);
  font-size: 0.9em;
}

.status[data-status='FAILED'] {
  color: var(--color-danger);
}

.hint {
  margin: 0;
  color: var(--color-text-muted);
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-3);
}

.form-error {
  margin: 0 0 var(--space-2);
  color: var(--color-danger);
}

.outline-button {
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  color: var(--color-text);
  cursor: pointer;
}

.link-button {
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius);
  background: var(--color-primary);
  color: #fff;
  text-decoration: none;
}
</style>
