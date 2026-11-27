<script setup>
// SC-20 관리자 개념 카드 목록 (F-13 AC13). 조건의 정본은 라우트 쿼리(?status=&source=&keyword=&page=)다.
// status가 없으면 검수 대기(DRAFT) 탭이고, 전체 탭은 status=ALL로 표시해 조회 때는 status를 빼고 보낸다.
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { fetchAdminConceptCounts, fetchAdminConcepts } from '@/api/adminConcepts'
import { errorMessage } from '@/api/http'
import AdminConceptStatusTabs from '@/components/concept/admin/AdminConceptStatusTabs.vue'
import AdminConceptTable from '@/components/concept/admin/AdminConceptTable.vue'
import { CONCEPT_SOURCES } from '@/components/concept/admin/labels'
import EmptyState from '@/components/common/EmptyState.vue'
import PageNav from '@/components/common/PageNav.vue'
import { overflowLastPage } from '@/utils/pagination'
import { useLatestRequest } from '@/composables/useLatestRequest'

const PAGE_SIZE = 20
const DEFAULT_TAB = 'DRAFT'
const TABS = ['DRAFT', 'PUBLISHED', 'HIDDEN', 'ALL']
const SOURCES = CONCEPT_SOURCES.map((s) => s.value)

const EMPTY_MESSAGES = {
  DRAFT: '검수할 초안이 없습니다.',
  PUBLISHED: '공개된 카드가 없습니다.',
  HIDDEN: '숨긴 카드가 없습니다.',
  ALL: '개념 카드가 없습니다.',
}

const route = useRoute()
const router = useRouter()

const items = ref([])
const total = ref(0)
const counts = ref(null)
const loading = ref(true)
const loadError = ref('')
// 편집 화면이 저장·삭제 후 history state로 넘긴 안내 (AC14·AC15)
const notice = ref('')

const text = (value) => (typeof value === 'string' ? value : '')

/** 라우트 쿼리 → 조건. 모르는 값은 기본값으로 본다. */
const conditions = computed(() => {
  const q = route.query
  const tab = text(q.status)
  const source = text(q.source)
  const page = Number.parseInt(text(q.page), 10)
  return {
    tab: TABS.includes(tab) ? tab : DEFAULT_TAB,
    source: SOURCES.includes(source) ? source : '',
    keyword: text(q.keyword).trim(),
    page: page >= 1 ? page : 1,
  }
})

/** 조건 → 쿼리. 기본값(검수 대기 탭, 1페이지, 빈 값)은 넣지 않는다. */
function toQuery({ tab, source, keyword, page }) {
  const query = {}
  if (tab !== DEFAULT_TAB) query.status = tab
  if (source) query.source = source
  if (keyword) query.keyword = keyword
  if (page > 1) query.page = String(page)
  return query
}

const form = reactive({ source: '', keyword: '' })

// 뒤로 가기 등으로 쿼리가 바뀌면 입력란도 따라간다
watch(conditions, ({ source, keyword }) => Object.assign(form, { source, keyword }), {
  immediate: true,
})

const emptyMessage = computed(() => {
  const { tab, source, keyword } = conditions.value
  return source || keyword ? '조건에 맞는 카드가 없습니다.' : EMPTY_MESSAGES[tab]
})

// 조건을 빠르게 바꾸면 이전 요청을 취소하고 늦게 온 응답은 버린다. 화면이 사라지면 남은 요청을 무효로 해
// 늦은 응답이 범위 보정 replace로 다음 화면을 바꾸지 않게 한다 (Codex 리뷰, V-4)
const latest = useLatestRequest()

// 편집 화면이 history state로 넘긴 성공 안내를 꺼내고 그 항목에서 지운다.
// 브라우저는 새로고침해도 history.state를 유지하므로 지우지 않으면 F5마다 다시 뜬다.
function takeNotice() {
  const history = router.options.history
  const message = history.state?.notice
  if (!message) return ''
  history.replace(history.location, { ...history.state, notice: null })
  return message
}

async function load() {
  const request = latest.begin()
  const { tab, source, keyword, page } = conditions.value
  let redirecting = false
  loading.value = true
  loadError.value = ''
  notice.value = takeNotice()
  try {
    const result = await fetchAdminConcepts(
      {
        status: tab === 'ALL' ? '' : tab,
        source,
        keyword,
        page,
        size: PAGE_SIZE,
      },
      { signal: request.signal },
    )
    if (!request.isCurrent()) return
    // 마지막 페이지의 카드를 처리하고 돌아와 page가 범위를 넘으면 마지막 page로 주소를 바꾼다.
    // 성공 안내는 바꾼 history 항목의 state로 넘겨 다음 조회의 takeNotice()가 다시 꺼내게 한다 (#76)
    const lastPage = overflowLastPage({
      page,
      size: PAGE_SIZE,
      total: result.total,
      itemCount: result.items.length,
    })
    // 응답 전에 다른 화면으로 떠났으면 주소를 바꾸지 않는다 (늦게 온 응답이 탐색을 일으키지 않게)
    if (lastPage && route.name === 'admin-concepts') {
      redirecting = true
      router.replace({
        name: 'admin-concepts',
        query: toQuery({ ...conditions.value, page: lastPage }),
        state: notice.value ? { notice: notice.value } : undefined,
      })
      return
    }
    items.value = result.items
    total.value = result.total
  } catch (error) {
    if (!request.isCurrent()) return
    loadError.value = errorMessage(error)
  } finally {
    if (request.isCurrent() && !redirecting) loading.value = false
  }
}

async function loadCounts() {
  try {
    counts.value = await fetchAdminConceptCounts()
  } catch {
    // 개수는 보조 정보라 실패하면 탭 이름만 보인다
    counts.value = null
  }
}

// 편집 화면으로 떠나는 순간의 쿼리 변화로는 조회하지 않는다
watch(
  () => route.query,
  () => {
    if (route.name === 'admin-concepts') load()
  },
  { immediate: true },
)

onMounted(loadCounts)

function go(next) {
  router.push({ name: 'admin-concepts', query: toQuery({ ...conditions.value, ...next }) })
}

const selectTab = (tab) => go({ tab, page: 1 })
const applyFilters = () => go({ source: form.source, keyword: form.keyword.trim(), page: 1 })
const changePage = (page) => go({ page })
</script>

<template>
  <section class="admin-concepts">
    <div class="heading">
      <h1>개념 카드</h1>
      <RouterLink :to="{ name: 'admin-concept-new' }" class="new" data-action="new">
        새 카드
      </RouterLink>
    </div>

    <p v-if="notice" class="notice" role="status">{{ notice }}</p>

    <AdminConceptStatusTabs
      :model-value="conditions.tab"
      :counts="counts"
      @update:model-value="selectTab"
    />

    <form class="filters" role="search" @submit.prevent="applyFilters">
      <select v-model="form.source" name="source" aria-label="출처" @change="applyFilters">
        <option value="">전체 출처</option>
        <option v-for="s in CONCEPT_SOURCES" :key="s.value" :value="s.value">{{ s.label }}</option>
      </select>
      <div class="search">
        <input
          v-model="form.keyword"
          name="keyword"
          type="search"
          maxlength="50"
          placeholder="이름이나 요약으로 검색"
          aria-label="검색어"
        />
        <button type="submit">검색</button>
      </div>
    </form>

    <p v-if="loading" class="hint">불러오는 중…</p>
    <div v-else-if="loadError" class="error" role="alert">
      <p>{{ loadError }}</p>
      <button type="button" data-action="retry" @click="load">다시 시도</button>
    </div>
    <EmptyState v-else-if="items.length === 0" :message="emptyMessage" />
    <template v-else>
      <AdminConceptTable :items="items" />
      <PageNav :page="conditions.page" :size="PAGE_SIZE" :total="total" @change="changePage" />
    </template>
  </section>
</template>

<style scoped>
.heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

h1 {
  margin: 0;
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

.notice {
  margin: var(--space-4) 0 0;
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius);
  background: #ecfdf5;
  color: #047857;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-4);
}

.search {
  display: flex;
  flex: 1 1 240px;
  gap: var(--space-2);
  min-width: 0;
}

.search input {
  flex: 1;
  min-width: 0;
}

input,
select,
button {
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  font: inherit;
}

.search button {
  color: #fff;
  background: var(--color-primary);
  border-color: var(--color-primary);
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}

.hint {
  margin-top: var(--space-6);
  color: var(--color-text-muted);
}

.error {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-top: var(--space-6);
  color: var(--color-danger);
}

.error p {
  margin: 0;
}

.error button {
  cursor: pointer;
}
</style>
