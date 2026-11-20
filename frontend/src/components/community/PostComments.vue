<script setup>
// SC-15 "댓글" 절 (F-18 AC11~AC13). 오래된순 목록(최대 200개) + 작성 폼/로그인 링크.
// 작성·수정·삭제가 끝나면 댓글 목록만 다시 받는다 (글을 다시 부르면 조회수가 오른다).
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { createComment, fetchComments } from '@/api/posts'
import { errorCode, errorDetails, errorMessage } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import CommentForm from '@/components/community/CommentForm.vue'
import CommentItem from '@/components/community/CommentItem.vue'

const props = defineProps({
  postId: { type: Number, required: true },
})

const auth = useAuthStore()

const items = ref([])
const total = ref(0)
const loading = ref(true)
const loadError = ref('')

const draft = ref('')
const saving = ref(false)
const fieldError = ref('')
const formError = ref('')
// 수정 중인 댓글 id (한 번에 하나)
const editingId = ref(null)

const truncated = computed(() => total.value > items.value.length)
const loginLink = computed(() => ({
  path: '/login',
  query: { redirect: `/community/${props.postId}` },
}))

const isMine = (comment) => comment.authorId !== null && comment.authorId === auth.user?.id

// 다시 불러오기가 겹치면 마지막 요청의 응답만 쓴다
let lastRequest = 0

async function load() {
  const request = ++lastRequest
  try {
    const result = await fetchComments(props.postId)
    if (request !== lastRequest) return
    items.value = result.items
    total.value = result.total
    loadError.value = ''
  } catch (error) {
    if (request !== lastRequest) return
    loadError.value = errorMessage(error)
  } finally {
    if (request === lastRequest) loading.value = false
  }
}

watch(
  () => props.postId,
  () => {
    items.value = []
    total.value = 0
    loading.value = true
    loadError.value = ''
    editingId.value = null
    load()
  },
  { immediate: true },
)

async function onCreate() {
  saving.value = true
  fieldError.value = ''
  formError.value = ''
  try {
    await createComment(props.postId, { content: draft.value })
    draft.value = ''
    await load()
  } catch (error) {
    if (errorCode(error) === 'COMMON_400') {
      fieldError.value = errorDetails(error).content ?? errorMessage(error)
    } else {
      formError.value = errorMessage(error)
    }
  } finally {
    saving.value = false
  }
}

function onChanged() {
  editingId.value = null
  load()
}
</script>

<template>
  <section class="comments" data-role="comments">
    <h2>댓글{{ loading || loadError ? '' : ` ${total}` }}</h2>

    <div class="write">
      <template v-if="auth.isLoggedIn">
        <CommentForm
          v-model="draft"
          submit-label="등록"
          :saving="saving"
          :field-error="fieldError"
          @submit="onCreate"
        />
        <p v-if="formError" class="form-error" role="alert" data-role="comment-form-error">
          {{ formError }}
        </p>
      </template>
      <RouterLink v-else :to="loginLink" class="login" data-action="login-to-comment">
        로그인하고 댓글 남기기
      </RouterLink>
    </div>

    <p v-if="loading" class="hint">댓글을 불러오는 중…</p>
    <p v-else-if="loadError" class="form-error" role="alert" data-role="comments-error">
      {{ loadError }}
    </p>
    <p v-else-if="items.length === 0" class="hint" data-role="comments-empty">
      첫 댓글을 남겨 주세요
    </p>
    <template v-else>
      <ul class="list">
        <CommentItem
          v-for="comment in items"
          :key="comment.id"
          :comment="comment"
          :post-id="postId"
          :mine="isMine(comment)"
          :editing="editingId === comment.id"
          @edit="editingId = comment.id"
          @cancel="editingId = null"
          @changed="onChanged"
        />
      </ul>
      <p v-if="truncated" class="hint" data-role="comments-truncated">
        댓글이 많아 처음 200개만 보입니다
      </p>
    </template>
  </section>
</template>

<style scoped>
.comments {
  margin-top: var(--space-8);
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border);
}

h2 {
  margin: 0 0 var(--space-4);
  font-size: 1.1em;
}

.write {
  margin-bottom: var(--space-4);
}

.login {
  color: var(--color-primary);
}

.list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.hint {
  color: var(--color-text-muted);
}

.form-error {
  margin: var(--space-2) 0 0;
  color: var(--color-danger);
}
</style>
