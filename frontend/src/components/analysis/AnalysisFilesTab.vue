<script setup>
// SC-07 "파일" 탭 (F-06 AC14·AC15): 레이어별 파일 목록 + 선택한 파일의 설명과 코드.
// 선택 파일은 부모가 route query(`file`)로 관리한다. 여기서는 `select` 이벤트로 알리기만 한다.
import { computed, ref, watch } from 'vue'
import { fetchAnalysisFile, fetchAnalysisFiles } from '@/api/analyses'
import { errorCode, errorMessage } from '@/api/http'
import { groupByLayer, layerLabel } from '@/utils/layers'
import CodeViewer from '@/components/analysis/CodeViewer.vue'
import FilePath from '@/components/analysis/FilePath.vue'

const props = defineProps({
  analysisId: { type: Number, required: true },
  selectedFileId: { type: Number, default: null },
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

async function loadFiles() {
  listLoading.value = true
  listError.value = ''
  try {
    files.value = await fetchAnalysisFiles(props.analysisId)
  } catch (error) {
    listError.value = errorMessage(error)
  } finally {
    listLoading.value = false
  }
}

// 응답이 늦게 와도 마지막으로 고른 파일만 보여 준다
let fileRequest = 0

async function loadFile() {
  const fileId = props.selectedFileId
  const request = ++fileRequest
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
    if (request === fileRequest) file.value = result
  } catch (error) {
    if (request !== fileRequest) return
    if (errorCode(error) === 'ANALYSIS_001') fileNotFound.value = true
    else fileError.value = errorMessage(error)
  } finally {
    if (request === fileRequest) fileLoading.value = false
  }
}

watch(() => props.analysisId, loadFiles, { immediate: true })
watch(() => [props.analysisId, props.selectedFileId], loadFile, { immediate: true })
</script>

<template>
  <div class="files-tab">
    <nav class="file-list" aria-label="파일 목록">
      <p v-if="listLoading" class="hint">파일 목록을 불러오는 중…</p>
      <div v-else-if="listError" class="error-box">
        <p class="form-error" role="alert">{{ listError }}</p>
        <button type="button" class="retry" data-action="retry-files" @click="loadFiles">
          다시 시도
        </button>
      </div>
      <p v-else-if="!files.length" class="hint">저장된 파일이 없습니다.</p>
      <template v-else>
        <section v-for="group in groups" :key="group.layer" :data-layer="group.layer">
          <h3>{{ group.label }}</h3>
          <ul>
            <li v-for="item in group.items" :key="item.id">
              <button
                type="button"
                class="file"
                :class="{ active: item.id === selectedFileId }"
                :data-file-id="item.id"
                :aria-current="item.id === selectedFileId ? 'true' : undefined"
                @click="emit('select', item.id)"
              >
                <FilePath class="path" :path="item.path" />
                <span v-if="item.role" class="role">{{ item.role }}</span>
              </button>
            </li>
          </ul>
        </section>
      </template>
    </nav>

    <article class="file-detail" data-section="file-detail">
      <p v-if="selectedFileId == null" class="hint">
        목록에서 파일을 고르면 설명과 코드를 보여 줍니다.
      </p>
      <p v-else-if="fileLoading" class="hint">파일을 불러오는 중…</p>
      <p v-else-if="fileNotFound" class="form-error">파일을 찾을 수 없습니다.</p>
      <div v-else-if="fileError" class="error-box">
        <p class="form-error" role="alert">{{ fileError }}</p>
        <button type="button" class="retry" data-action="retry-file" @click="loadFile">
          다시 시도
        </button>
      </div>
      <template v-else-if="file">
        <header>
          <span class="layer">{{ layerLabel(file.layer) }}</span>
          <h3 class="path"><FilePath :path="file.path" /></h3>
          <p v-if="file.role" class="role">{{ file.role }}</p>
        </header>
        <p v-if="file.summary" class="summary">{{ file.summary }}</p>
        <p v-else class="hint">이 파일은 설명을 만들지 못했습니다.</p>
        <CodeViewer :content="file.content ?? ''" :label="`${file.path} 코드`" />
      </template>
    </article>
  </div>
</template>

<style scoped>
.files-tab {
  display: grid;
  grid-template-columns: minmax(220px, 300px) minmax(0, 1fr);
  gap: var(--space-6);
  align-items: start;
}

.file-list,
.file-detail {
  min-width: 0;
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.file-list h3 {
  margin-bottom: var(--space-1);
  color: var(--color-text-muted);
  font-size: 0.85em;
}

.file-list ul {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding: 0;
  list-style: none;
}

.file {
  display: flex;
  flex-direction: column;
  width: 100%;
  padding: var(--space-2) var(--space-3);
  border: 1px solid transparent;
  border-radius: var(--radius);
  color: inherit;
  background: none;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.file:hover {
  background: var(--color-surface);
  border-color: var(--color-border);
}

.file.active {
  border-color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 8%, transparent);
}

.path {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 0.85em;
  overflow-wrap: anywhere;
}

.role {
  color: var(--color-text-muted);
  font-size: 0.85em;
}

.file-detail {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.file-detail header {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.file-detail h3.path {
  font-size: 0.95em;
}

.layer {
  align-self: flex-start;
  padding: 0 var(--space-2);
  border-radius: 999px;
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 10%, transparent);
  font-size: 0.8em;
  font-weight: 600;
}

.summary {
  white-space: pre-line;
}

.hint {
  color: var(--color-text-muted);
}

.form-error {
  color: var(--color-danger);
}

.error-box {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-2);
}

.retry {
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius);
  color: var(--color-primary);
  background: var(--color-surface);
  font: inherit;
  cursor: pointer;
}

@media (max-width: 768px) {
  .files-tab {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
