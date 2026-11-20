<script setup>
// SC-14 커뮤니티 글 목록 (F-17 AC13·AC14). 공개 화면. 조건의 정본은 라우트 쿼리이고 쿼리가 바뀔 때마다 다시 조회한다.
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchPosts } from '@/api/posts'
import { errorMessage } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import BoardTabs from '@/components/community/BoardTabs.vue'
import PostRow from '@/components/community/PostRow.vue'
import { isBoardType } from '@/components/community/boardTypes'
import EmptyState from '@/components/common/EmptyState.vue'
import PageNav from '@/components/common/PageNav.vue'

const PAGE_SIZE = 20
const WRITE_PATH = '/community/write'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const items = ref([])
const total = ref(0)
const loading = ref(true)
const loadError = ref('')
const keywordInput = ref('')

const text = (value) => (typeof value === 'string' ? value : '')

/** 라우트 쿼리 → 검색 조건. 없거나 잘못된 값은 기본값(전체, 빈 검색어, 1페이지). */
const conditions = computed(() => {
  const q = route.query
  const page = Number.parseInt(text(q.page), 10)
  const boardType = text(q.boardType)
  return {
    boardType: isBoardType(boardType) ? boardType : '',
    keyword: text(q.keyword).trim(),
    page: page >= 1 ? page : 1,
  }
})

/** 검색 조건 → 쿼리. 기본값은 넣지 않는다. */
function toQuery({ boardType, keyword, page }) {
  const query = {}
  if (boardType) query.boardType = boardType
  if (keyword) query.keyword = keyword
  if (page > 1) query.page = String(page)
  return query
}

// 조건을 빠르게 바꿀 때 늦게 도착한 이전 요청의 응답은 버린다
let lastRequest = 0

async function load() {
  const request = ++lastRequest
  loading.value = true
  loadError.value = ''
  keywordInput.value = conditions.value.keyword
  try {
    const result = await fetchPosts({ ...conditions.value, size: PAGE_SIZE })
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

// 다른 화면(/community/:id 등)으로 떠나는 순간의 쿼리 변화로는 조회하지 않는다
watch(
  () => route.query,
  () => {
    if (route.name === 'community') load()
  },
  { immediate: true },
)

function onBoardChange(boardType) {
  router.push({ query: toQuery({ ...conditions.value, boardType, page: 1 }) })
}

function onSearch() {
  router.push({
    query: toQuery({ ...conditions.value, keyword: keywordInput.value.trim(), page: 1 }),
  })
}

function onPageChange(page) {
  router.push({ query: toQuery({ ...conditions.value, page }) })
}

function resetSearch() {
  router.push({ query: toQuery({ ...conditions.value, keyword: '', page: 1 }) })
}

function goWrite() {
  if (auth.isLoggedIn) router.push(WRITE_PATH)
  else router.push({ path: '/login', query: { redirect: WRITE_PATH } })
}
</script>

<template>
  <section class="list">
    <div class="heading">
      <h1>커뮤니티</h1>
      <button type="button" class="write" data-action="write" @click="goWrite">글쓰기</button>
    </div>

    <BoardTabs :model-value="conditions.boardType" @change="onBoardChange" />

    <form class="search" role="search" @submit.prevent="onSearch">
      <input
        v-model="keywordInput"
        name="keyword"
        type="search"
        maxlength="50"
        placeholder="제목·본문 검색"
        aria-label="게시글 검색어"
      />
      <button type="submit">검색</button>
    </form>

    <p v-if="loading" class="hint">불러오는 중…</p>
    <p v-else-if="loadError" class="form-error" role="alert">{{ loadError }}</p>

    <EmptyState v-else-if="items.length === 0" message="게시글이 없습니다">
      <template v-if="conditions.keyword" #action>
        <button type="button" class="reset" data-action="reset-search" @click="resetSearch">
          검색 초기화
        </button>
      </template>
    </EmptyState>

    <template v-else>
      <ul class="rows">
        <li v-for="post in items" :key="post.id">
          <PostRow :post="post" />
        </li>
      </ul>
      <PageNav :page="conditions.page" :size="PAGE_SIZE" :total="total" @change="onPageChange" />
    </template>
  </section>
</template>

<style scoped>
.list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding: var(--space-8) 0;
}

.heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

h1 {
  margin: 0;
}

.write {
  padding: var(--space-2) var(--space-4);
  border: none;
  border-radius: var(--radius);
  background: var(--color-primary);
  color: #fff;
  font: inherit;
  cursor: pointer;
}

.search {
  display: flex;
  gap: var(--space-2);
}

.search input {
  flex: 1;
  min-width: 0;
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  font: inherit;
}

.search button,
.reset {
  padding: var(--space-2) var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  font: inherit;
  cursor: pointer;
}

.hint {
  color: var(--color-text-muted);
}

.form-error {
  color: var(--color-danger);
}

.rows {
  margin: 0;
  padding: 0;
  list-style: none;
  border-top: 1px solid var(--color-border);
}
</style>
