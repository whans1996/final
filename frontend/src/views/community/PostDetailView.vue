<script setup>
// SC-15 게시글 상세 (F-17 AC15·AC16, 댓글 절 F-18 AC11~AC13). 공개 화면. 열 때마다 서버가 조회수를 1 올린다.
// 본문은 텍스트 보간 + pre-wrap으로 글자 그대로 보여 준다 (v-html·마크다운 금지, spec 5절).
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { deletePost, fetchPost } from '@/api/posts'
import { errorCode, errorMessage } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import PostAttachment from '@/components/community/PostAttachment.vue'
import PostComments from '@/components/community/PostComments.vue'
import { authorName, boardChip, formatDateTime } from '@/components/community/boardTypes'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const post = ref(null)
const loading = ref(true)
const notFound = ref(false)
const loadError = ref('')
const actionError = ref('')
const deleting = ref(false)
// 남의 글 edit에서 돌려보낼 때 PostWriteView가 history state로 넘긴 안내 (AC18)
const notice = ref('')
// 댓글 절이 마지막으로 받은 댓글 수. 화면에서 댓글을 달거나 지우면 상세 응답의 commentCount보다 새롭다 (#210)
const liveCommentCount = ref(null)

const isMine = computed(() => post.value?.authorId != null && post.value.authorId === auth.user?.id)

let lastRequest = 0

// edit 화면이 history state로 넘긴 안내를 꺼내고 그 항목에서 지운다 (#171).
// 브라우저는 새로고침·뒤로 가기에도 history.state를 유지하므로 지우지 않으면 그때마다 다시 뜬다.
function takeNotice() {
  const history = router.options.history
  const message = history.state?.notice
  if (!message) return ''
  history.replace(history.location, { ...history.state, notice: null })
  return message
}

async function load(id) {
  const request = ++lastRequest
  loading.value = true
  notFound.value = false
  loadError.value = ''
  actionError.value = ''
  liveCommentCount.value = null
  notice.value = takeNotice()
  try {
    const result = await fetchPost(id)
    if (request !== lastRequest) return
    post.value = result
  } catch (error) {
    if (request !== lastRequest) return
    post.value = null
    if (errorCode(error) === 'POST_404') notFound.value = true
    else loadError.value = errorMessage(error)
  } finally {
    if (request === lastRequest) loading.value = false
  }
}

watch(
  () => route.params.id,
  (id) => {
    if (route.name === 'post-detail') load(Number(id))
  },
  { immediate: true },
)

// 댓글은 글과 함께 지워진다 (FK CASCADE). 있으면 확인 문구에 개수를 알린다 (#210)
function deleteConfirmMessage() {
  const count = liveCommentCount.value ?? post.value.commentCount ?? 0
  return count > 0
    ? `이 글을 삭제할까요?\n댓글 ${count}개도 함께 삭제됩니다.`
    : '이 글을 삭제할까요?'
}

async function onDelete() {
  if (!window.confirm(deleteConfirmMessage())) return
  deleting.value = true
  actionError.value = ''
  try {
    await deletePost(post.value.id)
    await router.push('/community')
  } catch (error) {
    actionError.value = errorMessage(error)
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <article class="detail">
    <RouterLink to="/community" class="back" data-action="back-to-list">← 목록으로</RouterLink>

    <p v-if="notice" class="notice" role="status">{{ notice }}</p>

    <p v-if="loading" class="hint">불러오는 중…</p>

    <div v-else-if="notFound" class="not-found">
      <p>게시글을 찾을 수 없습니다.</p>
      <RouterLink to="/community">목록으로</RouterLink>
    </div>

    <p v-else-if="loadError" class="form-error" role="alert">{{ loadError }}</p>

    <template v-else-if="post">
      <header>
        <span class="chip" data-role="board">{{ boardChip(post.boardType) }}</span>
        <h1>{{ post.title }}</h1>
        <div class="meta">
          <RouterLink
            v-if="post.authorId !== null"
            :to="`/users/${post.authorId}`"
            data-role="author"
          >
            {{ post.authorNickname }}
          </RouterLink>
          <span v-else data-role="author">{{ authorName(post) }}</span>
          <span data-role="date">{{ formatDateTime(post.createdAt) }}</span>
          <span data-role="views">조회 {{ post.viewCount }}</span>
        </div>
      </header>

      <p class="content" data-role="content">{{ post.content }}</p>

      <PostAttachment :analysis="post.analysis" :concept="post.concept" />

      <div v-if="isMine" class="actions">
        <RouterLink :to="`/community/${post.id}/edit`" class="button" data-action="edit">
          수정
        </RouterLink>
        <button
          type="button"
          class="button danger"
          data-action="delete"
          :disabled="deleting"
          @click="onDelete"
        >
          삭제
        </button>
      </div>
      <p v-if="actionError" class="form-error" role="alert">{{ actionError }}</p>

      <PostComments :post-id="post.id" @count="liveCommentCount = $event" />
    </template>
  </article>
</template>

<style scoped>
.detail {
  padding: var(--space-8) 0;
}

.back {
  color: var(--color-text-muted);
  text-decoration: none;
}

.notice {
  margin-top: var(--space-4);
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius);
  background: var(--color-background);
}

.hint {
  margin-top: var(--space-6);
  color: var(--color-text-muted);
}

.not-found {
  margin-top: var(--space-6);
}

header {
  margin-top: var(--space-6);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.chip {
  padding: 0 var(--space-2);
  border-radius: var(--radius);
  background: var(--color-background);
  color: var(--color-text-muted);
  font-size: 0.85em;
}

h1 {
  margin: var(--space-2) 0;
  overflow-wrap: anywhere;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  color: var(--color-text-muted);
  font-size: 0.9em;
}

.meta a {
  color: var(--color-text);
}

.content {
  margin: var(--space-6) 0 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  line-height: 1.7;
}

.actions {
  display: flex;
  gap: var(--space-2);
  justify-content: flex-end;
  margin-top: var(--space-6);
}

.button {
  padding: var(--space-2) var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  color: var(--color-text);
  font: inherit;
  text-decoration: none;
  cursor: pointer;
}

.danger {
  border-color: var(--color-danger);
  color: var(--color-danger);
}

.form-error {
  margin-top: var(--space-4);
  color: var(--color-danger);
}
</style>
