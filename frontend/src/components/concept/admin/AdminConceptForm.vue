<script setup>
// SC-20 관리자 카드 편집 폼 (F-13 AC14·AC15). 값은 부모가 갖고, 입력하면 바뀐 값 전체를 update:modelValue로 올린다.
// 본문은 오른쪽에 MarkdownBody 미리보기, 640px 이하에서는 "편집/미리보기" 탭으로 한쪽만 보인다.
// 버튼(저장·공개·숨김·삭제)은 카드 상태에 따라 달라서 편집 화면(AdminConceptEditView)이 둔다.
import { ref } from 'vue'
import MarkdownBody from '@/components/concept/MarkdownBody.vue'
import { LEVELS } from '@/utils/authValidation'
import { CONCEPT_CATEGORIES } from '@/utils/conceptCategories'

const props = defineProps({
  /** { name, category, difficulty, summary, body, exampleCode, referenceUrl } (빈 값은 '') */
  modelValue: { type: Object, required: true },
  /** 서버 details: { 필드: 사유 } */
  errors: { type: Object, default: () => ({}) },
  /** 시드 카드는 이름을 바꿀 수 없다 (AC9) */
  nameReadonly: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])

const pane = ref('edit')

function set(name, value) {
  emit('update:modelValue', { ...props.modelValue, [name]: value })
}

const invalid = (name) => (props.errors[name] ? 'true' : undefined)
</script>

<template>
  <div class="concept-form">
    <div class="row">
      <label class="field grow">
        <span>이름</span>
        <input
          name="name"
          type="text"
          maxlength="100"
          :value="modelValue.name"
          :readonly="nameReadonly"
          :aria-invalid="invalid('name')"
          @input="set('name', $event.target.value)"
        />
        <p v-if="errors.name" class="field-error" data-error-for="name">{{ errors.name }}</p>
      </label>
      <label class="field">
        <span>카테고리</span>
        <select
          name="category"
          :value="modelValue.category"
          :aria-invalid="invalid('category')"
          @change="set('category', $event.target.value)"
        >
          <option value="">선택</option>
          <option v-for="c in CONCEPT_CATEGORIES" :key="c.value" :value="c.value">
            {{ c.label }}
          </option>
        </select>
        <p v-if="errors.category" class="field-error" data-error-for="category">
          {{ errors.category }}
        </p>
      </label>
      <label class="field">
        <span>난이도</span>
        <select
          name="difficulty"
          :value="modelValue.difficulty"
          :aria-invalid="invalid('difficulty')"
          @change="set('difficulty', $event.target.value)"
        >
          <option value="">선택</option>
          <option v-for="l in LEVELS" :key="l.value" :value="l.value">{{ l.label }}</option>
        </select>
        <p v-if="errors.difficulty" class="field-error" data-error-for="difficulty">
          {{ errors.difficulty }}
        </p>
      </label>
    </div>

    <label class="field">
      <span>요약 <small>(500자 이하)</small></span>
      <textarea
        name="summary"
        rows="2"
        maxlength="500"
        :value="modelValue.summary"
        :aria-invalid="invalid('summary')"
        @input="set('summary', $event.target.value)"
      />
      <p v-if="errors.summary" class="field-error" data-error-for="summary">
        {{ errors.summary }}
      </p>
    </label>

    <div class="field">
      <span class="label">본문 <small>(Markdown, 10,000자 이하)</small></span>
      <div class="pane-tabs" role="tablist" aria-label="본문 보기">
        <button
          type="button"
          role="tab"
          data-pane-tab="edit"
          :aria-selected="pane === 'edit' ? 'true' : 'false'"
          @click="pane = 'edit'"
        >
          편집
        </button>
        <button
          type="button"
          role="tab"
          data-pane-tab="preview"
          :aria-selected="pane === 'preview' ? 'true' : 'false'"
          @click="pane = 'preview'"
        >
          미리보기
        </button>
      </div>
      <div class="editor" :data-pane="pane">
        <textarea
          name="body"
          class="body-input"
          rows="14"
          maxlength="10000"
          aria-label="본문"
          :value="modelValue.body"
          :aria-invalid="invalid('body')"
          @input="set('body', $event.target.value)"
        />
        <div class="preview" data-testid="body-preview" aria-label="본문 미리보기">
          <MarkdownBody v-if="modelValue.body.trim()" :source="modelValue.body" />
          <p v-else class="hint">본문을 입력하면 여기에 미리 보입니다.</p>
        </div>
      </div>
      <p v-if="errors.body" class="field-error" data-error-for="body">{{ errors.body }}</p>
    </div>

    <label class="field">
      <span>예제 코드 <small>(선택)</small></span>
      <textarea
        name="exampleCode"
        class="code"
        rows="6"
        maxlength="10000"
        spellcheck="false"
        :value="modelValue.exampleCode"
        :aria-invalid="invalid('exampleCode')"
        @input="set('exampleCode', $event.target.value)"
      />
      <p v-if="errors.exampleCode" class="field-error" data-error-for="exampleCode">
        {{ errors.exampleCode }}
      </p>
    </label>

    <label class="field">
      <span>참고 링크 <small>(선택, http:// 또는 https://)</small></span>
      <input
        name="referenceUrl"
        type="url"
        maxlength="500"
        placeholder="https://"
        :value="modelValue.referenceUrl"
        :aria-invalid="invalid('referenceUrl')"
        @input="set('referenceUrl', $event.target.value)"
      />
      <p v-if="errors.referenceUrl" class="field-error" data-error-for="referenceUrl">
        {{ errors.referenceUrl }}
      </p>
    </label>
  </div>
</template>

<style scoped>
.concept-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-4);
}

.field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  min-width: 0;
}

.field.grow {
  flex: 1 1 240px;
}

.field > span,
.label {
  font-weight: 600;
}

small {
  color: var(--color-text-muted);
  font-weight: 400;
}

input,
select,
textarea {
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  font: inherit;
}

input[readonly] {
  color: var(--color-text-muted);
  background: var(--color-background);
}

[aria-invalid='true'] {
  border-color: var(--color-danger);
}

textarea {
  resize: vertical;
}

.code,
.body-input {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 0.9em;
}

.editor {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: var(--space-4);
}

.preview {
  min-height: 100%;
  padding: var(--space-3) var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-background);
  overflow: auto;
}

.pane-tabs {
  display: none;
  gap: var(--space-1);
}

.pane-tabs button {
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  font: inherit;
  cursor: pointer;
}

.pane-tabs button[aria-selected='true'] {
  color: #fff;
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.hint {
  margin: 0;
  color: var(--color-text-muted);
}

.field-error {
  margin: 0;
  color: var(--color-danger);
  font-size: 0.9em;
}

@media (max-width: 640px) {
  .pane-tabs {
    display: flex;
  }

  .editor {
    grid-template-columns: minmax(0, 1fr);
  }

  .editor[data-pane='edit'] .preview,
  .editor[data-pane='preview'] .body-input {
    display: none;
  }
}
</style>
