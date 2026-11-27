<script setup>
// SC-07 분석 상세.
// 진행 중: 상태를 바로 1회, 이후 2초마다 조회하고 DONE·FAILED·404이거나 화면을 떠나면 멈춘다 (F-05a AC15).
// DONE: 상세를 1회 불러와 "개요"·"구조"·"파일"·"실행 흐름"·"개념"·"퀴즈" 탭을 보여 준다. 탭과 선택 파일은 쿼리(?tab=files&file=123)에 둔다
// (F-06 AC13~AC15, F-21a AC14, F-07 AC14, F-08 AC8, F-14 AC9).
// FAILED도 상세를 1회 불러와 주인에게 관리 영역(삭제)을 보여 준다. 남의 공개 분석은 읽기 전용이다: 관리·메모·퀴즈 탭이 없고
// 실행 흐름은 카드만 보인다 (F-19 AC13·AC14). 내 분석이 DONE이면 진행 중인 분석 챌린지 달성도를 보인다 (#211).
// FAILED인 내 URL 분석은 같은 설정으로 다시 실행해 새 분석으로 바꾸고, ZIP 분석은 다시 올리라고 안내한다 (L-1a AC9).
// 제목 근처에 "프로젝트 이름 · v번호"를 보이고 주인에게만 프로젝트 상세 링크로 준다 (P-1a AC16).
import { computed, nextTick, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { fetchAnalysis, fetchAnalysisStatus, rerunAnalysis } from '@/api/analyses'
import { errorCode, errorMessage } from '@/api/http'
import { usePolling } from '@/composables/usePolling'
import { useLatestRequest } from '@/composables/useLatestRequest'
import { isInProgress } from '@/utils/analysisStatus'
import AnalysisStatusBadge from '@/components/analysis/AnalysisStatusBadge.vue'
import AnalysisOverviewTab from '@/components/analysis/AnalysisOverviewTab.vue'
import AnalysisStructureTab from '@/components/analysis/AnalysisStructureTab.vue'
import AnalysisFilesTab from '@/components/analysis/AnalysisFilesTab.vue'
import AnalysisFlowsTab from '@/components/analysis/AnalysisFlowsTab.vue'
import AnalysisConceptsTab from '@/components/analysis/AnalysisConceptsTab.vue'
import AnalysisQuizTab from '@/components/analysis/AnalysisQuizTab.vue'
import AnalysisManagePanel from '@/components/analysis/AnalysisManagePanel.vue'
import ChallengeProgressNotice from '@/components/challenge/ChallengeProgressNotice.vue'
import BaseButton from '@/components/common/BaseButton.vue'
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

const rerunning = ref(false)
const rerunError = ref('')

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
// 프로젝트·버전 (P-1a AC16). 버전이 아직 없으면(FETCHING·버전 전 실패) 이름만
const projectLabel = computed(() => {
  const project = analysis.value?.project
  if (!project) return null
  const version = analysis.value.version
  return version ? `${project.name} · v${version.number}` : project.name
})

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
// 다른 분석으로 옮기면 이전 상세 요청을 취소하고 늦게 온 응답은 버린다 (V-4)
const detailLatest = useLatestRequest()
let detailStage = null

function stageOf(value) {
  return value === 'DONE' || value === 'FAILED' ? 'finished' : 'progress'
}

async function loadDetail() {
  const id = currentId
  const request = detailLatest.begin()
  detailStage = stageOf(status.value?.status)
  detailLoading.value = true
  detailError.value = ''
  try {
    const result = await fetchAnalysis(id)
    if (request.isCurrent()) analysis.value = result
  } catch (error) {
    if (request.isCurrent()) detailError.value = errorMessage(error)
  } finally {
    if (request.isCurrent()) detailLoading.value = false
  }
}

function onUpdated(changes) {
  analysis.value = { ...analysis.value, ...changes }
}

async function rerun() {
  rerunning.value = true
  rerunError.value = ''
  try {
    const result = await rerunAnalysis(currentId)
    // 실패한 분석은 지워졌으므로 뒤로 가기로 돌아오지 않게 바꿔 넣는다. 새 id로 바뀌면 폴링이 다시 시작된다
    await router.replace({ name: 'analysis-detail', params: { id: String(result.analysisId) } })
  } catch (error) {
    rerunError.value = errorMessage(error)
  } finally {
    rerunning.value = false
  }
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
    detailLatest.invalidate()
    detailStage = null
    detailLoading.value = false
    detailError.value = ''
    rerunError.value = ''
    notFound.value = false
    loadError.value = ''
    loading.value = true
    polling.start()
  },
  { immediate: true },
)
</script>

<template>
  <section class="mx-auto flex flex-col py-8" :class="isDone ? 'max-w-6xl' : 'max-w-3xl'">
    <!-- 진행 중·실패 화면은 주인만 본다. 남의 것으로 확인된 공개 분석만 숨기고, 아래 소유자 줄로 돌아간다 (#200) -->
    <RouterLink
      v-if="!isDone || analysis?.mine !== false"
      :to="{ name: 'analyses' }"
      class="self-start text-sm"
      data-action="back-to-mine"
      >← 내 분석</RouterLink
    >
    <h1 class="mt-2 wrap-anywhere">{{ isDone ? title : '분석 진행 상태' }}</h1>
    <template v-if="projectLabel">
      <RouterLink
        v-if="mine"
        :to="{ name: 'project-detail', params: { id: analysis.project.id } }"
        class="mt-1 self-start text-sm font-semibold wrap-anywhere"
        data-section="project"
        >{{ projectLabel }}</RouterLink
      >
      <p v-else class="mt-1 text-sm wrap-anywhere text-text-muted" data-section="project">
        {{ projectLabel }}
      </p>
    </template>
    <p v-if="analysis && !mine" class="mt-2" data-section="owner">
      <RouterLink :to="{ name: 'user-profile', params: { id: analysis.owner.id } }">
        {{ analysis.owner.nickname }}님의 분석
      </RouterLink>
    </p>
    <p
      v-if="mine && analysis.memo"
      class="mt-2 wrap-anywhere whitespace-pre-line text-text-muted"
      data-section="memo"
    >
      {{ analysis.memo }}
    </p>
    <a
      v-if="(isDone || isFailed) && sourceLink"
      :href="sourceLink"
      target="_blank"
      rel="noopener noreferrer"
      class="mt-2 self-start text-sm"
      data-action="source"
    >
      저장소 보기
    </a>

    <div v-if="mine && manageDetail" class="mt-3 flex flex-col items-start gap-2">
      <AnalysisManagePanel
        :detail="manageDetail"
        class="self-stretch"
        @updated="onUpdated"
        @deleted="onDeleted"
      />
      <RouterLink
        v-if="isDone"
        :to="{ name: 'post-write', query: { analysisId: String(analysis.id) } }"
        class="font-semibold"
        data-action="ask-community"
      >
        커뮤니티에 질문하기
      </RouterLink>
    </div>
    <ChallengeProgressNotice v-if="mine && isDone" goal-type="ANALYSIS_COUNT" class="mt-2" />

    <p v-if="notFound" class="mt-6 text-danger" data-message="not-found">
      분석을 찾을 수 없습니다.
    </p>
    <p v-else-if="loading" class="mt-6 text-text-muted">불러오는 중…</p>

    <template v-else>
      <p v-if="loadError" class="mt-4 text-danger" role="alert">{{ loadError }}</p>

      <div v-if="status && isDone" class="result mt-4 flex flex-col gap-4">
        <div class="flex flex-wrap items-center gap-3">
          <AnalysisStatusBadge :status="status.status" :progress="status.progress" />
          <p class="font-semibold text-text-muted" data-message="done" role="status">
            분석이 끝났습니다.
            <template v-if="analysis">
              {{ levelLabel(analysis.level) }} 수준 · 설명한 파일
              {{ analysis.explainedFileCount }}/{{ analysis.fileCount }}개
            </template>
          </p>
        </div>

        <p v-if="detailLoading" class="text-text-muted">결과를 불러오는 중…</p>
        <div v-else-if="detailError" class="flex flex-col items-start gap-3">
          <p class="text-danger" role="alert" data-error="detail">{{ detailError }}</p>
          <BaseButton variant="secondary" size="sm" data-action="retry-detail" @click="loadDetail">
            다시 시도
          </BaseButton>
        </div>
        <template v-else-if="analysis">
          <!-- F-27 AC11: GMS 대신 Ollama로 만든 결과가 있으면 알린다 -->
          <p
            v-if="analysis.liteMode"
            class="rounded-sm bg-info-soft px-4 py-3 text-sm text-text"
            role="status"
            data-message="lite-mode"
          >
            일부 결과는 간이 모드(서버의 로컬 AI)로 만들어 설명이 짧거나 덜 정확할 수 있어요.
          </p>
          <div
            ref="tabsEl"
            class="flex gap-1 overflow-x-auto border-b border-border"
            role="tablist"
            aria-label="분석 결과"
          >
            <button
              v-for="tab in tabs"
              :id="`tab-${tab.name}`"
              :key="tab.name"
              type="button"
              role="tab"
              class="shrink-0 cursor-pointer border-b-2 border-transparent px-4 py-2 font-semibold whitespace-nowrap text-text-muted hover:text-text aria-selected:border-primary aria-selected:text-primary"
              :aria-selected="activeTab === tab.name ? 'true' : 'false'"
              :aria-controls="`panel-${tab.name}`"
              @click="selectTab(tab.name)"
            >
              {{ tab.label }}
            </button>
          </div>

          <div
            :id="`panel-${activeTab}`"
            class="min-w-0 rounded-md border border-border bg-surface p-4 sm:p-6 dark:bg-surface-raised"
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
            <AnalysisQuizTab
              v-else-if="mine"
              :analysis-id="analysis.id"
              :quiz-status="analysis.quizStatus ?? null"
              :regenerations-left="analysis.quizRegenerationsLeft ?? 0"
              @updated="onUpdated"
            />
          </div>
        </template>
      </div>

      <div
        v-else-if="status"
        class="mt-6 flex flex-col gap-4 rounded-md border border-border bg-surface p-6 shadow-1 dark:bg-surface-raised"
      >
        <AnalysisStatusBadge :status="status.status" :progress="status.progress" />

        <div
          v-if="status.status === 'FAILED'"
          class="flex flex-col items-start gap-3"
          data-message="failed"
        >
          <p class="text-danger">{{ status.errorMessage }}</p>
          <template v-if="mine && analysis.sourceType === 'URL'">
            <BaseButton
              variant="secondary"
              size="sm"
              data-action="rerun"
              :disabled="rerunning"
              @click="rerun"
            >
              같은 설정으로 다시 실행
            </BaseButton>
            <p v-if="rerunError" class="text-danger" role="alert" data-error="rerun">
              {{ rerunError }}
            </p>
          </template>
          <template v-else-if="mine && analysis.sourceType === 'ZIP'">
            <p class="text-text-muted" data-message="zip-rerun">
              ZIP 분석은 원본을 보관하지 않습니다. ZIP 파일을 다시 올려 주세요.
            </p>
            <!-- 같은 ZIP 프로젝트의 새 버전으로 올린다 (SC-26 "ZIP 다시 올리기"와 같게, P-1a AC4·AC15) -->
            <BaseButton
              :to="{
                name: 'analysis-new',
                query: analysis.project ? { projectId: String(analysis.project.id) } : {},
              }"
              data-action="retry"
            >
              ZIP 다시 올리기
            </BaseButton>
          </template>
          <BaseButton v-else :to="{ name: 'analysis-new' }" data-action="retry">
            다시 요청하기
          </BaseButton>
        </div>
        <p v-else class="text-text-muted">
          {{ status.progress }}% · 화면을 닫아도 분석은 계속됩니다. 내 분석에서 다시 볼 수 있습니다.
        </p>

        <!-- 진행 중·FAILED의 상세는 관리 영역용이다 (F-19 AC13) -->
        <div v-if="detailError" class="flex flex-col items-start gap-3">
          <p class="text-danger" role="alert" data-error="detail">{{ detailError }}</p>
          <BaseButton variant="secondary" size="sm" data-action="retry-detail" @click="loadDetail">
            다시 시도
          </BaseButton>
        </div>
      </div>
    </template>
  </section>
</template>
