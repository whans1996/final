<script setup>
// SC-24 팀장의 목표 만들기 폼 (F-20c AC12). 검증은 서버가 하고, 400 details는 필드 아래, 그 밖의 실패는 폼 아래에 보여 준다.
// 실패해도 입력은 그대로 둔다 (부모가 폼을 다시 만들지 않는다).
import { reactive } from 'vue'
import { addDays, todayString } from './teamLabels'

defineProps({
  busy: { type: Boolean, default: false },
  fieldErrors: { type: Object, default: () => ({}) },
  formError: { type: String, default: '' },
})

const emit = defineEmits(['submit'])

const today = todayString()
const form = reactive({ startDate: today, endDate: addDays(today, 6), targetRate: 80 })

function onSubmit() {
  const rate = form.targetRate === '' || form.targetRate == null ? null : Number(form.targetRate)
  emit('submit', {
    startDate: form.startDate || null,
    endDate: form.endDate || null,
    targetRate: rate,
  })
}
</script>

<template>
  <form class="challenge-form" data-form="challenge" novalidate @submit.prevent="onSubmit">
    <h3>목표 만들기</h3>
    <div class="fields">
      <label>
        시작일
        <input v-model="form.startDate" type="date" name="startDate" :min="today" />
        <span v-if="fieldErrors.startDate" class="field-error" data-error="startDate">
          {{ fieldErrors.startDate }}
        </span>
      </label>
      <label>
        종료일
        <input v-model="form.endDate" type="date" name="endDate" :min="form.startDate || today" />
        <span v-if="fieldErrors.endDate" class="field-error" data-error="endDate">
          {{ fieldErrors.endDate }}
        </span>
      </label>
      <label>
        목표 정답률 (%)
        <input v-model="form.targetRate" type="number" name="targetRate" min="1" max="100" />
        <span v-if="fieldErrors.targetRate" class="field-error" data-error="targetRate">
          {{ fieldErrors.targetRate }}
        </span>
      </label>
    </div>
    <p v-if="fieldErrors.analysisId" class="form-error" data-error="analysisId">
      {{ fieldErrors.analysisId }}
    </p>
    <button type="submit" class="primary" data-action="create-challenge" :disabled="busy">
      목표 만들기
    </button>
    <p v-if="formError" class="form-error" role="alert" data-error="challenge-form">
      {{ formError }}
    </p>
  </form>
</template>

<style scoped>
.challenge-form {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-3);
  min-width: 0;
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
}

h3 {
  margin: 0;
  font-size: 1em;
}

.fields {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  min-width: 0;
}

label {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  min-width: 0;
  font-size: 0.9em;
}

input {
  max-width: 100%;
  padding: var(--space-1) var(--space-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  font: inherit;
}

.primary {
  padding: var(--space-2) var(--space-4);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius);
  background: var(--color-primary);
  color: #fff;
  font: inherit;
  cursor: pointer;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.field-error,
.form-error {
  margin: 0;
  color: var(--color-danger);
  font-size: 0.9em;
}
</style>
