<script setup>
// 새 커밋 배지 (P-1a AC15). 프로젝트 상세와 따로 불러와 실패해도 상세를 가리지 않는다 (조회 실패는 그리지 않는다).
// NEW_COMMIT "새 커밋이 있어요 · 메시지 · 상대 시각", UP_TO_DATE "최신 커밋을 분석했어요",
// UNKNOWN은 최신 커밋 정보가 있으면 그것만, NOT_SUPPORTED(ZIP)와 정보 없는 UNKNOWN은 아무것도 그리지 않는다.
import { computed, ref, watch } from 'vue'
import { GitCommitHorizontal } from 'lucide-vue-next'
import { fetchLatestCommit } from '@/api/projects'
import { useLatestRequest } from '@/composables/useLatestRequest'
import { formatRelativeTime } from '@/utils/relativeTime'
import BaseBadge from '@/components/common/BaseBadge.vue'

const props = defineProps({
  projectId: { type: Number, required: true },
})

const result = ref(null)
const loading = ref(true)

const latest = useLatestRequest()

async function load(id) {
  const request = latest.begin()
  loading.value = true
  result.value = null
  try {
    const value = await fetchLatestCommit(id, { signal: request.signal })
    if (!request.isCurrent()) return
    result.value = value
  } catch {
    // 배지는 보조 정보라 실패를 알리지 않는다 (서버도 실패를 UNKNOWN으로 주고, 여기 오는 것은 네트워크·권한 오류)
  } finally {
    if (request.isCurrent()) loading.value = false
  }
}

watch(() => props.projectId, load, { immediate: true })

const commit = computed(() => result.value?.latest ?? null)
const when = computed(() =>
  commit.value?.committedAt ? formatRelativeTime(commit.value.committedAt) : '',
)
const status = computed(() => {
  const value = result.value?.status
  if (value === 'NEW_COMMIT' || value === 'UP_TO_DATE') return value
  if (value === 'UNKNOWN' && commit.value) return 'UNKNOWN'
  return null
})
const tone = computed(
  () => ({ NEW_COMMIT: 'warning', UP_TO_DATE: 'success', UNKNOWN: 'neutral' })[status.value],
)
</script>

<template>
  <p
    v-if="loading"
    class="inline-flex items-center gap-1 text-sm text-text-muted"
    role="status"
    data-commit="checking"
  >
    <GitCommitHorizontal class="size-4" aria-hidden="true" />
    최신 커밋 확인 중…
  </p>
  <p v-else-if="status" class="flex flex-wrap items-center gap-2 text-sm" :data-commit="status">
    <BaseBadge :tone="tone">
      <GitCommitHorizontal class="size-3.5" aria-hidden="true" />
      <template v-if="status === 'NEW_COMMIT'">새 커밋이 있어요</template>
      <template v-else-if="status === 'UP_TO_DATE'">최신 커밋을 분석했어요</template>
      <template v-else>최신 커밋</template>
    </BaseBadge>
    <span v-if="status !== 'UP_TO_DATE' && commit.message" class="wrap-anywhere text-text">
      {{ commit.message }}
    </span>
    <span v-if="status !== 'UP_TO_DATE' && when" class="text-text-muted">· {{ when }}</span>
  </p>
</template>
