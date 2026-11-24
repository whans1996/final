<script setup>
// SC-21 관리자 챌린지 편집 (F-24 AC13). /admin/challenges/new(등록, POST)와 /admin/challenges/:id/edit(PUT)가 같은 화면을 쓴다.
// 클라이언트 검증을 통과해야 요청하고, 서버 400 details는 필드 아래에 보인다. 시작한 챌린지는 목표·시작일 입력을 잠근다.
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  createAdminChallenge,
  fetchAdminChallenge,
  updateAdminChallenge,
} from '@/api/adminChallenges'
import { errorCode, errorDetails, errorMessage } from '@/api/http'
import AdminChallengeForm from '@/components/challenge/admin/AdminChallengeForm.vue'
import { LOCK_MESSAGE, statusLabel } from '@/components/challenge/admin/labels'
import { todayIso, validateChallengeForm } from '@/components/challenge/admin/validation'

const route = useRoute()
const router = useRouter()

// 관리자 레이아웃의 RouterView가 화면을 재사용하므로(편집 A → 편집 B, 편집 → 새 챌린지) 주소에서 매번 다시 읽는다
const EDITOR_ROUTES = ['admin-challenge-new', 'admin-challenge-edit']
const isNew = computed(() => route.name === 'admin-challenge-new')
const challengeId = computed(() => (isNew.value ? null : Number(route.params.id)))

const EMPTY = {
  title: '',
  description: '',
  startDate: '',
  endDate: '',
  goalType: '',
  goalValue: '',
  image: null,
  removeImage: false,
}
const FIELDS = Object.keys(EMPTY).filter((key) => key !== 'removeImage')

const form = ref({ ...EMPTY })
const challenge = ref(null)
const loading = ref(!isNew.value)
const loadError = ref('')
const notFound = ref(false)
const saving = ref(false)
const fieldErrors = ref({})
const submitError = ref('')

/** 진행 중·종료 챌린지는 목표 유형·목표값·시작일을 바꿀 수 없다 (AC7) */
const locked = computed(() => Boolean(challenge.value) && challenge.value.status !== 'UPCOMING')

async function load(id) {
  loading.value = true
  try {
    const result = await fetchAdminChallenge(id)
    if (id !== challengeId.value) return // 그사이 다른 챌린지로 옮겼으면 늦은 응답은 버린다
    challenge.value = result
    form.value = {
      title: result.title,
      description: result.description ?? '',
      startDate: result.startDate,
      endDate: result.endDate,
      goalType: result.goalType,
      goalValue: String(result.goalValue),
      image: null,
      removeImage: false,
    }
  } catch (error) {
    if (id !== challengeId.value) return
    if (errorCode(error) === 'CHALLENGE_404') notFound.value = true
    else loadError.value = errorMessage(error)
  } finally {
    if (id === challengeId.value) loading.value = false
  }
}

/** 주소가 가리키는 챌린지로 화면 상태를 처음부터 다시 맞춘다 */
function reset() {
  form.value = { ...EMPTY }
  challenge.value = null
  loadError.value = ''
  notFound.value = false
  fieldErrors.value = {}
  submitError.value = ''
  loading.value = !isNew.value
  if (!isNew.value) load(challengeId.value)
}

watch(
  () => [route.name, route.params.id],
  () => {
    // 목록 등 다른 화면으로 나갈 때는 다시 불러오지 않는다
    if (EDITOR_ROUTES.includes(route.name)) reset()
  },
  { immediate: true },
)

/** 목록에서 들어왔으면 그 목록 조건(탭·페이지)으로, 아니면 기본 목록으로 돌아간다 */
function listLocation() {
  const back = router.options.history.state?.back
  if (typeof back === 'string') {
    const resolved = router.resolve(back)
    if (resolved.name === 'admin-challenges') {
      return { name: 'admin-challenges', query: resolved.query }
    }
  }
  return { name: 'admin-challenges' }
}

async function save() {
  if (saving.value) return
  submitError.value = ''
  fieldErrors.value = validateChallengeForm(form.value, {
    today: todayIso(),
    originalEndDate: challenge.value?.endDate ?? null,
  })
  if (Object.keys(fieldErrors.value).length > 0) return

  saving.value = true
  const creating = isNew.value
  const body = { ...form.value, title: form.value.title.trim() }
  try {
    if (creating) await createAdminChallenge(body)
    else await updateAdminChallenge(challengeId.value, body)
    await router.push({
      ...listLocation(),
      state: { notice: creating ? '챌린지를 등록했습니다.' : '저장했습니다.' },
    })
  } catch (error) {
    showSubmitError(error)
  } finally {
    saving.value = false
  }
}

/** 404는 없는 챌린지 안내로, 409는 잠금 안내로, 폼 칸이 있는 details는 필드 아래에, 나머지는 서버 메시지로 보인다 */
function showSubmitError(error) {
  const code = errorCode(error)
  if (code === 'CHALLENGE_404') {
    notFound.value = true
    return
  }
  if (code === 'CHALLENGE_001') {
    submitError.value = LOCK_MESSAGE
    return
  }
  const details = errorDetails(error)
  fieldErrors.value = Object.fromEntries(
    Object.entries(details).filter(([key]) => FIELDS.includes(key)),
  )
  const others = Object.entries(details).filter(([key]) => !FIELDS.includes(key))
  if (others.length > 0) {
    const reasons = others.map(([key, reason]) => `${key}: ${reason}`).join(', ')
    submitError.value = `${errorMessage(error)} (${reasons})`
  } else if (Object.keys(details).length === 0) {
    submitError.value = errorMessage(error)
  }
}
</script>

<template>
  <section class="admin-challenge-edit">
    <RouterLink :to="{ name: 'admin-challenges' }" class="back">← 목록</RouterLink>
    <h1>{{ isNew ? '새 챌린지' : '챌린지 수정' }}</h1>

    <p v-if="loading" class="hint">불러오는 중…</p>
    <div v-else-if="notFound" class="not-found">
      <p>챌린지를 찾을 수 없습니다.</p>
      <RouterLink :to="{ name: 'admin-challenges' }" data-action="back-to-list">
        목록으로 돌아가기
      </RouterLink>
    </div>
    <div v-else-if="loadError" class="error" role="alert">
      <p>{{ loadError }}</p>
      <button type="button" data-action="retry" @click="reset">다시 시도</button>
    </div>

    <form v-else novalidate @submit.prevent="save">
      <dl v-if="challenge" class="meta">
        <div>
          <dt>상태</dt>
          <dd>{{ statusLabel(challenge.status) }}</dd>
        </div>
        <div>
          <dt>참여자</dt>
          <dd>{{ challenge.participantCount }}명</dd>
        </div>
      </dl>

      <AdminChallengeForm
        v-model="form"
        :errors="fieldErrors"
        :locked="locked"
        :current-image-url="challenge?.imageUrl ?? null"
      />

      <p v-if="submitError" class="submit-error" role="alert" data-testid="submit-error">
        {{ submitError }}
      </p>

      <div class="actions">
        <RouterLink :to="listLocation()" class="cancel">취소</RouterLink>
        <!-- Enter 제출도 기본 버튼 클릭으로 들어오므로 click에서 저장하고 form 제출은 막는다 -->
        <button
          type="submit"
          class="primary"
          data-action="save"
          :disabled="saving"
          @click.prevent="save"
        >
          {{ saving ? '저장 중…' : isNew ? '등록' : '저장' }}
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
  max-width: 720px;
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

.submit-error {
  margin: 0;
  color: var(--color-danger);
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
}

.actions button,
.actions .cancel {
  padding: var(--space-2) var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  color: inherit;
  font: inherit;
  text-decoration: none;
  cursor: pointer;
}

.actions button.primary {
  color: #fff;
  background: var(--color-primary);
  border-color: var(--color-primary);
  font-weight: 700;
}

.actions button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
