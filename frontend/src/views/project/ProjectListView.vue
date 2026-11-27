<script setup>
// SC-25 내 프로젝트 목록 (P-1a AC14). 분석이 하나 이상 남은 내 프로젝트를 마지막 분석 요청 최신순으로 보여 준다.
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { FolderGit2, FileArchive } from 'lucide-vue-next'
import { fetchProjects } from '@/api/projects'
import { errorMessage } from '@/api/http'
import { useLatestRequest } from '@/composables/useLatestRequest'
import { formatRelativeTime } from '@/utils/relativeTime'
import AsyncState from '@/components/common/AsyncState.vue'
import BaseBadge from '@/components/common/BaseBadge.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import PageNav from '@/components/common/PageNav.vue'
import { SOURCE_LABELS, levelLabel } from '@/components/project/projectLabels'

const PAGE_SIZE = 20

const items = ref([])
const page = ref(1)
const total = ref(0)
const loading = ref(true)
const loadError = ref('')

// 쪽을 빠르게 넘기면 이전 요청을 취소하고 늦게 온 응답은 버린다 (V-4)
const latest = useLatestRequest()

async function load(target) {
  const request = latest.begin()
  loading.value = true
  loadError.value = ''
  try {
    const result = await fetchProjects(
      { page: target, size: PAGE_SIZE },
      { signal: request.signal },
    )
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
      <div>
        <h1>내 프로젝트</h1>
        <p class="mt-1 text-sm text-text-muted">
          같은 저장소·ZIP 프로젝트의 분석을 버전별로 묶어 봐요.
        </p>
      </div>
      <BaseButton :to="{ name: 'analysis-new' }">분석 요청</BaseButton>
    </div>

    <AsyncState
      :loading="loading"
      :error="loadError"
      :empty="items.length === 0"
      empty-message="아직 분석한 프로젝트가 없어요"
      @retry="load(page)"
    >
      <template #empty-action>
        <BaseButton :to="{ name: 'analysis-new' }" data-action="first-request">
          첫 분석 요청하기
        </BaseButton>
      </template>

      <ul class="grid list-none grid-cols-1 gap-4 ps-0 sm:grid-cols-2 lg:grid-cols-3">
        <li v-for="project in items" :key="project.id">
          <RouterLink
            :to="{ name: 'project-detail', params: { id: project.id } }"
            class="flex h-full flex-col gap-3 rounded-md border border-border bg-surface p-4 text-text no-underline shadow-1 transition-colors hover:border-primary focus-visible:outline-2 focus-visible:outline-primary dark:bg-surface-raised"
            :data-project-id="project.id"
          >
            <span class="flex items-start justify-between gap-2">
              <strong class="min-w-0 break-all text-lg">{{ project.name }}</strong>
              <BaseBadge :tone="project.sourceType === 'URL' ? 'info' : 'neutral'">
                <FolderGit2
                  v-if="project.sourceType === 'URL'"
                  class="size-3.5"
                  aria-hidden="true"
                />
                <FileArchive v-else class="size-3.5" aria-hidden="true" />
                <span data-source>{{
                  SOURCE_LABELS[project.sourceType] ?? project.sourceType
                }}</span>
              </BaseBadge>
            </span>

            <span class="text-sm text-text-muted" data-section="latest-version">
              <template v-if="project.latestVersion">
                <span class="font-semibold text-text">v{{ project.latestVersion.number }}</span>
                <template v-if="project.latestVersion.commitMessage">
                  · {{ project.latestVersion.commitMessage }}
                </template>
              </template>
              <template v-else>버전 정하는 중</template>
            </span>

            <span class="mt-auto flex flex-wrap items-center gap-2">
              <BaseBadge v-for="level in project.levels" :key="level" tone="primary" data-level>
                {{ levelLabel(level) }}
              </BaseBadge>
            </span>
            <span class="text-xs text-text-muted">
              버전 {{ project.versionCount }}개 · 마지막 분석
              {{ formatRelativeTime(project.lastAnalyzedAt) }}
            </span>
          </RouterLink>
        </li>
      </ul>

      <PageNav :page="page" :size="PAGE_SIZE" :total="total" @change="load" />
    </AsyncState>
  </section>
</template>
