<script setup>
// SC-05 분석 요청 (F-05a AC14, F-05b AC13). ZIP 또는 공개 저장소 주소와 GMS 전송 동의를 받아 접수하고,
// 202면 진행 상태 화면으로 간다.
// ?projectId=가 있으면 그 프로젝트의 새 버전으로 분석한다: 프로젝트를 불러와 입력 방식(저장소 프로젝트는 주소까지)을
// 고정하고 projectId를 함께 보낸다 (P-1a AC15).
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { createAnalysis } from '@/api/analyses'
import { fetchProject } from '@/api/projects'
import { errorCode, errorDetails, errorMessage } from '@/api/http'
import { useLatestRequest } from '@/composables/useLatestRequest'
import { LEVELS } from '@/utils/authValidation'
import BaseButton from '@/components/common/BaseButton.vue'

// 서버 multipart 한도와 같다 (backend application.yml max-file-size)
const MAX_FILE_BYTES = 10 * 1024 * 1024
const FILE_TOO_LARGE = '10MB 이하의 ZIP 파일만 올릴 수 있습니다.'

const auth = useAuthStore()
const route = useRoute()
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

// ?projectId= 프로젝트 (P-1a AC15)
const project = ref(null)
const projectLoading = ref(false)
const projectError = ref('')
const projectNotFound = ref(false)

const hasProjectQuery = computed(() => route.query.projectId != null)
const locked = computed(() => project.value !== null)
const isUrl = computed(() => source.value === 'URL')
const trimmedUrl = computed(() => sourceUrl.value.trim())
const fileTooLarge = computed(() => !!file.value && file.value.size > MAX_FILE_BYTES)
const fileError = computed(() => (fileTooLarge.value ? FILE_TOO_LARGE : fieldErrors.value.file))
const hasSource = computed(() =>
  isUrl.value ? !!trimmedUrl.value : !!file.value && !fileTooLarge.value,
)
// projectId로 들어왔으면 프로젝트를 불러온 뒤에만 보낼 수 있다 (못 찾으면 새 프로젝트로 몰래 보내지 않는다)
const canSubmit = computed(
  () =>
    hasSource.value &&
    consent.value &&
    !submitting.value &&
    (!hasProjectQuery.value || locked.value),
)

// 방식을 바꾸면 다른 방식의 입력값과 그 필드 오류를 비운다 (ZIP 칸은 v-if로 다시 그려져 선택이 사라진다).
// 프로젝트로 고정할 때는 주소를 넣은 채로 방식을 바꾸므로 비우지 않는다
watch(source, () => {
  if (locked.value) return
  file.value = null
  sourceUrl.value = ''
  fieldErrors.value = { ...fieldErrors.value, file: undefined, sourceUrl: undefined }
})

const projectRequest = useLatestRequest()

function unlock() {
  const wasLocked = locked.value
  project.value = null
  projectLoading.value = false
  projectError.value = ''
  projectNotFound.value = false
  if (wasLocked) source.value = 'ZIP' // watch(source)가 입력값을 비운다
}

async function loadProject(raw) {
  const request = projectRequest.begin()
  const id = Number(raw)
  if (!Number.isInteger(id) || id <= 0) {
    projectNotFound.value = true
    return
  }
  projectLoading.value = true
  try {
    const result = await fetchProject(id, { signal: request.signal })
    if (!request.isCurrent()) return
    project.value = result
    source.value = result.sourceType === 'URL' ? 'URL' : 'ZIP'
    file.value = null
    sourceUrl.value = result.sourceType === 'URL' ? (result.sourceUrl ?? '') : ''
    fieldErrors.value = {}
  } catch (error) {
    if (!request.isCurrent()) return
    if (errorCode(error) === 'PROJECT_404') projectNotFound.value = true
    else projectError.value = errorMessage(error)
  } finally {
    if (request.isCurrent()) projectLoading.value = false
  }
}

// 같은 화면에서 쿼리만 바뀌는 경우(프로젝트 → 메뉴의 "분석 요청")도 따라간다. 다른 화면으로 떠나는 중이면 건드리지 않는다
watch(
  () => route.query.projectId,
  (raw) => {
    if (route.name !== 'analysis-new') return
    projectRequest.invalidate()
    unlock()
    if (raw != null) loadProject(Array.isArray(raw) ? raw[0] : raw)
  },
  { immediate: true },
)

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
    const payload = isUrl.value
      ? { sourceUrl: trimmedUrl.value, level: level.value, consent: true }
      : { file: file.value, level: level.value, consent: true }
    if (locked.value) payload.projectId = project.value.id
    const { analysisId } = await createAnalysis(payload)
    await router.push({ name: 'analysis-detail', params: { id: analysisId } })
  } catch (error) {
    formError.value = errorMessage(error)
    fieldErrors.value = errorDetails(error)
  } finally {
    submitting.value = false
  }
}

const inputClass =
  'h-10 w-full rounded-sm border border-border bg-surface px-3 font-normal text-text aria-[invalid=true]:border-danger read-only:bg-bg read-only:text-text-muted focus-visible:outline-2 focus-visible:outline-primary'
const choiceClass =
  'flex cursor-pointer items-center gap-2 rounded-sm border border-border px-3 py-2 has-[:checked]:border-primary has-[:checked]:bg-primary-soft has-[:disabled]:cursor-not-allowed has-[:disabled]:opacity-50'
</script>

<template>
  <section class="mx-auto flex max-w-xl flex-col gap-6 py-8">
    <div>
      <h1>분석 요청</h1>
      <p class="mt-2 text-text-muted">
        AI로 만든 프로젝트를 ZIP으로 올리거나 공개 저장소 주소를 입력하면 주요 파일을 골라 내 수준에
        맞게 설명해 드립니다.
      </p>
    </div>

    <div
      v-if="hasProjectQuery"
      class="rounded-md border border-border bg-surface px-4 py-3 text-sm"
      data-section="project"
    >
      <p v-if="projectLoading" class="text-text-muted" role="status">프로젝트를 불러오는 중…</p>
      <p v-else-if="projectNotFound" class="text-danger" role="alert">
        프로젝트를 찾을 수 없어요.
        <RouterLink :to="{ name: 'analysis-new' }" class="ms-1">새 프로젝트로 요청하기</RouterLink>
      </p>
      <p v-else-if="projectError" class="text-danger" role="alert">{{ projectError }}</p>
      <p v-else-if="project">
        <RouterLink
          :to="{ name: 'project-detail', params: { id: project.id } }"
          class="font-semibold break-all"
          >{{ project.name }}</RouterLink
        >
        프로젝트의 새 버전으로 분석해요.
        <span class="text-text-muted">
          {{
            project.sourceType === 'URL'
              ? '기본 브랜치의 최신 커밋을 받아요.'
              : '같은 프로젝트의 새 ZIP 파일을 올려 주세요.'
          }}
        </span>
      </p>
    </div>

    <form
      class="flex flex-col gap-5 rounded-md border border-border bg-surface p-6 shadow-1 dark:bg-surface-raised"
      novalidate
      @submit.prevent="onSubmit"
    >
      <p v-if="formError" class="text-danger" role="alert">{{ formError }}</p>

      <fieldset class="flex flex-wrap gap-2">
        <legend class="mb-2 font-semibold">입력 방식</legend>
        <label v-for="item in SOURCES" :key="item.value" :class="choiceClass">
          <input
            v-model="source"
            type="radio"
            name="source"
            :value="item.value"
            :disabled="locked && source !== item.value"
          />
          <span>{{ item.label }}</span>
        </label>
      </fieldset>

      <label v-if="isUrl" class="flex flex-col gap-1 font-semibold">
        <span>저장소 주소</span>
        <input
          v-model="sourceUrl"
          name="sourceUrl"
          type="url"
          inputmode="url"
          autocomplete="off"
          maxlength="500"
          placeholder="https://github.com/owner/repo"
          :class="inputClass"
          :readonly="locked"
          :aria-invalid="!!fieldErrors.sourceUrl"
          @input="onUrlInput"
        />
        <small
          v-if="fieldErrors.sourceUrl"
          class="text-sm font-normal text-danger"
          data-error="sourceUrl"
        >
          {{ fieldErrors.sourceUrl }}
        </small>
        <small class="text-sm font-normal text-text-muted">
          공개 GitHub·GitLab(gitlab.com) 저장소의 기본 브랜치를 분석합니다. 저장소 주소만
          입력하세요.
        </small>
        <small class="text-sm font-normal text-text-muted">
          하루 5회 · 지원 스택 Java/Spring/MyBatis/JavaScript/Vue · 저장소 아카이브 30MB 이하 · 큰
          저장소는 중요한 파일 30개를 골라 설명합니다
        </small>
      </label>

      <label v-else class="flex flex-col gap-1 font-semibold">
        <span>프로젝트 ZIP 파일</span>
        <input
          name="file"
          type="file"
          accept=".zip,application/zip"
          class="max-w-full rounded-sm border border-border bg-surface p-2 font-normal text-text aria-[invalid=true]:border-danger"
          :aria-invalid="!!fileError"
          @change="onFileChange"
        />
        <small v-if="fileError" class="text-sm font-normal text-danger" data-error="file">
          {{ fileError }}
        </small>
        <small v-else class="text-sm font-normal text-text-muted">
          하루 5회 · 지원 스택 Java/Spring/MyBatis/JavaScript/Vue · ZIP 10MB 이하 · 큰 프로젝트는
          중요한 파일 30개를 골라 설명합니다
        </small>
      </label>

      <fieldset class="flex flex-col gap-2">
        <legend class="mb-2 font-semibold">설명 수준</legend>
        <label
          v-for="item in LEVELS"
          :key="item.value"
          class="grid cursor-pointer grid-cols-[auto_1fr] gap-x-2 rounded-sm border border-border p-3 has-[:checked]:border-primary has-[:checked]:bg-primary-soft"
        >
          <input
            v-model="level"
            type="radio"
            name="level"
            :value="item.value"
            class="row-span-2 self-center"
          />
          <strong>{{ item.label }}</strong>
          <span class="text-sm text-text-muted">{{ item.description }}</span>
        </label>
        <small v-if="fieldErrors.level" class="text-sm text-danger" data-error="level">
          {{ fieldErrors.level }}
        </small>
      </fieldset>

      <div class="flex flex-col gap-2 rounded-sm bg-bg p-4 text-sm">
        <p>
          분석을 위해 주요 파일이 SSAFY GMS(교육용 AI 게이트웨이)를 거쳐 외부 AI 모델로 전송됩니다.
          GMS를 쓸 수 없을 때는 서버의 로컬 AI로 간이 분석하며, 이때는 외부로 전송되지 않습니다.
          비공개·민감한 코드는 올리지 마세요. <code>.env</code>와 키 패턴은 전송 전에 지웁니다.
        </p>
        <label class="flex items-start gap-2 font-semibold">
          <input v-model="consent" type="checkbox" name="consent" class="mt-1" />
          <span>위 내용을 확인했고 코드 전송에 동의합니다.</span>
        </label>
        <small v-if="fieldErrors.consent" class="text-danger" data-error="consent">
          {{ fieldErrors.consent }}
        </small>
      </div>

      <BaseButton type="submit" :disabled="!canSubmit" :loading="submitting">
        {{ submitting ? '요청 중…' : '분석 요청' }}
      </BaseButton>
    </form>
  </section>
</template>
