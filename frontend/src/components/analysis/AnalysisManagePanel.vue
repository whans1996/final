<script setup>
// SC-07 주인 "관리" 영역 (F-19 AC13). 제목·메모 수정, 공개 토글(DONE만), 삭제(DONE·FAILED만).
// detail.status는 상세를 불러온 뒤 바뀔 수 있으므로 부모가 최신 상태를 넣어 준다.
import { computed, ref } from 'vue'
import { deleteAnalysis, updateAnalysis, updateVisibility } from '@/api/analyses'
import { errorMessage } from '@/api/http'

const TITLE_MAX = 100
const MEMO_MAX = 1000
const DELETE_CONFIRM = '이 분석을 삭제할까요? 퀴즈 풀이 기록은 남습니다.'

const props = defineProps({
  detail: { type: Object, required: true },
})

const emit = defineEmits(['updated', 'deleted'])

const editing = ref(false)
const title = ref('')
const memo = ref('')
const saving = ref(false)
const saveError = ref('')

const isPublic = ref(props.detail.isPublic)
const toggling = ref(false)
const visibilityError = ref('')

const deleting = ref(false)
const deleteError = ref('')

const isDone = computed(() => props.detail.status === 'DONE')
const canDelete = computed(() => ['DONE', 'FAILED'].includes(props.detail.status))

function startEdit() {
  title.value = props.detail.title ?? ''
  memo.value = props.detail.memo ?? ''
  saveError.value = ''
  editing.value = true
}

async function save() {
  saving.value = true
  saveError.value = ''
  try {
    const result = await updateAnalysis(props.detail.id, { title: title.value, memo: memo.value })
    editing.value = false
    emit('updated', result)
  } catch (error) {
    saveError.value = errorMessage(error)
  } finally {
    saving.value = false
  }
}

async function toggleVisibility() {
  const previous = isPublic.value
  const next = !previous
  isPublic.value = next
  toggling.value = true
  visibilityError.value = ''
  try {
    const result = await updateVisibility(props.detail.id, next)
    isPublic.value = result.isPublic
    emit('updated', { isPublic: result.isPublic })
  } catch (error) {
    isPublic.value = previous
    visibilityError.value = errorMessage(error)
  } finally {
    toggling.value = false
  }
}

async function remove() {
  if (!window.confirm(DELETE_CONFIRM)) return
  deleting.value = true
  deleteError.value = ''
  try {
    await deleteAnalysis(props.detail.id)
    emit('deleted')
  } catch (error) {
    deleteError.value = errorMessage(error)
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <section class="manage" aria-label="분석 관리">
    <form v-if="editing" class="edit" data-form="edit" @submit.prevent="save">
      <label class="field">
        <span>제목</span>
        <input v-model="title" type="text" name="title" data-field="title" />
        <span class="counter" data-counter="title">{{ title.length }}/{{ TITLE_MAX }}</span>
      </label>
      <label class="field">
        <span>메모</span>
        <textarea v-model="memo" name="memo" rows="4" data-field="memo"></textarea>
        <span class="counter" data-counter="memo">{{ memo.length }}/{{ MEMO_MAX }}</span>
      </label>
      <p v-if="saveError" class="form-error" role="alert" data-error="save">{{ saveError }}</p>
      <div class="actions">
        <button type="submit" class="primary" data-action="save" :disabled="saving">
          {{ saving ? '저장 중…' : '저장' }}
        </button>
        <button type="button" data-action="cancel-edit" :disabled="saving" @click="editing = false">
          취소
        </button>
      </div>
    </form>

    <div v-else class="actions">
      <button type="button" data-action="edit" @click="startEdit">제목·메모 수정</button>

      <label class="switch">
        <input
          type="checkbox"
          role="switch"
          data-action="visibility"
          :checked="isPublic"
          :disabled="!isDone || toggling"
          @change="toggleVisibility"
        />
        <span>공개</span>
      </label>

      <button
        v-if="canDelete"
        type="button"
        class="danger"
        data-action="delete"
        :disabled="deleting"
        @click="remove"
      >
        {{ deleting ? '삭제 중…' : '삭제' }}
      </button>
    </div>

    <p v-if="!editing" class="hint" data-message="visibility-hint">
      {{
        isDone
          ? '공개하면 팔로워 피드와 게시글 첨부에서 보입니다.'
          : '분석이 끝난 뒤에 공개할 수 있습니다.'
      }}
    </p>
    <p v-if="visibilityError" class="form-error" role="alert" data-error="visibility">
      {{ visibilityError }}
    </p>
    <p v-if="deleteError" class="form-error" role="alert" data-error="delete">
      {{ deleteError }}
    </p>
  </section>
</template>

<style scoped>
.manage {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
}

.actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
}

.actions button {
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  font: inherit;
  cursor: pointer;
}

.actions button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.actions .primary {
  border-color: var(--color-primary);
  color: #fff;
  background: var(--color-primary);
}

.actions .danger {
  border-color: var(--color-danger);
  color: var(--color-danger);
}

.switch {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  font-weight: 600;
}

.edit {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
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
  padding: var(--space-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  font: inherit;
  font-weight: 400;
}

.counter {
  align-self: flex-end;
  color: var(--color-text-muted);
  font-size: 0.85em;
  font-weight: 400;
}

.hint {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.9em;
}

.form-error {
  margin: 0;
  color: var(--color-danger);
}
</style>
