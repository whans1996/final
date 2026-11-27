<script setup>
// 라벨·에러·힌트가 붙는 입력 칸 (F-29a AC8).
// 기본 슬롯이 { id, describedBy, invalid, required }를 받아 입력에 단다 (* 표시는 aria-hidden이라 required를 입력에 달아야 스크린리더가 안다):
//   <FormField label="이메일" required :error="errors.email" v-slot="{ id, describedBy, invalid, required }">
//     <input :id="id" v-model="email" :aria-describedby="describedBy" :aria-invalid="invalid" :required="required" />
//   </FormField>
import { computed, useId } from 'vue'

const props = defineProps({
  label: { type: String, required: true },
  id: { type: String, default: null },
  error: { type: String, default: '' },
  hint: { type: String, default: '' },
  required: { type: Boolean, default: false },
})

const autoId = useId()
const inputId = computed(() => props.id ?? `field-${autoId}`)
const messageId = computed(() => `${inputId.value}-message`)
const describedBy = computed(() => (props.error || props.hint ? messageId.value : undefined))
const invalid = computed(() => (props.error ? 'true' : undefined))
</script>

<template>
  <div class="flex flex-col gap-1">
    <label :for="inputId" class="text-sm font-semibold text-text">
      {{ label }}
      <span v-if="required" data-required class="text-danger" aria-hidden="true">*</span>
    </label>
    <slot :id="inputId" :described-by="describedBy" :invalid="invalid" :required="required" />
    <p v-if="error" :id="messageId" data-error class="text-sm text-danger">{{ error }}</p>
    <p v-else-if="hint" :id="messageId" data-hint class="text-sm text-text-muted">{{ hint }}</p>
  </div>
</template>
