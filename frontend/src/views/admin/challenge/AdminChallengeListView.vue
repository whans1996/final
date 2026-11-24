<script setup>
// SC-21 관리자 챌린지 목록 (F-24 AC11·AC12). 조건의 정본은 라우트 쿼리(?status=&page=)다. status가 없으면 전체 탭이다.
// 삭제는 브라우저 confirm 대신 화면 안 대화상자로 참여 기록이 함께 지워진다는 것을 알린다.
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { deleteAdminChallenge, fetchAdminChallenges } from '@/api/adminChallenges'
import { errorMessage } from '@/api/http'
import AdminChallengeStatusTabs from '@/components/challenge/admin/AdminChallengeStatusTabs.vue'
import AdminChallengeTable from '@/components/challenge/admin/AdminChallengeTable.vue'
import { CHALLENGE_STATUSES } from '@/components/challenge/labels'
import EmptyState from '@/components/common/EmptyState.vue'
import PageNav from '@/components/common/PageNav.vue'
import { overflowLastPage } from '@/utils/pagination'

const PAGE_SIZE = 20
const STATUSES = CHALLENGE_STATUSES.map((s) => s.value)

const EMPTY_MESSAGES = {
  ALL: '등록된 챌린지가 없습니다.',
  UPCOMING: '예정된 챌린지가 없습니다.',
  ONGOING: '진행 중인 챌린지가 없습니다.',
  ENDED: '종료된 챌린지가 없습니다.',
}

const route = useRoute()
const router = useRouter()

const items = ref([])
const total = ref(0)
const loading = ref(true)
const loadError = ref('')
// 편집 화면이 저장 후 history state로 넘긴 안내, 삭제 성공 안내
const notice = ref('')
const actionError = ref('')
// 삭제 확인 대화상자에 띄운 챌린지
const pendingDelete = ref(null)
const deleting = ref(false)
// 대화상자가 열리면 되돌릴 수 없는 삭제 대신 취소에 포커스를 둔다
const cancelButton = ref(null)

const text = (value) => (typeof value === 'string' ? value : '')

/** 라우트 쿼리 → 조건. 모르는 값은 기본값으로 본다. */
const conditions = computed(() => {
  const status = text(route.query.status)
  const page = Number.parseInt(text(route.query.page), 10)
  return {
    tab: STATUSES.includes(status) ? status : 'ALL',
    page: page >= 1 ? page : 1,
  }
})

/** 조건 → 쿼리. 기본값(전체 탭, 1페이지)은 넣지 않는다. */
function toQuery({ tab, page }) {
  const query = {}
  if (tab !== 'ALL') query.status = tab
  if (page > 1) query.page = String(page)
  return query
}

let lastRequest = 0
// 화면이 사라지면 남은 요청을 무효로 한다. 늦은 응답이 범위 보정 replace로 다음 화면을 바꾸지 않게
onBeforeUnmount(() => {
  lastRequest++
})

// 편집 화면이 history state로 넘긴 성공 안내를 꺼내고 그 항목에서 지운다 (새로고침 때 다시 뜨지 않게)
function takeNotice() {
  const history = router.options.history
  const message = history.state?.notice
  if (!message) return ''
  history.replace(history.location, { ...history.state, notice: null })
  return message
}

async function load({ keepNotice = false } = {}) {
  const request = ++lastRequest
  const { tab, page } = conditions.value
  let redirecting = false
  loading.value = true
  loadError.value = ''
  if (!keepNotice) notice.value = takeNotice()
  try {
    const result = await fetchAdminChallenges({
      status: tab === 'ALL' ? '' : tab,
      page,
      size: PAGE_SIZE,
    })
    if (request !== lastRequest) return
    // 마지막 페이지의 챌린지를 지우고 page가 범위를 넘으면 마지막 page로 주소를 바꾼다
    const lastPage = overflowLastPage({
      page,
      size: PAGE_SIZE,
      total: result.total,
      itemCount: result.items.length,
    })
    if (lastPage && route.name === 'admin-challenges') {
      redirecting = true
      router.replace({
        name: 'admin-challenges',
        query: toQuery({ ...conditions.value, page: lastPage }),
        state: notice.value ? { notice: notice.value } : undefined,
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

// 편집 화면으로 떠나는 순간의 쿼리 변화로는 조회하지 않는다
watch(
  () => route.query,
  () => {
    if (route.name === 'admin-challenges') load()
  },
  { immediate: true },
)

function go(next) {
  router.push({ name: 'admin-challenges', query: toQuery({ ...conditions.value, ...next }) })
}

const selectTab = (tab) => go({ tab, page: 1 })
const changePage = (page) => go({ page })

async function askDelete(challenge) {
  actionError.value = ''
  pendingDelete.value = challenge
  await nextTick()
  cancelButton.value?.focus()
}

function cancelDelete() {
  if (!deleting.value) pendingDelete.value = null
}

async function confirmDelete() {
  if (deleting.value) return
  const target = pendingDelete.value
  deleting.value = true
  notice.value = ''
  try {
    await deleteAdminChallenge(target.id)
    notice.value = `"${target.title}" 챌린지를 삭제했습니다.`
    pendingDelete.value = null
    await load({ keepNotice: true })
  } catch (error) {
    pendingDelete.value = null
    actionError.value = errorMessage(error)
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <section class="admin-challenges">
    <div class="heading">
      <h1>챌린지</h1>
      <RouterLink :to="{ name: 'admin-challenge-new' }" class="new" data-action="new">
        새 챌린지
      </RouterLink>
    </div>

    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <p v-if="actionError" class="action-error" role="alert" data-testid="action-error">
      {{ actionError }}
    </p>

    <AdminChallengeStatusTabs :model-value="conditions.tab" @update:model-value="selectTab" />

    <p v-if="loading" class="hint">불러오는 중…</p>
    <div v-else-if="loadError" class="error" role="alert">
      <p>{{ loadError }}</p>
      <button type="button" data-action="retry" @click="load()">다시 시도</button>
    </div>
    <EmptyState v-else-if="items.length === 0" :message="EMPTY_MESSAGES[conditions.tab]">
      <template #action>
        <RouterLink :to="{ name: 'admin-challenge-new' }" class="new" data-action="new">
          챌린지 등록
        </RouterLink>
      </template>
    </EmptyState>
    <template v-else>
      <AdminChallengeTable :items="items" @delete="askDelete" />
      <PageNav :page="conditions.page" :size="PAGE_SIZE" :total="total" @change="changePage" />
    </template>

    <div v-if="pendingDelete" class="backdrop" @click.self="cancelDelete">
      <div
        class="dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="delete-dialog-title"
        @keydown.esc="cancelDelete"
      >
        <h2 id="delete-dialog-title">챌린지를 삭제할까요?</h2>
        <p class="target">{{ pendingDelete.title }}</p>
        <p>
          참여자 {{ pendingDelete.participantCount }}명의 참여 기록도 함께 지워집니다. 되돌릴 수
          없습니다.
        </p>
        <div class="dialog-actions">
          <button
            ref="cancelButton"
            type="button"
            data-action="cancel-delete"
            :disabled="deleting"
            @click="cancelDelete"
          >
            취소
          </button>
          <button
            type="button"
            class="danger"
            data-action="confirm-delete"
            :disabled="deleting"
            @click="confirmDelete"
          >
            삭제
          </button>
        </div>
      </div>
    </div>
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

.notice,
.action-error {
  margin: var(--space-4) 0 0;
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius);
}

.notice {
  background: #ecfdf5;
  color: #047857;
}

.action-error {
  background: #fef2f2;
  color: var(--color-danger);
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

button {
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  font: inherit;
  cursor: pointer;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.backdrop {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-4);
  background: rgb(0 0 0 / 40%);
}

.dialog {
  width: min(420px, 100%);
  padding: var(--space-6);
  border-radius: var(--radius);
  background: var(--color-surface);
}

.dialog h2 {
  margin: 0 0 var(--space-3);
  font-size: 1.15em;
}

.dialog p {
  margin: 0 0 var(--space-3);
}

.dialog .target {
  font-weight: 700;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
  margin-top: var(--space-4);
}

.dialog-actions .danger {
  color: #fff;
  background: var(--color-danger);
  border-color: var(--color-danger);
  font-weight: 700;
}
</style>
