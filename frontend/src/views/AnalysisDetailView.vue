<script setup>
// SC-07 분석 상세 — 진행 상태 부분 (F-05a AC15). 결과 탭은 F-06부터 붙는다.
// 상태를 바로 1회, 이후 2초마다 조회하고 DONE·FAILED·404이거나 화면을 떠나면 멈춘다.
import { ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { fetchAnalysisStatus } from '@/api/analyses'
import { errorCode, errorMessage } from '@/api/http'
import { usePolling } from '@/composables/usePolling'
import { isInProgress } from '@/utils/analysisStatus'
import AnalysisStatusBadge from '@/components/analysis/AnalysisStatusBadge.vue'

const POLL_INTERVAL_MS = 2000

const route = useRoute()

const status = ref(null)
const loading = ref(true)
const notFound = ref(false)
const loadError = ref('')

async function poll(id, isCurrent) {
  try {
    const result = await fetchAnalysisStatus(id)
    if (!isCurrent()) return false
    status.value = result
    loadError.value = ''
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
    notFound.value = false
    loadError.value = ''
    loading.value = true
    polling.start()
  },
  { immediate: true },
)
</script>

<template>
  <section class="detail">
    <RouterLink :to="{ name: 'analyses' }" class="back">← 내 분석</RouterLink>
    <h1>분석 진행 상태</h1>

    <p v-if="notFound" class="form-error" data-message="not-found">분석을 찾을 수 없습니다.</p>
    <p v-else-if="loading" class="hint">불러오는 중…</p>

    <template v-else>
      <p v-if="loadError" class="form-error" role="alert">{{ loadError }}</p>

      <div v-if="status" class="card">
        <AnalysisStatusBadge :status="status.status" :progress="status.progress" />

        <p v-if="status.status === 'DONE'" class="done" data-message="done" role="status">
          분석이 끝났습니다.
        </p>
        <div v-else-if="status.status === 'FAILED'" class="failed" data-message="failed">
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
