<script setup>
// 리뷰 작성·수정 폼 (F-11 AC15·AC16). 별을 고르고 내용(앞뒤 공백 제외 1~200자)을 입력해야 제출할 수 있다.
import { computed, ref } from 'vue'
import StarRating from '@/components/concept/StarRating.vue'

const MAX_LENGTH = 200

const props = defineProps({
  /** 수정이면 기존 값 { rating, content } */
  initial: { type: Object, default: null },
  submitLabel: { type: String, default: '등록' },
  submitting: { type: Boolean, default: false },
  /** 서버 details (필드별 사유) */
  errors: { type: Object, default: () => ({}) },
  /** 필드에 묶이지 않는 서버 message */
  error: { type: String, default: '' },
  cancellable: { type: Boolean, default: false },
})
const emit = defineEmits(['submit', 'cancel'])

const rating = ref(props.initial?.rating ?? null)
const content = ref(props.initial?.content ?? '')

const canSubmit = computed(
  () => rating.value !== null && content.value.trim().length > 0 && !props.submitting,
)

function onSubmit() {
  if (!canSubmit.value) return
  emit('submit', { rating: rating.value, content: content.value.trim() })
}
</script>

<template>
  <form class="review-form" novalidate @submit.prevent="onSubmit">
    <StarRating v-model="rating" />
    <p v-if="errors.rating" class="field-error" data-error="rating">{{ errors.rating }}</p>

    <label class="content">
      <span class="sr-only">리뷰 내용</span>
      <textarea
        v-model="content"
        name="content"
        rows="3"
        :maxlength="MAX_LENGTH"
        placeholder="이 카드가 이해에 도움이 됐나요?"
      />
    </label>
    <div class="meta">
      <p v-if="errors.content" class="field-error" data-error="content">{{ errors.content }}</p>
      <span class="count" data-role="count">{{ content.length }}/{{ MAX_LENGTH }}</span>
    </div>

    <p v-if="error" class="form-error" role="alert">{{ error }}</p>

    <div class="actions">
      <button v-if="cancellable" type="button" data-action="cancel" @click="emit('cancel')">
        취소
      </button>
      <button type="submit" class="primary" data-action="submit" :disabled="!canSubmit">
        {{ submitLabel }}
      </button>
    </div>
  </form>
</template>

<style scoped>
.review-form {
  display: grid;
  gap: var(--space-2);
  min-width: 0;
}

.content textarea {
  box-sizing: border-box;
  width: 100%;
  padding: var(--space-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  font: inherit;
  resize: vertical;
}

.meta {
  display: flex;
  justify-content: space-between;
  gap: var(--space-2);
}

.count {
  margin-left: auto;
  color: var(--color-text-muted);
  font-size: 0.85em;
}

.field-error {
  margin: 0;
  color: var(--color-danger);
  font-size: 0.85em;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
}
</style>
