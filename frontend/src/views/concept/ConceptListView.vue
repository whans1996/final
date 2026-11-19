<script setup>
// SC-09 개념 카드 목록·검색 (F-09 AC9~AC11). 공개 화면. 조건의 정본은 라우트 쿼리이고 쿼리가 바뀔 때마다 다시 조회한다.
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchConcepts } from '@/api/concepts'
import { errorMessage } from '@/api/http'
import ConceptCard from '@/components/concept/ConceptCard.vue'
import ConceptFilterBar from '@/components/concept/ConceptFilterBar.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import PageNav from '@/components/common/PageNav.vue'

const PAGE_SIZE = 20
const DEFAULT_SORT = 'POPULAR'

const route = useRoute()
const router = useRouter()

const items = ref([])
const total = ref(0)
const loading = ref(true)
const loadError = ref('')

const text = (value) => (typeof value === 'string' ? value : '')

/** 라우트 쿼리 → 검색 조건. 없는 값은 기본값(빈 값, 인기순, 1페이지). */
const conditions = computed(() => {
  const q = route.query
  const page = Number.parseInt(text(q.page), 10)
  return {
    keyword: text(q.keyword).trim(),
    category: text(q.category),
    difficulty: text(q.difficulty),
    sort: text(q.sort) || DEFAULT_SORT,
    page: page >= 1 ? page : 1,
  }
})

const filters = computed(() => {
  const { keyword, category, difficulty, sort } = conditions.value
  return { keyword, category, difficulty, sort }
})

/** 검색 조건 → 쿼리. 기본값은 넣지 않는다 (AC10). */
function toQuery({ keyword, category, difficulty, sort, page }) {
  const query = {}
  if (keyword) query.keyword = keyword
  if (category) query.category = category
  if (difficulty) query.difficulty = difficulty
  if (sort && sort !== DEFAULT_SORT) query.sort = sort
  if (page > 1) query.page = String(page)
  return query
}

// 조건을 빠르게 바꿀 때 늦게 도착한 이전 요청의 응답은 버린다
let lastRequest = 0

async function load() {
  const request = ++lastRequest
  loading.value = true
  loadError.value = ''
  try {
    const result = await fetchConcepts({ ...conditions.value, size: PAGE_SIZE })
    if (request !== lastRequest) return
    items.value = result.items
    total.value = result.total
  } catch (error) {
    if (request !== lastRequest) return
    loadError.value = errorMessage(error)
  } finally {
    if (request === lastRequest) loading.value = false
  }
}

// 다른 화면(/concepts/:id 등)으로 떠나는 순간의 쿼리 변화로는 조회하지 않는다
watch(
  () => route.query,
  () => {
    if (route.name === 'concepts') load()
  },
  { immediate: true },
)

function onFilterChange(next) {
  router.push({ query: toQuery({ ...next, page: 1 }) })
}

function onPageChange(page) {
  router.push({ query: toQuery({ ...conditions.value, page }) })
}

function resetFilters() {
  router.push({ path: '/concepts' })
}
</script>

<template>
  <section class="list">
    <h1>개념 카드</h1>
    <ConceptFilterBar :filters="filters" @change="onFilterChange" />

    <p v-if="loading" class="hint">불러오는 중…</p>
    <p v-else-if="loadError" class="form-error" role="alert">{{ loadError }}</p>

    <EmptyState v-else-if="items.length === 0" message="조건에 맞는 개념 카드가 없습니다">
      <template #action>
        <button type="button" class="reset" data-action="reset-filters" @click="resetFilters">
          필터 초기화
        </button>
      </template>
    </EmptyState>

    <template v-else>
      <ul class="cards">
        <li v-for="concept in items" :key="concept.id">
          <ConceptCard :concept="concept" />
        </li>
      </ul>
      <PageNav :page="conditions.page" :size="PAGE_SIZE" :total="total" @change="onPageChange" />
    </template>
  </section>
</template>

<style scoped>
.list {
  padding: var(--space-8) 0;
}

h1 {
  margin: 0;
}

.hint {
  margin-top: var(--space-6);
  color: var(--color-text-muted);
}

.form-error {
  margin-top: var(--space-6);
  color: var(--color-danger);
}

.cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(260px, 100%), 1fr));
  gap: var(--space-4);
  margin: var(--space-6) 0 0;
  padding: 0;
  list-style: none;
}

.reset {
  padding: var(--space-2) var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  font: inherit;
  cursor: pointer;
}
</style>
