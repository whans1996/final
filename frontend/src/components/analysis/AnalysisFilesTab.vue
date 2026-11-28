<script setup>
// SC-07 "파일" 탭 (F-06 AC14·AC15): 레이어별 파일 목록 + 선택한 파일의 설명과 코드.
// 선택 파일과 강조할 줄은 부모가 route query(`file`·`lines`)로 관리한다. 여기서는 `select` 이벤트로 알리기만 한다.
// 면접 질문 근거(I-1a AC13·AC15)로 들어오면 highlight 범위를 코드에서 형광펜 색으로 강조한다.
// 분석·파일을 바꾸면 이전 요청을 취소하고 늦은 응답은 버린다 (V-4, #250).
import { computed, ref, watch } from 'vue'
import { fetchAnalysisFile, fetchAnalysisFiles } from '@/api/analyses'
import { errorCode, errorMessage } from '@/api/http'
import { useLatestRequest } from '@/composables/useLatestRequest'
import { groupByLayer, layerLabel } from '@/utils/layers'
import BaseButton from '@/components/common/BaseButton.vue'
import CodeViewer from '@/components/analysis/CodeViewer.vue'
import FilePath from '@/components/analysis/FilePath.vue'

const props = defineProps({
  analysisId: { type: Number, required: true },
  selectedFileId: { type: Number, default: null },
  highlight: { type: Object, default: null },
})

const emit = defineEmits(['select'])

const files = ref([])
const listLoading = ref(true)
const listError = ref('')

const file = ref(null)
const fileLoading = ref(false)
const fileError = ref('')
const fileNotFound = ref(false)

const groups = computed(() => groupByLayer(files.value))

const listLatest = useLatestRequest()
const fileLatest = useLatestRequest()

async function loadFiles() {
  const request = listLatest.begin()
  listLoading.value = true
  listError.value = ''
  try {
    const result = await fetchAnalysisFiles(props.analysisId)
    if (request.isCurrent()) files.value = result
  } catch (error) {
    if (request.isCurrent()) listError.value = errorMessage(error)
  } finally {
    if (request.isCurrent()) listLoading.value = false
  }
}

async function loadFile() {
  const fileId = props.selectedFileId
  const request = fileLatest.begin()
  file.value = null
  fileError.value = ''
  fileNotFound.value = false
  if (fileId == null) {
    fileLoading.value = false
    return
  }
  fileLoading.value = true
  try {
    const result = await fetchAnalysisFile(props.analysisId, fileId)
    if (request.isCurrent()) file.value = result
  } catch (error) {
    if (!request.isCurrent()) return
    if (errorCode(error) === 'ANALYSIS_001') fileNotFound.value = true
    else fileError.value = errorMessage(error)
  } finally {
    if (request.isCurrent()) fileLoading.value = false
  }
}

watch(() => props.analysisId, loadFiles, { immediate: true })
watch(() => [props.analysisId, props.selectedFileId], loadFile, { immediate: true })
</script>

<template>
  <div class="grid grid-cols-1 items-start gap-6 lg:grid-cols-[minmax(220px,300px)_minmax(0,1fr)]">
    <nav class="flex min-w-0 flex-col gap-4" aria-label="파일 목록">
      <p v-if="listLoading" class="text-text-muted">파일 목록을 불러오는 중…</p>
      <div v-else-if="listError" class="flex flex-col items-start gap-2">
        <p class="text-danger" role="alert">{{ listError }}</p>
        <BaseButton variant="secondary" size="sm" data-action="retry-files" @click="loadFiles">
          다시 시도
        </BaseButton>
      </div>
      <p v-else-if="!files.length" class="text-text-muted">저장된 파일이 없습니다.</p>
      <template v-else>
        <section v-for="group in groups" :key="group.layer" :data-layer="group.layer">
          <h3 class="mb-1 text-sm text-text-muted">{{ group.label }}</h3>
          <ul class="flex list-none flex-col gap-1 ps-0">
            <li v-for="item in group.items" :key="item.id">
              <button
                type="button"
                class="flex w-full cursor-pointer flex-col rounded-sm border px-3 py-2 text-left hover:border-border hover:bg-bg"
                :class="
                  item.id === selectedFileId
                    ? 'border-primary bg-primary-soft'
                    : 'border-transparent bg-transparent'
                "
                :data-file-id="item.id"
                :aria-current="item.id === selectedFileId ? 'true' : undefined"
                @click="emit('select', item.id)"
              >
                <FilePath class="text-sm wrap-anywhere" :path="item.path" />
                <span v-if="item.role" class="text-sm text-text-muted">{{ item.role }}</span>
              </button>
            </li>
          </ul>
        </section>
      </template>
    </nav>

    <article class="flex min-w-0 flex-col gap-4" data-section="file-detail">
      <p v-if="selectedFileId == null" class="text-text-muted">
        목록에서 파일을 고르면 설명과 코드를 보여 줍니다.
      </p>
      <p v-else-if="fileLoading" class="text-text-muted">파일을 불러오는 중…</p>
      <p v-else-if="fileNotFound" class="text-danger">파일을 찾을 수 없습니다.</p>
      <div v-else-if="fileError" class="flex flex-col items-start gap-2">
        <p class="text-danger" role="alert">{{ fileError }}</p>
        <BaseButton variant="secondary" size="sm" data-action="retry-file" @click="loadFile">
          다시 시도
        </BaseButton>
      </div>
      <template v-else-if="file">
        <header class="flex flex-col gap-1">
          <span
            class="self-start rounded-full bg-primary-soft px-2 text-xs font-semibold text-primary"
          >
            {{ layerLabel(file.layer) }}
          </span>
          <h3 class="text-base"><FilePath class="wrap-anywhere" :path="file.path" /></h3>
          <p v-if="file.role" class="text-sm text-text-muted">{{ file.role }}</p>
        </header>
        <p v-if="file.summary" class="whitespace-pre-line">{{ file.summary }}</p>
        <p v-else class="text-text-muted">이 파일은 설명을 만들지 못했습니다.</p>
        <CodeViewer
          :content="file.content ?? ''"
          :label="`${file.path} 코드`"
          :highlight="highlight"
          :scroll-ready="!listLoading"
        />
      </template>
    </article>
  </div>
</template>
