<script setup>
// SC-16 글쓰기·수정 (F-17 AC17·AC18). /community/write(새 글, ?conceptId=·?analysisId=로 첨부 미리 채움)와
// /community/:id/edit(내 글 수정)이 같은 화면을 쓴다. 로그인 필요(라우트 meta.requiresAuth).
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createPost, fetchPost, updatePost } from '@/api/posts'
import { fetchAnalysis } from '@/api/analyses'
import { fetchConcept } from '@/api/concepts'
import { errorCode, errorDetails, errorMessage } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import AttachmentPicker from '@/components/community/AttachmentPicker.vue'
import { BOARD_TYPES, DEFAULT_BOARD_TYPE, analysisLabel } from '@/components/community/boardTypes'

const TITLE_MAX = 100
const CONTENT_MAX = 5000
const LEAVE_MESSAGE = '작성 중인 내용이 사라집니다. 이 화면을 나갈까요?'
const PREFILL_FAILED = '첨부할 수 없는 항목이라 빼고 열었습니다.'
const NOT_MINE = '본인이 쓴 글만 수정할 수 있습니다.'
const ATTACHMENT_ERROR_CODES = ['POST_001', 'POST_002']

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const isEdit = route.name === 'post-edit'
const postId = isEdit ? Number(route.params.id) : null

const form = reactive({ boardType: DEFAULT_BOARD_TYPE, title: '', content: '' })
const attachment = ref(null)

const loading = ref(isEdit)
const loadError = ref('')
const notice = ref('')
const saving = ref(false)
const fieldErrors = ref({})
const attachmentError = ref('')
const submitError = ref('')

// 불러오기·미리 채우기를 마친 상태를 기준으로 입력이 바뀌었는지 본다 (이탈 확인)
const snapshot = () =>
  JSON.stringify({
    ...form,
    attachment: attachment.value && [attachment.value.type, attachment.value.id],
  })
const initial = ref(snapshot())
const dirty = computed(() => snapshot() !== initial.value)
let submitted = false

const canSubmit = computed(
  () => form.title.trim() !== '' && form.content.trim() !== '' && !saving.value && !loading.value,
)

// 작성자 본인에게 viewable이 아닌 첨부(삭제한 분석, 숨겨진 카드)는 다시 보내면 POST_001·002가 되므로
// 빼고 연다. 그대로 "저장"하면 첨부가 없어진다 (AC18 "빼고 열었습니다"와 같은 처리)
const attachmentFromPost = (post) => {
  const a = post.analysis
  const c = post.concept
  if (a?.viewable) return { type: 'ANALYSIS', id: a.id, label: analysisLabel(a) }
  if (c?.viewable) return { type: 'CONCEPT', id: c.id, label: c.name }
  if (a || c) notice.value = PREFILL_FAILED
  return null
}

async function loadPost() {
  try {
    const post = await fetchPost(postId)
    if (post.authorId === null || post.authorId !== auth.user?.id) {
      notice.value = NOT_MINE
      await router.replace({ path: `/community/${postId}`, state: { notice: NOT_MINE } })
      return
    }
    form.boardType = post.boardType
    form.title = post.title
    form.content = post.content
    attachment.value = attachmentFromPost(post)
  } catch (error) {
    loadError.value = errorMessage(error)
  } finally {
    loading.value = false
  }
}

const queryId = (value) => {
  const id = Number.parseInt(typeof value === 'string' ? value : '', 10)
  return id >= 1 ? id : null
}

async function prefillAttachment() {
  const conceptId = queryId(route.query.conceptId)
  const analysisId = queryId(route.query.analysisId)
  if (conceptId === null && analysisId === null) {
    if (route.query.conceptId !== undefined || route.query.analysisId !== undefined) {
      notice.value = PREFILL_FAILED
    }
    return
  }
  try {
    if (conceptId !== null) {
      const concept = await fetchConcept(conceptId)
      attachment.value = { type: 'CONCEPT', id: concept.id, label: concept.name }
    } else {
      const analysis = await fetchAnalysis(analysisId)
      if (analysis.status !== 'DONE') throw new Error('not attachable')
      attachment.value = { type: 'ANALYSIS', id: analysis.id, label: analysisLabel(analysis) }
    }
  } catch {
    attachment.value = null
    notice.value = PREFILL_FAILED
  }
}

onMounted(async () => {
  if (isEdit) await loadPost()
  else await prefillAttachment()
  initial.value = snapshot()
})

// 이 화면을 떠나는 이동만 확인한다. 등록·저장 성공 뒤 이동과 남의 글 돌려보내기는 묻지 않는다
const removeLeaveGuard = router.beforeEach((to, from) => {
  if (submitted || !dirty.value) return true
  if (from.name !== 'post-write' && from.name !== 'post-edit') return true
  return window.confirm(LEAVE_MESSAGE)
})

function onBeforeUnload(event) {
  if (submitted || !dirty.value) return
  event.preventDefault()
  event.returnValue = ''
}
window.addEventListener('beforeunload', onBeforeUnload)

onUnmounted(() => {
  removeLeaveGuard()
  window.removeEventListener('beforeunload', onBeforeUnload)
})

async function onSubmit() {
  if (!canSubmit.value) return
  saving.value = true
  fieldErrors.value = {}
  attachmentError.value = ''
  submitError.value = ''
  const body = {
    boardType: form.boardType,
    title: form.title,
    content: form.content,
    analysisId: attachment.value?.type === 'ANALYSIS' ? attachment.value.id : null,
    conceptId: attachment.value?.type === 'CONCEPT' ? attachment.value.id : null,
  }
  try {
    const id = isEdit ? (await updatePost(postId, body), postId) : (await createPost(body)).id
    submitted = true
    await router.push(`/community/${id}`)
  } catch (error) {
    const code = errorCode(error)
    const { conceptId, ...fields } = errorDetails(error)
    if (ATTACHMENT_ERROR_CODES.includes(code)) {
      attachmentError.value = errorMessage(error)
    } else if (conceptId || Object.keys(fields).length > 0) {
      fieldErrors.value = fields
      attachmentError.value = conceptId ?? ''
    } else {
      submitError.value = errorMessage(error)
    }
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="write">
    <h1>{{ isEdit ? '글 수정' : '글쓰기' }}</h1>

    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <p v-if="loading" class="hint">불러오는 중…</p>
    <p v-else-if="loadError" class="form-error" role="alert">{{ loadError }}</p>

    <form v-else novalidate @submit.prevent="onSubmit">
      <fieldset class="boards">
        <legend>게시판</legend>
        <label v-for="board in BOARD_TYPES" :key="board.value">
          <input v-model="form.boardType" type="radio" name="boardType" :value="board.value" />
          {{ board.label }}
        </label>
        <p v-if="fieldErrors.boardType" class="field-error" data-error="boardType">
          {{ fieldErrors.boardType }}
        </p>
      </fieldset>

      <label class="field">
        <span>제목</span>
        <input
          v-model="form.title"
          name="title"
          type="text"
          :maxlength="TITLE_MAX"
          placeholder="제목을 입력하세요"
        />
      </label>
      <p v-if="fieldErrors.title" class="field-error" data-error="title">{{ fieldErrors.title }}</p>

      <label class="field">
        <span>본문</span>
        <textarea
          v-model="form.content"
          name="content"
          rows="12"
          :maxlength="CONTENT_MAX"
          placeholder="이해가 안 되는 코드나 배운 점을 적어 주세요"
        />
      </label>
      <p class="count" data-role="content-count">{{ form.content.length }}/{{ CONTENT_MAX }}</p>
      <p v-if="fieldErrors.content" class="field-error" data-error="content">
        {{ fieldErrors.content }}
      </p>

      <AttachmentPicker v-model="attachment" />
      <p v-if="attachmentError" class="field-error" data-error="attachment">
        {{ attachmentError }}
      </p>

      <p v-if="submitError" class="form-error" role="alert">{{ submitError }}</p>

      <div class="actions">
        <button type="submit" class="submit" :disabled="!canSubmit">
          {{ isEdit ? '저장' : '등록' }}
        </button>
      </div>
    </form>
  </section>
</template>

<style scoped>
.write {
  padding: var(--space-8) 0;
}

h1 {
  margin: 0 0 var(--space-4);
}

form {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.boards {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-4);
  margin: 0;
  padding: 0;
  border: none;
}

.boards legend {
  margin-bottom: var(--space-2);
  font-weight: 600;
}

.field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  font-weight: 600;
}

.field input,
.field textarea {
  width: 100%;
  box-sizing: border-box;
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  font: inherit;
  font-weight: normal;
}

.field textarea {
  resize: vertical;
}

.count {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.85em;
  text-align: right;
}

.notice {
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius);
  background: var(--color-background);
}

.hint {
  color: var(--color-text-muted);
}

.field-error,
.form-error {
  margin: 0;
  color: var(--color-danger);
}

.actions {
  display: flex;
  justify-content: flex-end;
}

.submit {
  padding: var(--space-2) var(--space-6);
  border: none;
  border-radius: var(--radius);
  background: var(--color-primary);
  color: #fff;
  font: inherit;
  cursor: pointer;
}

.submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
