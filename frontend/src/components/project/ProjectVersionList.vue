<script setup>
// 버전별로 묶은 분석 (P-1a AC15). versions는 서버가 번호 내림차순으로 주고, 분석이 없는 버전은 서버가 뺀다.
// 아직 버전이 없는 분석(FETCHING 중·버전 전에 실패)은 맨 위 "버전 정하는 중"에 모은다.
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import BaseBadge from '@/components/common/BaseBadge.vue'
import { formatRelativeTime } from '@/utils/relativeTime'
import { levelLabel, shortSha, statusLabel, statusTone } from './projectLabels'

const props = defineProps({
  versions: { type: Array, required: true },
  analyses: { type: Array, required: true },
})

const byVersion = computed(() => {
  const groups = new Map()
  for (const analysis of props.analyses) {
    const key = analysis.versionId ?? null
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key).push(analysis)
  }
  // 같은 버전 안에서는 요청 순서대로
  for (const list of groups.values()) list.sort((a, b) => a.createdAt.localeCompare(b.createdAt))
  return groups
})

const unversioned = computed(() => byVersion.value.get(null) ?? [])
const groups = computed(() =>
  props.versions.map((version) => ({ version, analyses: byVersion.value.get(version.id) ?? [] })),
)
</script>

<template>
  <div class="flex flex-col gap-4">
    <section
      v-if="unversioned.length"
      class="rounded-md border border-dashed border-border bg-surface p-4"
      data-section="no-version"
    >
      <h3 class="text-base">버전 정하는 중</h3>
      <p class="mt-1 text-sm text-text-muted">
        코드를 받는 중이거나 받기 전에 실패한 분석이에요. 코드를 받으면 커밋에 맞는 버전에 붙어요.
      </p>
      <ul class="mt-3 flex list-none flex-wrap gap-2 ps-0">
        <li v-for="analysis in unversioned" :key="analysis.id">
          <RouterLink
            :to="{ name: 'analysis-detail', params: { id: analysis.id } }"
            class="flex items-center gap-2 rounded-sm border border-border px-3 py-2 text-sm text-text no-underline hover:border-primary"
            :data-analysis-id="analysis.id"
          >
            <span class="font-semibold">{{ levelLabel(analysis.level) }}</span>
            <BaseBadge :tone="statusTone(analysis.status)">{{
              statusLabel(analysis.status)
            }}</BaseBadge>
          </RouterLink>
        </li>
      </ul>
    </section>

    <section
      v-for="{ version, analyses: items } in groups"
      :key="version.id"
      class="rounded-md border border-border bg-surface p-4 shadow-1 dark:bg-surface-raised"
      :data-version-id="version.id"
    >
      <header class="flex flex-wrap items-baseline gap-x-3 gap-y-1">
        <h3 class="text-lg">v{{ version.number }}</h3>
        <template v-if="version.commitSha">
          <code class="rounded-sm bg-code-bg px-1.5 py-0.5 text-xs text-code-text">
            {{ shortSha(version.commitSha) }}
          </code>
          <span v-if="version.commitMessage" class="min-w-0 wrap-anywhere text-sm text-text">
            {{ version.commitMessage }}
          </span>
          <span v-if="version.committedAt" class="text-sm text-text-muted">
            · {{ formatRelativeTime(version.committedAt) }}
          </span>
        </template>
        <span v-else class="text-sm text-text-muted">
          커밋 정보 없음 · {{ formatRelativeTime(version.createdAt) }}
        </span>
      </header>
      <ul class="mt-3 flex list-none flex-wrap gap-2 ps-0">
        <li v-for="analysis in items" :key="analysis.id">
          <RouterLink
            :to="{ name: 'analysis-detail', params: { id: analysis.id } }"
            class="flex items-center gap-2 rounded-sm border border-border px-3 py-2 text-sm text-text no-underline hover:border-primary"
            :data-analysis-id="analysis.id"
          >
            <span class="font-semibold">{{ levelLabel(analysis.level) }}</span>
            <BaseBadge :tone="statusTone(analysis.status)">{{
              statusLabel(analysis.status)
            }}</BaseBadge>
          </RouterLink>
        </li>
      </ul>
    </section>
  </div>
</template>
