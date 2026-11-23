<script setup>
// 팀 만들기·수정 공용 폼 (F-20a AC13·AC15). 이름 30자·소개 200자 제한, 이름이 공백뿐이거나 요청 중이면 제출 비활성.
// 서버 검증 메시지(details)는 fieldErrors로, 그 밖의 실패 메시지는 formError로 받아 보여 준다.
import { computed, reactive } from 'vue'
import { TEAM_DESCRIPTION_MAX, TEAM_NAME_MAX } from '@/api/teams'

const props = defineProps({
  submitLabel: { type: String, required: true },
  initial: { type: Object, default: null },
  busy: { type: Boolean, default: false },
  fieldErrors: { type: Object, default: () => ({}) },
  formError: { type: String, default: '' },
  cancelable: { type: Boolean, default: false },
})

const emit = defineEmits(['submit', 'cancel'])

const form = reactive({
  name: props.initial?.name ?? '',
  description: props.initial?.description ?? '',
})

const canSubmit = computed(() => form.name.trim() !== '' && !props.busy)

function onSubmit() {
  if (!canSubmit.value) return
  emit('submit', { name: form.name, description: form.description })
}
</script>

<template>
  <form class="team-form" novalidate @submit.prevent="onSubmit">
    <label class="field">
      <span>팀 이름</span>
      <input
        v-model="form.name"
        name="name"
        type="text"
        :maxlength="TEAM_NAME_MAX"
        placeholder="예: 스프링 스터디"
      />
    </label>
    <p v-if="fieldErrors.name" class="field-error" data-error="name">{{ fieldErrors.name }}</p>

    <label class="field">
      <span>소개 (선택)</span>
      <textarea
        v-model="form.description"
        name="description"
        rows="3"
        :maxlength="TEAM_DESCRIPTION_MAX"
        placeholder="무엇을 함께 공부하는 팀인지 적어 주세요"
      />
    </label>
    <p v-if="fieldErrors.description" class="field-error" data-error="description">
      {{ fieldErrors.description }}
    </p>

    <p v-if="formError" class="form-error" role="alert" data-error="form">{{ formError }}</p>

    <div class="actions">
      <button
        v-if="cancelable"
        type="button"
        class="secondary"
        data-action="cancel"
        :disabled="busy"
        @click="emit('cancel')"
      >
        취소
      </button>
      <button type="submit" class="primary" :disabled="!canSubmit">{{ submitLabel }}</button>
    </div>
  </form>
</template>

<style scoped>
.team-form {
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
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  font: inherit;
  font-weight: normal;
}

.field textarea {
  resize: vertical;
}

.field-error,
.form-error {
  margin: 0;
  color: var(--color-danger);
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
}

.primary,
.secondary {
  padding: var(--space-2) var(--space-6);
  border-radius: var(--radius);
  font: inherit;
  cursor: pointer;
}

.primary {
  border: none;
  background: var(--color-primary);
  color: #fff;
}

.secondary {
  border: 1px solid var(--color-border);
  background: transparent;
  color: inherit;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
