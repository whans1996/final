<script setup>
// SC-07 분석 상세.
// 진행 중: 상태를 바로 1회, 이후 2초마다 조회하고 DONE·FAILED·404이거나 화면을 떠나면 멈춘다 (F-05a AC15).
// DONE: 상세를 1회 불러와 "개요"·"파일"·"실행 흐름"·"개념"·"퀴즈" 탭을 보여 준다. 탭과 선택 파일은 쿼리(?tab=files&file=123)에 둔다
// (F-06 AC13~AC15, F-07 AC14, F-08 AC8, F-14 AC9).
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { fetchAnalysis, fetchAnalysisStatus } from '@/api/analyses'
import { errorCode, errorMessage } from '@/api/http'
import { usePolling } from '@/composables/usePolling'
import { isInProgress } from '@/utils/analysisStatus'
import AnalysisStatusBadge from '@/components/analysis/AnalysisStatusBadge.vue'
import AnalysisOverviewTab from '@/components/analysis/AnalysisOverviewTab.vue'
import AnalysisFilesTab from '@/components/analysis/AnalysisFilesTab.vue'
import AnalysisFlowsTab from '@/components/analysis/AnalysisFlowsTab.vue'
import AnalysisConceptsTab from '@/components/analysis/AnalysisConceptsTab.vue'
import AnalysisQuizTab from '@/components/analysis/AnalysisQuizTab.vue'
import { LEVELS } from '@/utils/authValidation'

const POLL_INTERVAL_MS = 2000
const TABS = [
  { name: 'overview', label: '개요' },
  { name: 'files', label: '파일' },
  { name: 'flows', label: '실행 흐름' },
  { name: 'concepts', label: '개념' },
  { name: 'quiz', label: '퀴즈' },
]

const route = useRoute()
const router = useRouter()

const status = ref(null)
const loading = ref(true)
const notFound = ref(false)
const loadError = ref('')

const analysis = ref(null)
const detailLoading = ref(false)
const detailError = ref('')

const isDone = computed(() => status.value?.status === 'DONE')
const activeTab = computed(() =>
  TABS.some((t) => t.name === route.query.tab) ? route.query.tab : 'overview',
)
const selectedFileId = computed(() => {
  const id = Number(route.query.file)
  return Number.isInteger(id) && id > 0 ? id : null
})

// 상세는 상태가 DONE이 된 뒤 1회만 불러온다 (결과는 다시 바뀌지 않는다)
let detailRequest = 0

async function loadDetail() {
  const id = currentId
  const request = ++detailRequest
  detailLoading.value = true
  detailError.value = ''
  try {
    const result = await fetchAnalysis(id)
    if (request === detailRequest) analysis.value = result
  } catch (error) {
    if (request === detailRequest) detailError.value = errorMessage(error)
  } finally {
    if (request === detailRequest) detailLoading.value = false
  }
}

function levelLabel(level) {
  return LEVELS.find((l) => l.value === level)?.label ?? level
}

function selectTab(name) {
  if (name === activeTab.value) return
  router.push({ query: { ...route.query, tab: name } })
}

function selectFile(fileId) {
  router.push({ query: { ...route.query, tab: 'files', file: String(fileId) } })
}

async function poll(id, isCurrent) {
  try {
    const result = await fetchAnalysisStatus(id)
    if (!isCurrent()) return false
    status.value = result
    loadError.value = ''
    if (result.status === 'DONE' && !analysis.value && !detailLoading.value) loadDetail()
    return isInProgress(result.status)
  } catch (error) {
    if (!isCurrent()) return false
    if (errorCode(error) === 'ANALYSIS_404') {
      notFound.value = true
      return false
    }
    // 일시적인 오류일 수 있으니 메시지를 보여 주고 다음 주기에 다시 조회한다
    loadError.value = errorMessage(error)
    return true
  } finally {
    if (isCurrent()) loading.value = false
  }
}

let currentId = null
const polling = usePolling((isCurrent) => poll(currentId, isCurrent), {
  interval: POLL_INTERVAL_MS,
})

watch(
  () => Number(route.params.id),
  (id) => {
    // 상세가 아닌 주소로 나가는 중이면 params.id가 없어 NaN이 된다
    if (!Number.isInteger(id)) return
    currentId = id
    status.value = null
    analysis.value = null
    detailRequest += 1
    detailLoading.value = false
    detailError.value = ''
    notFound.value = false
    loadError.value = ''
    loading.value = true
    polling.start()
  },
  { immediate: true },
)
</script>

<template>
  <section class="detail" :class="{ wide: isDone }">
    <RouterLink :to="{ name: 'analyses' }" class="back">← 내 분석</RouterLink>
    <h1>{{ isDone ? (analysis?.title ?? '분석 결과') : '분석 진행 상태' }}</h1>

    <p v-if="notFound" class="form-error" data-message="not-found">분석을 찾을 수 없습니다.</p>
    <p v-else-if="loading" class="hint">불러오는 중…</p>

    <template v-else>
      <p v-if="loadError" class="form-error" role="alert">{{ loadError }}</p>

      <div v-if="status && isDone" class="result">
        <div class="meta">
          <AnalysisStatusBadge :status="status.status" :progress="status.progress" />
          <p class="done hint" data-message="done" role="status">
            분석이 끝났습니다.
            <template v-if="analysis">
              {{ levelLabel(analysis.level) }} 수준 · 설명한 파일
              {{ analysis.explainedFileCount }}/{{ analysis.fileCount }}개
            </template>
          </p>
        </div>

        <p v-if="detailLoading" class="hint">결과를 불러오는 중…</p>
        <div v-else-if="detailError" class="failed">
          <p class="form-error" role="alert">{{ detailError }}</p>
          <button type="button" class="retry-button" data-action="retry-detail" @click="loadDetail">
            다시 시도
          </button>
        </div>
        <template v-else-if="analysis">
          <div class="tabs" role="tablist" aria-label="분석 결과">
            <button
              v-for="tab in TABS"
              :id="`tab-${tab.name}`"
              :key="tab.name"
              type="button"
              role="tab"
              class="tab"
              :aria-selected="activeTab === tab.name ? 'true' : 'false'"
              :aria-controls="`panel-${tab.name}`"
              @click="selectTab(tab.name)"
            >
              {{ tab.label }}
            </button>
          </div>

          <div
            :id="`panel-${activeTab}`"
            class="panel"
            role="tabpanel"
            :aria-labelledby="`tab-${activeTab}`"
          >
            <AnalysisOverviewTab v-if="activeTab === 'overview'" :overview="analysis.overview" />
            <AnalysisFilesTab
              v-else-if="activeTab === 'files'"
              :analysis-id="analysis.id"
              :selected-file-id="selectedFileId"
              @select="selectFile"
            />
            <AnalysisFlowsTab
              v-else-if="activeTab === 'flows'"
              :analysis-id="analysis.id"
              :apis="analysis.overview?.apis ?? []"
              @select-file="selectFile"
            />
            <AnalysisConceptsTab
              v-else-if="activeTab === 'concepts'"
              :analysis-id="analysis.id"
              @select-file="selectFile"
            />
            <AnalysisQuizTab v-else :analysis-id="analysis.id" />
          </div>
        </template>
      </div>

      <div v-else-if="status" class="card">
        <AnalysisStatusBadge :status="status.status" :progress="status.progress" />

        <div v-if="status.status === 'FAILED'" class="failed" data-message="failed">
          <p class="form-error">{{ status.errorMessage }}</p>
          <RouterLink :to="{ name: 'analysis-new' }" class="retry" data-action="retry">
            다시 요청하기
          </RouterLink>
        </div>
        <p v-else class="hint">
          {{ status.progress }}% · 화면을 닫아도 분석은 계속됩니다. 내 분석에서 다시 볼 수 있습니다.
        </p>
      </div>
    </template>
  </section>
</template>

<style scoped>
.detail {
  max-width: 720px;
  margin: 0 auto;
  padding: var(--space-8) 0;
}

.detail.wide {
  max-width: 1120px;
}

.detail h1 {
  overflow-wrap: anywhere;
}

.result {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  margin-top: var(--space-4);
}

.meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
}

.tabs {
  display: flex;
  gap: var(--space-1);
  border-bottom: 1px solid var(--color-border);
}

.tab {
  padding: var(--space-2) var(--space-4);
  border: none;
  border-bottom: 2px solid transparent;
  color: var(--color-text-muted);
  background: none;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.tab[aria-selected='true'] {
  border-bottom-color: var(--color-primary);
  color: var(--color-primary);
}

.panel {
  min-width: 0;
  padding: var(--space-6);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
}

.retry-button {
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius);
  color: var(--color-primary);
  background: var(--color-surface);
  font: inherit;
  cursor: pointer;
}

@media (max-width: 480px) {
  .panel {
    padding: var(--space-4);
  }
}

.back {
  font-size: 0.9em;
}

.hint {
  color: var(--color-text-muted);
}

.form-error {
  color: var(--color-danger);
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

.done {
  font-weight: 600;
}

.failed {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-3);
}

.retry {
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius);
  color: #fff;
  background: var(--color-primary);
  font-weight: 700;
  text-decoration: none;
}
</style>
