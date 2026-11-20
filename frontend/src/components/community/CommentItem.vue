<script setup>
// 댓글 한 개 (F-18 AC11·AC13). 내 댓글이면 머리 줄에 "수정"·"삭제" 글자 버튼(모바일도 같은 배치).
// 수정 중 여부는 부모가 정한다(한 번에 한 댓글). 저장·삭제에 성공하거나 이미 지워진 댓글(POST_003)이면 changed를 올려 부모가 목록을 다시 받는다.
// 내용은 텍스트 보간 + pre-wrap으로 글자 그대로 보여 준다 (v-html 금지, spec 5절).
import { ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { deleteComment, updateComment } from '@/api/posts'
import { errorCode, errorDetails, errorMessage } from '@/api/http'
import CommentForm from '@/components/community/CommentForm.vue'
import { authorName, formatDateTime } from '@/components/community/boardTypes'

const props = defineProps({
  comment: { type: Object, required: true },
  postId: { type: Number, required: true },
  mine: { type: Boolean, default: false },
  editing: { type: Boolean, default: false },
})
const emit = defineEmits(['edit', 'cancel', 'changed'])

const draft = ref('')
const saving = ref(false)
const deleting = ref(false)
const fieldError = ref('')
const error = ref('')

// 수정을 열 때마다 원래 내용으로 채운다 ("취소" 뒤 다시 열어도 고치다 만 값이 남지 않는다)
watch(
  () => props.editing,
  (editing) => {
    fieldError.value = ''
    error.value = ''
    if (editing) draft.value = props.comment.content
  },
  { immediate: true },
)

async function onSave() {
  saving.value = true
  fieldError.value = ''
  error.value = ''
  try {
    await updateComment(props.postId, props.comment.id, { content: draft.value })
    emit('changed')
  } catch (e) {
    if (errorCode(e) === 'COMMON_400') fieldError.value = errorDetails(e).content ?? errorMessage(e)
    else error.value = errorMessage(e)
  } finally {
    saving.value = false
  }
}

async function onDelete() {
  if (!window.confirm('이 댓글을 삭제할까요?')) return
  deleting.value = true
  error.value = ''
  try {
    await deleteComment(props.postId, props.comment.id)
    emit('changed')
  } catch (e) {
    error.value = errorMessage(e)
    // 이미 지워진 댓글(다른 탭·기기에서 삭제)이면 목록을 다시 받아 화면에서도 뺀다
    if (errorCode(e) === 'POST_003') emit('changed')
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <li class="comment" :data-comment-id="comment.id">
    <div class="head">
      <RouterLink
        v-if="comment.authorId !== null"
        :to="`/users/${comment.authorId}`"
        class="author"
        data-role="comment-author"
      >
        {{ comment.authorNickname }}
      </RouterLink>
      <span v-else class="author" data-role="comment-author">{{ authorName(comment) }}</span>
      <time :datetime="comment.createdAt" class="date" data-role="comment-date">
        {{ formatDateTime(comment.createdAt) }}
      </time>
      <span v-if="comment.edited" class="edited" data-role="edited">(수정됨)</span>
      <span v-if="mine && !editing" class="actions">
        <button type="button" class="text-button" data-action="edit-comment" @click="emit('edit')">
          수정
        </button>
        <button
          type="button"
          class="text-button danger"
          data-action="delete-comment"
          :disabled="deleting"
          @click="onDelete"
        >
          삭제
        </button>
      </span>
    </div>

    <CommentForm
      v-if="editing"
      v-model="draft"
      submit-label="저장"
      cancellable
      :saving="saving"
      :field-error="fieldError"
      @submit="onSave"
      @cancel="emit('cancel')"
    />
    <p v-else class="content" data-role="comment-content">{{ comment.content }}</p>

    <p v-if="error" class="form-error" role="alert" data-role="comment-error">{{ error }}</p>
  </li>
</template>

<style scoped>
.comment {
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--color-border);
}

.head {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  align-items: baseline;
  margin-bottom: var(--space-2);
  font-size: 0.9em;
}

.author {
  color: var(--color-text);
  font-weight: 600;
  overflow-wrap: anywhere;
}

.date,
.edited {
  color: var(--color-text-muted);
}

.actions {
  display: flex;
  gap: var(--space-2);
  margin-left: auto;
}

.text-button {
  padding: 0;
  border: none;
  background: none;
  color: var(--color-text-muted);
  font: inherit;
  cursor: pointer;
}

.text-button:hover {
  text-decoration: underline;
}

.danger {
  color: var(--color-danger);
}

.content {
  margin: 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  line-height: 1.6;
}

.form-error {
  margin: var(--space-2) 0 0;
  color: var(--color-danger);
}
</style>
