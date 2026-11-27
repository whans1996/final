<script setup>
// SC-06 내 분석 목록 (F-05a AC16). 공개 배지와 메모 첫 줄을 보여 준다 (F-19 AC15). 수정·삭제·공개 설정은 SC-07에서만 한다.
// 머리에 프로젝트별 보기(SC-25) 링크가 있다 (P-1a 5절).
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { FolderGit2 } from 'lucide-vue-next'
import { fetchAnalyses } from '@/api/analyses'
import { errorMessage } from '@/api/http'
import { useLatestRequest } from '@/composables/useLatestRequest'
import { LEVELS } from '@/utils/authValidation'
import AnalysisStatusBadge from '@/components/analysis/AnalysisStatusBadge.vue'
import BaseBadge from '@/components/common/BaseBadge.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import PageNav from '@/components/common/PageNav.vue'

const PAGE_SIZE = 20

const items = ref([])
const page = ref(1)
const total = ref(0)
const loading = ref(true)
const loadError = ref('')

function levelLabel(level) {
  return LEVELS.find((l) => l.value === level)?.label ?? level
}

/** 메모의 첫 줄 (빈 줄은 건너뛴다). 없으면 null. */
function memoFirstLine(memo) {
  return memo?.split('\n').find((line) => line.trim() !== '') ?? null
}

/** '2026-11-24T14:20:00+09:00' → '2026.11.24' */
function formatDate(value) {
  return value ? value.slice(0, 10).replaceAll('-', '.') : ''
}

// 페이지를 빠르게 넘기면 이전 요청을 취소하고 늦게 도착한 응답은 버린다 (V-4)
const latest = useLatestRequest()

async function load(target) {
  const request = latest.begin()
  loading.value = true
  loadError.value = ''
  try {
    const result = await fetchAnalyses({ page: target, size: PAGE_SIZE })
    if (!request.isCurrent()) return
    items.value = result.items
    total.value = result.total
    page.value = result.page
  } catch (error) {
    if (!request.isCurrent()) return
    loadError.value = errorMessage(error)
  } finally {
    if (request.isCurrent()) loading.value = false
  }
}

onMounted(() => load(1))
</script>

<template>
  <section class="flex flex-col gap-6 py-8">
    <div class="flex flex-wrap items-center justify-between gap-4">
      <h1>내 분석</h1>
      <div class="flex flex-wrap items-center gap-2">
        <BaseButton variant="secondary" :to="{ name: 'projects' }" data-action="by-project">
          <FolderGit2 class="size-4" aria-hidden="true" />
          프로젝트별로 보기
        </BaseButton>
        <BaseButton :to="{ name: 'analysis-new' }">분석 요청</BaseButton>
      </div>
    </div>

    <p v-if="loading" class="text-text-muted">불러오는 중…</p>
    <p v-else-if="loadError" class="text-danger" role="alert">{{ loadError }}</p>

    <EmptyState
      v-else-if="items.length === 0"
      message="아직 분석이 없습니다. AI로 만든 프로젝트를 올려 코드를 이해해 보세요."
    >
      <template #action>
        <BaseButton :to="{ name: 'analysis-new' }" data-action="first-request">
          첫 분석 요청하기
        </BaseButton>
      </template>
    </EmptyState>

    <template v-else>
      <ul class="grid list-none grid-cols-1 gap-4 ps-0 sm:grid-cols-2 lg:grid-cols-3">
        <li v-for="analysis in items" :key="analysis.id">
          <RouterLink
            :to="{ name: 'analysis-detail', params: { id: analysis.id } }"
            class="flex h-full flex-col gap-2 rounded-md border border-border bg-surface p-4 text-text no-underline shadow-1 transition-colors hover:border-primary focus-visible:outline-2 focus-visible:outline-primary dark:bg-surface-raised"
            :data-analysis-id="analysis.id"
          >
            <span class="flex items-start justify-between gap-2">
              <strong class="title wrap-anywhere">
                {{ analysis.title ?? analysis.projectName ?? '제목 없는 분석' }}
              </strong>
              <BaseBadge v-if="analysis.isPublic" tone="primary" data-badge="public"
                >공개</BaseBadge
              >
            </span>
            <span
              v-if="memoFirstLine(analysis.memo)"
              class="truncate text-sm text-text-muted"
              data-section="memo"
            >
              {{ memoFirstLine(analysis.memo) }}
            </span>
            <span class="text-sm text-text-muted">
              {{ levelLabel(analysis.level) }} · {{ formatDate(analysis.createdAt) }}
            </span>
            <AnalysisStatusBadge :status="analysis.status" :progress="analysis.progress" />
          </RouterLink>
        </li>
      </ul>

      <PageNav :page="page" :size="PAGE_SIZE" :total="total" @change="load" />
    </template>
  </section>
</template>
