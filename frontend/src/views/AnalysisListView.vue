<script setup>
// SC-06 내 분석 목록 (F-05a AC16). 수정·삭제·공개 설정은 F-19에서 붙는다.
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchAnalyses } from '@/api/analyses'
import { errorMessage } from '@/api/http'
import { LEVELS } from '@/utils/authValidation'
import AnalysisStatusBadge from '@/components/analysis/AnalysisStatusBadge.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import PageNav from '@/components/common/PageNav.vue'

const PAGE_SIZE = 20

const items = ref([])
const page = ref(1)
const total = ref(0)
const loading = ref(true)
const loadError = ref('')

function levelLabel(level) {
  return LEVELS.find((l) => l.value === level)?.label ?? level
}

/** '2026-11-24T14:20:00+09:00' → '2026.11.24' */
function formatDate(value) {
  return value ? value.slice(0, 10).replaceAll('-', '.') : ''
}

// 페이지를 빠르게 넘길 때 늦게 도착한 이전 요청의 응답은 버린다
let lastRequest = 0

async function load(target) {
  const request = ++lastRequest
  loading.value = true
  loadError.value = ''
  try {
    const result = await fetchAnalyses({ page: target, size: PAGE_SIZE })
    if (request !== lastRequest) return
    items.value = result.items
    total.value = result.total
    page.value = result.page
  } catch (error) {
    if (request !== lastRequest) return
    loadError.value = errorMessage(error)
  } finally {
    if (request === lastRequest) loading.value = false
  }
}

onMounted(() => load(1))
</script>

<template>
  <section class="list">
    <div class="heading">
      <h1>내 분석</h1>
      <RouterLink :to="{ name: 'analysis-new' }" class="new">분석 요청</RouterLink>
    </div>

    <p v-if="loading" class="hint">불러오는 중…</p>
    <p v-else-if="loadError" class="form-error" role="alert">{{ loadError }}</p>

    <EmptyState
      v-else-if="items.length === 0"
      message="아직 분석이 없습니다. AI로 만든 프로젝트를 올려 코드를 이해해 보세요."
    >
      <template #action>
        <RouterLink :to="{ name: 'analysis-new' }" class="new" data-action="first-request">
          첫 분석 요청하기
        </RouterLink>
      </template>
    </EmptyState>

    <template v-else>
      <ul class="cards">
        <li v-for="analysis in items" :key="analysis.id">
          <RouterLink
            :to="{ name: 'analysis-detail', params: { id: analysis.id } }"
            class="card"
            :data-analysis-id="analysis.id"
          >
            <strong class="title">{{ analysis.title }}</strong>
            <span class="meta">
              {{ levelLabel(analysis.level) }} · {{ formatDate(analysis.createdAt) }}
            </span>
            <AnalysisStatusBadge :status="analysis.status" :progress="analysis.progress" />
          </RouterLink>
        </li>
      </ul>

      <PageNav :page="page" :size="PAGE_SIZE" :total="total" @change="load" />
    </template>
  </section>
</template>

<style scoped>
.list {
  padding: var(--space-8) 0;
}

.heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.hint {
  margin-top: var(--space-6);
  color: var(--color-text-muted);
}

.form-error {
  margin-top: var(--space-6);
  color: var(--color-danger);
}

.new {
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius);
  color: #fff;
  background: var(--color-primary);
  font-weight: 700;
  text-decoration: none;
  white-space: nowrap;
}

.cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: var(--space-4);
  margin-top: var(--space-6);
  padding: 0;
  list-style: none;
}

.card {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  height: 100%;
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  color: inherit;
  background: var(--color-surface);
  text-decoration: none;
}

.card:hover {
  border-color: var(--color-primary);
}

.title {
  overflow-wrap: anywhere;
}

.meta {
  color: var(--color-text-muted);
  font-size: 0.9em;
}
</style>
