<script setup>
// 댓글 입력창 (F-18 AC12·AC13). 작성("등록")과 인라인 수정("저장"·"취소")이 같이 쓴다.
// 값은 부모가 v-model로 가진다. 500자는 textarea maxlength로 막고, 공백뿐이거나 저장 중이면 제출을 막는다.
import { computed } from 'vue'

const MAX_LENGTH = 500

const props = defineProps({
  modelValue: { type: String, required: true },
  submitLabel: { type: String, required: true },
  saving: { type: Boolean, default: false },
  fieldError: { type: String, default: '' },
  cancellable: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'submit', 'cancel'])

const blank = computed(() => props.modelValue.trim() === '')

function onSubmit() {
  if (blank.value || props.saving) return
  emit('submit')
}
</script>

<template>
  <form class="comment-form" @submit.prevent="onSubmit">
    <textarea
      name="content"
      rows="3"
      :maxlength="MAX_LENGTH"
      aria-label="댓글 내용"
      placeholder="댓글을 입력해 주세요"
      :value="modelValue"
      @input="emit('update:modelValue', $event.target.value)"
    ></textarea>
    <p v-if="fieldError" class="field-error" data-role="field-error">{{ fieldError }}</p>
    <div class="row">
      <span class="counter" data-role="counter">{{ modelValue.length }}/{{ MAX_LENGTH }}</span>
      <button
        v-if="cancellable"
        type="button"
        class="button"
        data-action="cancel"
        :disabled="saving"
        @click="emit('cancel')"
      >
        취소
      </button>
      <button type="submit" class="button primary" data-action="submit" :disabled="blank || saving">
        {{ submitLabel }}
      </button>
    </div>
  </form>
</template>

<style scoped>
.comment-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

textarea {
  box-sizing: border-box;
  width: 100%;
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  color: var(--color-text);
  font: inherit;
  resize: vertical;
}

.field-error {
  margin: 0;
  color: var(--color-danger);
  font-size: 0.9em;
}

.row {
  display: flex;
  gap: var(--space-2);
  align-items: center;
  justify-content: flex-end;
}

.counter {
  margin-right: auto;
  color: var(--color-text-muted);
  font-size: 0.85em;
}

.button {
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  color: var(--color-text);
  font: inherit;
  cursor: pointer;
}

.primary {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: #fff;
}

.button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
