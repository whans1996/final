<script setup>
// SC-21 관리자 챌린지 편집 폼 (F-24 AC13). 값은 부모가 갖고, 입력하면 바뀐 값 전체를 update:modelValue로 올린다.
// 검증과 저장 버튼은 편집 화면(AdminChallengeEditView)이 둔다. 이미지는 고른 파일을 object URL로 미리 보여 주고,
// 바꾸거나 화면을 떠나면 이전 주소를 해제한다.
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { GOAL_TYPES, LOCK_MESSAGE } from './labels'
import {
  DESCRIPTION_MAX,
  GOAL_VALUE_MAX,
  GOAL_VALUE_MIN,
  IMAGE_ACCEPT,
  TITLE_MAX,
} from './validation'

const props = defineProps({
  /** { title, description, startDate, endDate, goalType, goalValue, image: File|null, removeImage: boolean } */
  modelValue: { type: Object, required: true },
  /** 서버·클라이언트 검증 사유: { 필드: 사유 } */
  errors: { type: Object, default: () => ({}) },
  /** 시작한 챌린지는 목표 유형·목표값·시작일을 바꿀 수 없다 (AC7) */
  locked: { type: Boolean, default: false },
  /** 저장된 대표 이미지 주소 (수정 화면) */
  currentImageUrl: { type: String, default: null },
})

const emit = defineEmits(['update:modelValue'])

function set(name, value) {
  emit('update:modelValue', { ...props.modelValue, [name]: value })
}

const invalid = (name) => (props.errors[name] ? 'true' : undefined)

// 새로 고른 파일의 미리보기 주소. 파일이 바뀌거나 화면을 떠나면 해제한다
const objectUrl = ref(null)
watch(
  () => props.modelValue.image,
  (file) => {
    if (objectUrl.value) URL.revokeObjectURL(objectUrl.value)
    objectUrl.value = file ? URL.createObjectURL(file) : null
  },
  { immediate: true },
)
onBeforeUnmount(() => {
  if (objectUrl.value) URL.revokeObjectURL(objectUrl.value)
})

const previewUrl = computed(() => {
  if (objectUrl.value) return objectUrl.value
  return props.modelValue.removeImage ? null : props.currentImageUrl
})

// 같은 파일을 다시 고를 수 있게 제거하면 파일 입력을 새로 만든다
const fileInputKey = ref(0)

function chooseImage(event) {
  const file = event.target.files?.[0]
  if (!file) return
  emit('update:modelValue', { ...props.modelValue, image: file, removeImage: false })
}

function removeImage() {
  fileInputKey.value++
  emit('update:modelValue', {
    ...props.modelValue,
    image: null,
    removeImage: Boolean(props.currentImageUrl),
  })
}
</script>

<template>
  <div class="challenge-form">
    <label class="field">
      <span
        >제목 <small>({{ TITLE_MAX }}자 이하)</small></span
      >
      <input
        name="title"
        type="text"
        :maxlength="TITLE_MAX"
        :value="modelValue.title"
        :aria-invalid="invalid('title')"
        @input="set('title', $event.target.value)"
      />
      <p v-if="errors.title" class="field-error" data-error-for="title">{{ errors.title }}</p>
    </label>

    <label class="field">
      <span>설명 <small>(선택, 2,000자 이하)</small></span>
      <textarea
        name="description"
        rows="4"
        :maxlength="DESCRIPTION_MAX"
        :value="modelValue.description"
        :aria-invalid="invalid('description')"
        @input="set('description', $event.target.value)"
      />
      <p v-if="errors.description" class="field-error" data-error-for="description">
        {{ errors.description }}
      </p>
    </label>

    <p v-if="locked" class="lock-notice" role="note" data-testid="lock-notice">
      {{ LOCK_MESSAGE }}
    </p>

    <div class="row">
      <label class="field">
        <span>시작일</span>
        <input
          name="startDate"
          type="date"
          :value="modelValue.startDate"
          :disabled="locked"
          :aria-invalid="invalid('startDate')"
          @input="set('startDate', $event.target.value)"
        />
        <p v-if="errors.startDate" class="field-error" data-error-for="startDate">
          {{ errors.startDate }}
        </p>
      </label>
      <label class="field">
        <span>종료일 <small>(90일 이하)</small></span>
        <input
          name="endDate"
          type="date"
          :value="modelValue.endDate"
          :aria-invalid="invalid('endDate')"
          @input="set('endDate', $event.target.value)"
        />
        <p v-if="errors.endDate" class="field-error" data-error-for="endDate">
          {{ errors.endDate }}
        </p>
      </label>
    </div>

    <div class="row">
      <label class="field">
        <span>목표 유형</span>
        <select
          name="goalType"
          :value="modelValue.goalType"
          :disabled="locked"
          :aria-invalid="invalid('goalType')"
          @change="set('goalType', $event.target.value)"
        >
          <option value="">선택</option>
          <option v-for="g in GOAL_TYPES" :key="g.value" :value="g.value">{{ g.label }}</option>
        </select>
        <p v-if="errors.goalType" class="field-error" data-error-for="goalType">
          {{ errors.goalType }}
        </p>
      </label>
      <label class="field">
        <span
          >목표 횟수
          <small>({{ GOAL_VALUE_MIN }}~{{ GOAL_VALUE_MAX.toLocaleString() }})</small></span
        >
        <input
          name="goalValue"
          type="number"
          inputmode="numeric"
          :min="GOAL_VALUE_MIN"
          :max="GOAL_VALUE_MAX"
          step="1"
          :value="modelValue.goalValue"
          :disabled="locked"
          :aria-invalid="invalid('goalValue')"
          @input="set('goalValue', $event.target.value)"
        />
        <p v-if="errors.goalValue" class="field-error" data-error-for="goalValue">
          {{ errors.goalValue }}
        </p>
      </label>
    </div>

    <div class="field">
      <span class="label">대표 이미지 <small>(선택, jpg·png·webp, 2MB 이하)</small></span>
      <div class="image">
        <img
          v-if="previewUrl"
          class="preview"
          :src="previewUrl"
          alt="대표 이미지 미리보기"
          data-testid="image-preview"
        />
        <div class="image-actions">
          <label class="file">
            {{ previewUrl ? '이미지 바꾸기' : '이미지 선택' }}
            <input
              :key="fileInputKey"
              name="image"
              type="file"
              :accept="IMAGE_ACCEPT"
              :aria-invalid="invalid('image')"
              @change="chooseImage"
            />
          </label>
          <button
            v-if="previewUrl"
            type="button"
            class="remove"
            data-action="remove-image"
            @click="removeImage"
          >
            이미지 제거
          </button>
        </div>
      </div>
      <p v-if="errors.image" class="field-error" data-error-for="image">{{ errors.image }}</p>
    </div>
  </div>
</template>

<style scoped>
.challenge-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-4);
}

.row .field {
  flex: 1 1 200px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  min-width: 0;
}

.field > span,
.label {
  font-weight: 600;
}

small {
  color: var(--color-text-muted);
  font-weight: 400;
}

input:not([type='file']),
select,
textarea {
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  font: inherit;
}

input:disabled,
select:disabled {
  color: var(--color-text-muted);
  background: var(--color-background);
  cursor: not-allowed;
}

[aria-invalid='true'] {
  border-color: var(--color-danger);
}

.field-error {
  margin: 0;
  color: var(--color-danger);
  font-size: 0.9em;
}

.lock-notice {
  margin: 0;
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius);
  color: #92400e;
  background: #fef3c7;
}

.image {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: var(--space-3);
}

.preview {
  width: 240px;
  max-width: 100%;
  aspect-ratio: 16 / 10;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  object-fit: cover;
}

.image-actions {
  display: flex;
  gap: var(--space-2);
}

.file,
.remove {
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  font: inherit;
  cursor: pointer;
}

.file {
  position: relative;
}

.file input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
}

.file:focus-within {
  outline: 2px solid var(--color-primary);
}

.remove {
  color: var(--color-danger);
  border-color: var(--color-danger);
}
</style>
