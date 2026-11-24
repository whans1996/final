<script setup>
// SC-12 챌린지 목록 (F-25 AC12). 누구나 볼 수 있다. 조건의 정본은 라우트 쿼리(?status=&page=)다.
// status가 없으면 진행 중 탭이다. "내 챌린지"(MY)는 로그인했을 때만 있고, 비로그인이 MY로 오면 진행 중 탭으로 본다.
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CHALLENGE_PAGE_SIZE, fetchChallenges, fetchMyChallenges } from '@/api/challenges'
import { errorMessage } from '@/api/http'
import ChallengeCard from '@/components/challenge/ChallengeCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import PageNav from '@/components/common/PageNav.vue'
import { useAuthStore } from '@/stores/auth'
import { overflowLastPage } from '@/utils/pagination'

const DEFAULT_TAB = 'ONGOING'

const STATUS_TABS = [
  { value: 'ONGOING', label: '진행 중', empty: '진행 중인 챌린지가 없습니다.' },
  { value: 'UPCOMING', label: '예정', empty: '예정된 챌린지가 없습니다.' },
  { value: 'ENDED', label: '종료', empty: '종료된 챌린지가 없습니다.' },
]
const MY_TAB = { value: 'MY', label: '내 챌린지', empty: '참여한 챌린지가 없습니다.' }

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const items = ref([])
const total = ref(0)
const loading = ref(true)
const loadError = ref('')

const tabs = computed(() => (auth.isLoggedIn ? [...STATUS_TABS, MY_TAB] : STATUS_TABS))

const text = (value) => (typeof value === 'string' ? value : '')

/** 라우트 쿼리 → 조건. 모르는 값(비로그인의 MY 포함)은 기본값으로 본다. */
const conditions = computed(() => {
  const status = text(route.query.status)
  const page = Number.parseInt(text(route.query.page), 10)
  return {
    tab: tabs.value.some((t) => t.value === status) ? status : DEFAULT_TAB,
    page: page >= 1 ? page : 1,
  }
})

const current = computed(() => tabs.value.find((t) => t.value === conditions.value.tab))

/** 조건 → 쿼리. 기본값(진행 중, 1페이지)은 넣지 않는다. */
function toQuery({ tab, page }) {
  const query = {}
  if (tab !== DEFAULT_TAB) query.status = tab
  if (page > 1) query.page = String(page)
  return query
}

let lastRequest = 0
// 화면이 사라지면 남은 요청을 무효로 한다. 늦은 응답이 범위 보정 replace로 다음 화면을 바꾸지 않게
onBeforeUnmount(() => {
  lastRequest++
})

async function load() {
  const request = ++lastRequest
  const { tab, page } = conditions.value
  let redirecting = false
  loading.value = true
  loadError.value = ''
  try {
    const query = { page, size: CHALLENGE_PAGE_SIZE }
    const result =
      tab === MY_TAB.value
        ? await fetchMyChallenges(query)
        : await fetchChallenges({ status: tab, ...query })
    if (request !== lastRequest) return
    const lastPage = overflowLastPage({
      page,
      size: CHALLENGE_PAGE_SIZE,
      total: result.total,
      itemCount: result.items.length,
    })
    if (lastPage && route.name === 'challenge-list') {
      redirecting = true
      router.replace({
        name: 'challenge-list',
        query: toQuery({ ...conditions.value, page: lastPage }),
      })
      return
    }
    items.value = result.items
    total.value = result.total
  } catch (error) {
    if (request !== lastRequest) return
    loadError.value = errorMessage(error)
  } finally {
    if (request === lastRequest && !redirecting) loading.value = false
  }
}

// 상세로 떠나는 순간의 쿼리 변화로는 조회하지 않는다. 로그아웃하면 MY 탭이 사라지므로 다시 불러온다
watch(
  [() => route.query, () => auth.isLoggedIn],
  () => {
    if (route.name === 'challenge-list') load()
  },
  { immediate: true },
)

function go(next) {
  router.push({ name: 'challenge-list', query: toQuery({ ...conditions.value, ...next }) })
}

const selectTab = (tab) => go({ tab, page: 1 })
const changePage = (page) => go({ page })
</script>

<template>
  <section class="challenges">
    <h1>챌린지</h1>

    <div class="tabs" role="tablist" aria-label="챌린지 상태">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        type="button"
        role="tab"
        :data-status-tab="tab.value"
        :aria-selected="conditions.tab === tab.value ? 'true' : 'false'"
        :class="{ active: conditions.tab === tab.value }"
        @click="selectTab(tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <p v-if="loading" class="hint">불러오는 중…</p>
    <div v-else-if="loadError" class="error" role="alert">
      <p>{{ loadError }}</p>
      <button type="button" class="retry" data-action="retry" @click="load()">다시 시도</button>
    </div>
    <EmptyState v-else-if="items.length === 0" :message="current.empty">
      <template v-if="conditions.tab === 'MY'" #action>
        <button type="button" class="retry" data-action="browse" @click="selectTab(DEFAULT_TAB)">
          진행 중 챌린지 보기
        </button>
      </template>
    </EmptyState>
    <template v-else>
      <ul class="grid">
        <li v-for="challenge in items" :key="challenge.id">
          <ChallengeCard :challenge="challenge" />
        </li>
      </ul>
      <PageNav
        :page="conditions.page"
        :size="CHALLENGE_PAGE_SIZE"
        :total="total"
        @change="changePage"
      />
    </template>
  </section>
</template>

<style scoped>
.challenges {
  max-width: 1080px;
  margin: 0 auto;
  padding: var(--space-8) 0;
}

h1 {
  margin: 0;
}

.tabs {
  display: flex;
  gap: var(--space-1);
  margin-top: var(--space-4);
  border-bottom: 1px solid var(--color-border);
  overflow-x: auto;
}

.tabs button {
  padding: var(--space-2) var(--space-3);
  border: 0;
  border-bottom: 2px solid transparent;
  background: none;
  font: inherit;
  color: var(--color-text-muted);
  cursor: pointer;
  white-space: nowrap;
}

.tabs button.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
  font-weight: 700;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: var(--space-4);
  margin: var(--space-6) 0 0;
  padding: 0;
  list-style: none;
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

.retry {
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  font: inherit;
  cursor: pointer;
}

@media (max-width: 480px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
