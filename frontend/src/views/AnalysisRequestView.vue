<script setup>
// SC-05 분석 요청 (F-05a AC14, F-05b AC13). ZIP 또는 공개 저장소 주소와 GMS 전송 동의를 받아 접수하고,
// 202면 진행 상태 화면으로 간다.
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { createAnalysis } from '@/api/analyses'
import { errorDetails, errorMessage } from '@/api/http'
import { LEVELS } from '@/utils/authValidation'

// 서버 multipart 한도와 같다 (backend application.yml max-file-size)
const MAX_FILE_BYTES = 10 * 1024 * 1024
const FILE_TOO_LARGE = '10MB 이하의 ZIP 파일만 올릴 수 있습니다.'

const auth = useAuthStore()
const router = useRouter()

const SOURCES = [
  { value: 'ZIP', label: 'ZIP 업로드' },
  { value: 'URL', label: '저장소 주소' },
]

const source = ref('ZIP')
const file = ref(null)
const sourceUrl = ref('')
const level = ref(auth.level)
const consent = ref(false)
const submitting = ref(false)
const formError = ref('')
const fieldErrors = ref({})

const isUrl = computed(() => source.value === 'URL')
const trimmedUrl = computed(() => sourceUrl.value.trim())
const fileTooLarge = computed(() => !!file.value && file.value.size > MAX_FILE_BYTES)
const fileError = computed(() => (fileTooLarge.value ? FILE_TOO_LARGE : fieldErrors.value.file))
const hasSource = computed(() =>
  isUrl.value ? !!trimmedUrl.value : !!file.value && !fileTooLarge.value,
)
const canSubmit = computed(() => hasSource.value && consent.value && !submitting.value)

// 방식을 바꾸면 다른 방식의 입력값과 그 필드 오류를 비운다 (ZIP 칸은 v-if로 다시 그려져 선택이 사라진다)
watch(source, () => {
  file.value = null
  sourceUrl.value = ''
  fieldErrors.value = { ...fieldErrors.value, file: undefined, sourceUrl: undefined }
})

function onFileChange(event) {
  file.value = event.target.files?.[0] ?? null
  fieldErrors.value = { ...fieldErrors.value, file: undefined }
}

function onUrlInput() {
  fieldErrors.value = { ...fieldErrors.value, sourceUrl: undefined }
}

async function onSubmit() {
  if (!canSubmit.value) return
  formError.value = ''
  fieldErrors.value = {}
  submitting.value = true
  try {
    const { analysisId } = await createAnalysis(
      isUrl.value
        ? { sourceUrl: trimmedUrl.value, level: level.value, consent: true }
        : { file: file.value, level: level.value, consent: true },
    )
    await router.push({ name: 'analysis-detail', params: { id: analysisId } })
  } catch (error) {
    formError.value = errorMessage(error)
    fieldErrors.value = errorDetails(error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="request">
    <h1>분석 요청</h1>
    <p class="hint">
      AI로 만든 프로젝트를 ZIP으로 올리거나 공개 저장소 주소를 입력하면 주요 파일을 골라 내 수준에
      맞게 설명해 드립니다.
    </p>

    <form class="card" novalidate @submit.prevent="onSubmit">
      <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>

      <fieldset class="sources">
        <legend>입력 방식</legend>
        <label
          v-for="item in SOURCES"
          :key="item.value"
          class="source"
          :class="{ selected: source === item.value }"
        >
          <input v-model="source" type="radio" name="source" :value="item.value" />
          <span>{{ item.label }}</span>
        </label>
      </fieldset>

      <label v-if="isUrl" class="field">
        <span>저장소 주소</span>
        <input
          v-model="sourceUrl"
          name="sourceUrl"
          type="url"
          inputmode="url"
          autocomplete="off"
          maxlength="500"
          placeholder="https://github.com/owner/repo"
          :aria-invalid="!!fieldErrors.sourceUrl"
          @input="onUrlInput"
        />
        <small v-if="fieldErrors.sourceUrl" class="field-error" data-error="sourceUrl">
          {{ fieldErrors.sourceUrl }}
        </small>
        <small class="hint">
          공개 GitHub·GitLab(gitlab.com) 저장소의 기본 브랜치를 분석합니다. 저장소 주소만
          입력하세요.
        </small>
        <small class="hint">
          하루 5회 · 지원 스택 Java/Spring/MyBatis/JavaScript/Vue · 저장소 아카이브 30MB 이하 · 큰
          저장소는 중요한 파일 30개를 골라 설명합니다
        </small>
      </label>

      <label v-else class="field">
        <span>프로젝트 ZIP 파일</span>
        <input
          name="file"
          type="file"
          accept=".zip,application/zip"
          :aria-invalid="!!fileError"
          @change="onFileChange"
        />
        <small v-if="fileError" class="field-error" data-error="file">{{ fileError }}</small>
        <small v-else class="hint">
          하루 5회 · 지원 스택 Java/Spring/MyBatis/JavaScript/Vue · ZIP 10MB 이하 · 큰 프로젝트는
          중요한 파일 30개를 골라 설명합니다
        </small>
      </label>

      <fieldset class="levels">
        <legend>설명 수준</legend>
        <label
          v-for="item in LEVELS"
          :key="item.value"
          class="level"
          :class="{ selected: level === item.value }"
        >
          <input v-model="level" type="radio" name="level" :value="item.value" />
          <strong>{{ item.label }}</strong>
          <span>{{ item.description }}</span>
        </label>
        <small v-if="fieldErrors.level" class="field-error" data-error="level">
          {{ fieldErrors.level }}
        </small>
      </fieldset>

      <div class="notice">
        <p>
          분석을 위해 주요 파일이 SSAFY GMS(교육용 AI 게이트웨이)를 거쳐 외부 AI 모델로 전송됩니다.
          GMS를 쓸 수 없을 때는 서버의 로컬 AI로 간이 분석하며, 이때는 외부로 전송되지 않습니다.
          비공개·민감한 코드는 올리지 마세요. <code>.env</code>와 키 패턴은 전송 전에 지웁니다.
        </p>
        <label class="confirm">
          <input v-model="consent" type="checkbox" name="consent" />
          <span>위 내용을 확인했고 코드 전송에 동의합니다.</span>
        </label>
        <small v-if="fieldErrors.consent" class="field-error" data-error="consent">
          {{ fieldErrors.consent }}
        </small>
      </div>

      <button class="submit" type="submit" :disabled="!canSubmit">
        {{ submitting ? '요청 중…' : '분석 요청' }}
      </button>
    </form>
  </section>
</template>

<style scoped>
.request {
  max-width: 560px;
  margin: 0 auto;
  padding: var(--space-8) 0;
}

.hint {
  color: var(--color-text-muted);
  font-weight: 400;
}

.card {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  margin-top: var(--space-6);
  padding: var(--space-6);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
}

.field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  font-weight: 600;
}

.field input {
  max-width: 100%;
  padding: var(--space-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  font: inherit;
  font-weight: 400;
}

.field input[aria-invalid='true'] {
  border-color: var(--color-danger);
}

.field-error,
.form-error {
  color: var(--color-danger);
  font-weight: 400;
}

.sources {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  padding: 0;
  border: none;
}

.sources legend {
  font-weight: 600;
}

.source {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  cursor: pointer;
}

.source.selected {
  border-color: var(--color-primary);
}

.levels {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: 0;
  border: none;
}

.levels legend {
  font-weight: 600;
}

.level {
  display: grid;
  grid-template-columns: auto 1fr;
  column-gap: var(--space-2);
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  cursor: pointer;
}

.level input {
  grid-row: span 2;
  align-self: center;
}

.level span {
  color: var(--color-text-muted);
  font-size: 0.9em;
}

.level.selected {
  border-color: var(--color-primary);
}

.notice {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-4);
  border-radius: var(--radius);
  background: var(--color-background);
  font-size: 0.9em;
}

.confirm {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  font-weight: 600;
}

.submit {
  padding: var(--space-3);
  border: none;
  border-radius: var(--radius);
  color: #fff;
  background: var(--color-primary);
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
