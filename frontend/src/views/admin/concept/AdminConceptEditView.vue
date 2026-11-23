<script setup>
// SC-20 관리자 카드 편집 (F-13 AC14·AC15). /admin/concepts/new(새 카드, POST)와 /admin/concepts/:id/edit(PUT)가 같은 화면을 쓴다.
// 버튼은 상태별로 다르다: 새 카드 "초안으로 저장"·"공개" / DRAFT "초안으로 저장"·"공개"·"숨김"·"삭제"
// / PUBLISHED "저장"·"숨김" / HIDDEN "저장"·"공개". 공개·숨김 카드를 DRAFT로 되돌리는 버튼은 없다 (AC8).
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  createAdminConcept,
  deleteAdminConcept,
  fetchAdminConcept,
  updateAdminConcept,
} from '@/api/adminConcepts'
import { errorCode, errorDetails, errorMessage } from '@/api/http'
import AdminConceptForm from '@/components/concept/admin/AdminConceptForm.vue'
import { sourceLabel, statusLabel } from '@/components/concept/admin/labels'

const AI_NOTICE = 'AI가 만든 초안입니다. 사실관계·예제를 확인한 뒤 공개하세요.'
const SEED_NOTICE =
  '시드 카드입니다. 이름은 바꿀 수 없고, 시드 파일이 바뀌면 내용이 파일 기준으로 다시 덮어써집니다.'
const SAVED_NOTICES = {
  DRAFT: '초안으로 저장했습니다.',
  PUBLISHED: '공개했습니다.',
  HIDDEN: '숨겼습니다.',
}

const route = useRoute()
const router = useRouter()

const isNew = route.name === 'admin-concept-new'
const conceptId = isNew ? null : Number(route.params.id)

const EMPTY = {
  name: '',
  category: '',
  difficulty: '',
  summary: '',
  body: '',
  exampleCode: '',
  referenceUrl: '',
}

const form = ref({ ...EMPTY })
const concept = ref(null)
const loading = ref(!isNew)
const loadError = ref('')
const notFound = ref(false)
const saving = ref(false)
const fieldErrors = ref({})
const submitError = ref('')

const status = computed(() => concept.value?.status ?? null)

/** [{ label, status }] 순서대로 버튼이 된다 */
const actions = computed(() => {
  if (isNew) {
    return [
      { label: '초안으로 저장', status: 'DRAFT' },
      { label: '공개', status: 'PUBLISHED' },
    ]
  }
  if (status.value === 'DRAFT') {
    return [
      { label: '초안으로 저장', status: 'DRAFT' },
      { label: '공개', status: 'PUBLISHED' },
      { label: '숨김', status: 'HIDDEN' },
    ]
  }
  if (status.value === 'PUBLISHED') {
    return [
      { label: '저장', status: 'PUBLISHED' },
      { label: '숨김', status: 'HIDDEN' },
    ]
  }
  return [
    { label: '저장', status: 'HIDDEN' },
    { label: '공개', status: 'PUBLISHED' },
  ]
})

const text = (value) => value ?? ''

async function load() {
  loading.value = true
  loadError.value = ''
  notFound.value = false
  try {
    const result = await fetchAdminConcept(conceptId)
    concept.value = result
    form.value = {
      name: result.name,
      category: result.category,
      difficulty: result.difficulty,
      summary: result.summary,
      body: text(result.body),
      exampleCode: text(result.exampleCode),
      referenceUrl: text(result.referenceUrl),
    }
  } catch (error) {
    if (errorCode(error) === 'CONCEPT_404') notFound.value = true
    else loadError.value = errorMessage(error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (!isNew) load()
})

// 빈 선택 필드는 null로 보낸다 (BE도 빈 문자열을 NULL로 저장한다)
const orNull = (value) => (value.trim() === '' ? null : value)

/** 목록에서 들어왔으면 그 목록 조건(탭·필터·페이지)으로, 아니면 기본 목록으로 돌아간다 */
function listLocation() {
  const back = router.options.history.state?.back
  if (typeof back === 'string') {
    const resolved = router.resolve(back)
    if (resolved.name === 'admin-concepts') return { name: 'admin-concepts', query: resolved.query }
  }
  return { name: 'admin-concepts' }
}

const backToList = (notice) => router.push({ ...listLocation(), state: { notice } })

async function save(nextStatus) {
  if (saving.value) return
  saving.value = true
  fieldErrors.value = {}
  submitError.value = ''
  const body = {
    name: form.value.name,
    category: form.value.category,
    difficulty: form.value.difficulty,
    summary: form.value.summary,
    body: orNull(form.value.body),
    exampleCode: orNull(form.value.exampleCode),
    referenceUrl: orNull(form.value.referenceUrl),
    status: nextStatus,
  }
  try {
    if (isNew) await createAdminConcept(body)
    else await updateAdminConcept(conceptId, body)
    const unchanged = !isNew && nextStatus === status.value
    await backToList(unchanged ? '저장했습니다.' : SAVED_NOTICES[nextStatus])
  } catch (error) {
    showSubmitError(error)
  } finally {
    saving.value = false
  }
}

/** 404는 없는 카드 안내로, 폼 칸이 있는 details는 필드 아래에, 나머지는 서버 메시지로 버튼 위에 보인다 */
function showSubmitError(error) {
  if (errorCode(error) === 'CONCEPT_404') {
    notFound.value = true
    return
  }
  const details = errorDetails(error)
  const fields = Object.keys(EMPTY)
  fieldErrors.value = Object.fromEntries(
    Object.entries(details).filter(([key]) => fields.includes(key)),
  )
  const others = Object.entries(details).filter(([key]) => !fields.includes(key))
  if (others.length > 0) {
    const reasons = others.map(([key, reason]) => `${key}: ${reason}`).join(', ')
    submitError.value = `${errorMessage(error)} (${reasons})`
  } else if (Object.keys(details).length === 0) {
    submitError.value = errorMessage(error)
  }
}

async function remove() {
  if (saving.value) return
  const count = concept.value.linkedAnalysisCount
  const message = `이 초안과 분석 ${count}개의 연결을 지웁니다. 다시 추출되지 않게 하려면 숨김을 쓰세요.`
  if (!window.confirm(message)) return
  saving.value = true
  submitError.value = ''
  try {
    await deleteAdminConcept(conceptId)
    await backToList('초안을 삭제했습니다.')
  } catch (error) {
    showSubmitError(error)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="admin-concept-edit">
    <RouterLink :to="{ name: 'admin-concepts' }" class="back">← 목록</RouterLink>
    <h1>{{ isNew ? '새 개념 카드' : '개념 카드 편집' }}</h1>

    <p v-if="loading" class="hint">불러오는 중…</p>
    <div v-else-if="notFound" class="not-found">
      <p>카드를 찾을 수 없습니다.</p>
      <RouterLink :to="{ name: 'admin-concepts' }" data-action="back-to-list">
        목록으로 돌아가기
      </RouterLink>
    </div>
    <div v-else-if="loadError" class="error" role="alert">
      <p>{{ loadError }}</p>
      <button type="button" data-action="retry" @click="load">다시 시도</button>
    </div>

    <form v-else @submit.prevent>
      <dl v-if="concept" class="meta">
        <div>
          <dt>상태</dt>
          <dd>{{ statusLabel(concept.status) }}</dd>
        </div>
        <div>
          <dt>출처</dt>
          <dd>{{ sourceLabel(concept.source) }}</dd>
        </div>
        <div>
          <dt>찜</dt>
          <dd>{{ concept.wishCount }}</dd>
        </div>
        <div>
          <dt>평점</dt>
          <dd>{{ concept.avgRating.toFixed(1) }} ({{ concept.reviewCount }})</dd>
        </div>
      </dl>

      <div v-if="concept?.source === 'AI'" class="banner ai" role="note">
        <p>{{ AI_NOTICE }}</p>
        <p class="linked">연결된 분석 {{ concept.linkedAnalysisCount }}개</p>
      </div>
      <div v-else-if="concept?.source === 'SEED'" class="banner seed" role="note">
        <p>{{ SEED_NOTICE }}</p>
      </div>

      <AdminConceptForm
        v-model="form"
        :errors="fieldErrors"
        :name-readonly="concept?.source === 'SEED'"
      />

      <p v-if="submitError" class="submit-error" role="alert" data-testid="submit-error">
        {{ submitError }}
      </p>

      <div class="actions">
        <button
          v-for="action in actions"
          :key="action.label"
          type="button"
          :class="{ primary: action.status === 'PUBLISHED' }"
          :disabled="saving"
          @click="save(action.status)"
        >
          {{ action.label }}
        </button>
        <button
          v-if="status === 'DRAFT'"
          type="button"
          class="danger"
          :disabled="saving"
          @click="remove"
        >
          삭제
        </button>
      </div>
    </form>
  </section>
</template>

<style scoped>
.back {
  color: var(--color-text-muted);
  text-decoration: none;
}

h1 {
  margin: var(--space-2) 0 var(--space-4);
}

.hint {
  color: var(--color-text-muted);
}

.not-found,
.error {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-3);
}

.error {
  color: var(--color-danger);
}

.not-found p,
.error p {
  margin: 0;
}

form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2) var(--space-6);
  margin: 0;
  color: var(--color-text-muted);
}

.meta div {
  display: flex;
  gap: var(--space-2);
}

.meta dd {
  margin: 0;
  color: var(--color-text);
  font-weight: 600;
}

.banner {
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius);
}

.banner p {
  margin: 0;
}

.banner.ai {
  color: #92400e;
  background: #fef3c7;
}

.banner.seed {
  color: #1e40af;
  background: #dbeafe;
}

.linked {
  margin-top: var(--space-1) !important;
  font-weight: 600;
}

.submit-error {
  margin: 0;
  color: var(--color-danger);
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.actions button {
  padding: var(--space-2) var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  font: inherit;
  cursor: pointer;
}

.actions button.primary {
  color: #fff;
  background: var(--color-primary);
  border-color: var(--color-primary);
  font-weight: 700;
}

.actions button.danger {
  margin-left: auto;
  color: var(--color-danger);
  border-color: var(--color-danger);
}

.actions button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
