<script setup>
// SC-07 분석 상세.
// 진행 중: 상태를 바로 1회, 이후 2초마다 조회하고 DONE·FAILED·404이거나 화면을 떠나면 멈춘다 (F-05a AC15).
// DONE: 상세를 1회 불러와 "개요"·"구조"·"파일"·"실행 흐름"·"개념"·"퀴즈" 탭을 보여 준다. 탭과 선택 파일은 쿼리(?tab=files&file=123)에 둔다
// (F-06 AC13~AC15, F-21a AC14, F-07 AC14, F-08 AC8, F-14 AC9).
// FAILED도 상세를 1회 불러와 주인에게 관리 영역(삭제)을 보여 준다. 남의 공개 분석은 읽기 전용이다: 관리·메모·퀴즈 탭이 없고
// 실행 흐름은 카드만 보인다 (F-19 AC13·AC14).
import { computed, nextTick, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { fetchAnalysis, fetchAnalysisStatus } from '@/api/analyses'
import { errorCode, errorMessage } from '@/api/http'
import { usePolling } from '@/composables/usePolling'
import { isInProgress } from '@/utils/analysisStatus'
import AnalysisStatusBadge from '@/components/analysis/AnalysisStatusBadge.vue'
import AnalysisOverviewTab from '@/components/analysis/AnalysisOverviewTab.vue'
import AnalysisStructureTab from '@/components/analysis/AnalysisStructureTab.vue'
import AnalysisFilesTab from '@/components/analysis/AnalysisFilesTab.vue'
import AnalysisFlowsTab from '@/components/analysis/AnalysisFlowsTab.vue'
import AnalysisConceptsTab from '@/components/analysis/AnalysisConceptsTab.vue'
import AnalysisQuizTab from '@/components/analysis/AnalysisQuizTab.vue'
import AnalysisManagePanel from '@/components/analysis/AnalysisManagePanel.vue'
import { LEVELS } from '@/utils/authValidation'

const POLL_INTERVAL_MS = 2000
const TABS = [
  { name: 'overview', label: '개요' },
  { name: 'structure', label: '구조' },
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
const isFailed = computed(() => status.value?.status === 'FAILED')
const mine = computed(() => analysis.value?.mine === true)
const title = computed(() => {
  if (!analysis.value) return '분석 결과'
  return analysis.value.title ?? analysis.value.projectName ?? '제목 없는 분석'
})
// 관리 영역은 상세를 불러온 뒤의 상태 변화(진행 중 → DONE·FAILED)를 따라야 하므로 폴링한 상태를 덮어쓴다
const manageDetail = computed(() =>
  analysis.value && status.value ? { ...analysis.value, status: status.value.status } : null,
)
// URL 분석의 정규화한 저장소 주소 (F-05b AC14, FAILED는 F-19 AC12). 서버가 https 주소만 저장하지만 href에 넣으므로 한 번 더 거른다
const sourceLink = computed(() => {
  const url = status.value?.sourceUrl ?? analysis.value?.sourceUrl
  return typeof url === 'string' && url.startsWith('https://') ? url : null
})
// 퀴즈는 주인만 푼다 (F-14, F-19 AC14)
const tabs = computed(() => (mine.value ? TABS : TABS.filter((t) => t.name !== 'quiz')))
const activeTab = computed(() =>
  tabs.value.some((t) => t.name === route.query.tab) ? route.query.tab : 'overview',
)
const tabsEl = ref(null)

// 좁은 화면에서 탭 줄이 가로로 스크롤되므로 선택된 탭이 보이게 한다 (?tab=quiz로 바로 들어온 경우 포함)
watch(
  [activeTab, analysis],
  async () => {
    await nextTick()
    tabsEl.value
      ?.querySelector('[aria-selected="true"]')
      ?.scrollIntoView?.({ block: 'nearest', inline: 'nearest' })
  },
  { flush: 'post' },
)

const selectedFileId = computed(() => {
  const id = Number(route.query.file)
  return Number.isInteger(id) && id > 0 ? id : null
})

// 상세는 첫 상태를 받으면 1회(진행 중이면 관리 영역용, F-19 AC13), 진행 중에 불러왔으면 DONE·FAILED가 된 뒤
// 결과를 위해 1회 더 불러온다 (결과는 다시 바뀌지 않는다). 단계는 요청할 때 정해 실패해도 같은 단계에서 다시 부르지 않는다
let detailRequest = 0
let detailStage = null

function stageOf(value) {
  return value === 'DONE' || value === 'FAILED' ? 'finished' : 'progress'
}

async function loadDetail() {
  const id = currentId
  const request = ++detailRequest
  detailStage = stageOf(status.value?.status)
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

function onUpdated(changes) {
  analysis.value = { ...analysis.value, ...changes }
}

function onDeleted() {
  router.push({ name: 'analyses' })
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
    if (detailStage !== 'finished' && detailStage !== stageOf(result.status)) loadDetail()
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
    detailStage = null
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
    <h1>{{ isDone ? title : '분석 진행 상태' }}</h1>
    <p v-if="analysis && !mine" class="owner" data-section="owner">
      <RouterLink :to="{ name: 'user-profile', params: { id: analysis.owner.id } }">
        {{ analysis.owner.nickname }}님의 분석
      </RouterLink>
    </p>
    <p v-if="mine && analysis.memo" class="memo" data-section="memo">{{ analysis.memo }}</p>
    <a
      v-if="(isDone || isFailed) && sourceLink"
      :href="sourceLink"
      target="_blank"
      rel="noopener noreferrer"
      class="source-link"
      data-action="source"
    >
      저장소 보기
    </a>

    <div v-if="mine && manageDetail" class="manage-area">
      <AnalysisManagePanel :detail="manageDetail" @updated="onUpdated" @deleted="onDeleted" />
      <RouterLink
        v-if="isDone"
        :to="{ name: 'post-write', query: { analysisId: String(analysis.id) } }"
        class="ask"
        data-action="ask-community"
      >
        커뮤니티에 질문하기
      </RouterLink>
    </div>

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
          <p class="form-error" role="alert" data-error="detail">{{ detailError }}</p>
          <button type="button" class="retry-button" data-action="retry-detail" @click="loadDetail">
            다시 시도
          </button>
        </div>
        <template v-else-if="analysis">
          <!-- F-27 AC11: GMS 대신 Ollama로 만든 결과가 있으면 알린다 -->
          <p v-if="analysis.liteMode" class="lite-mode" role="status" data-message="lite-mode">
            일부 결과는 간이 모드(서버의 로컬 AI)로 만들어 설명이 짧거나 덜 정확할 수 있어요.
          </p>
          <div ref="tabsEl" class="tabs" role="tablist" aria-label="분석 결과">
            <button
              v-for="tab in tabs"
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
            <AnalysisStructureTab
              v-else-if="activeTab === 'structure'"
              :analysis-id="analysis.id"
            />
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
              :readonly="!mine"
              @select-file="selectFile"
            />
            <AnalysisConceptsTab
              v-else-if="activeTab === 'concepts'"
              :analysis-id="analysis.id"
              @select-file="selectFile"
            />
            <AnalysisQuizTab v-else-if="mine" :analysis-id="analysis.id" />
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

        <!-- 진행 중·FAILED의 상세는 관리 영역용이다 (F-19 AC13) -->
        <div v-if="detailError" class="failed">
          <p class="form-error" role="alert" data-error="detail">{{ detailError }}</p>
          <button type="button" class="retry-button" data-action="retry-detail" @click="loadDetail">
            다시 시도
          </button>
        </div>
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

.lite-mode {
  margin: 0;
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius);
  background: var(--color-background);
  font-size: 0.9em;
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
  overflow-x: auto;
  border-bottom: 1px solid var(--color-border);
}

.tab {
  flex-shrink: 0;
  white-space: nowrap;
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

.owner,
.memo {
  margin: 0 0 var(--space-2);
}

.memo {
  white-space: pre-line;
  overflow-wrap: anywhere;
  color: var(--color-text-muted);
}

.manage-area {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}

.manage-area > section {
  align-self: stretch;
}

.ask {
  font-weight: 600;
}

.source-link {
  display: inline-block;
  margin-bottom: var(--space-2);
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
