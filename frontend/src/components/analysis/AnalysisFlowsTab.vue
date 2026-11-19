<script setup>
// SC-07 "실행 흐름" 탭 (F-07 AC14~AC16). 개요 apis에서 남은 개수(3 − FAILED가 아닌 흐름)까지 골라 요청하고,
// PENDING이 있으면 3초마다 목록을 다시 불러 모두 끝나면 멈춘다 (탭을 떠나면 unmount로 멈춤).
import { computed, onMounted, ref } from 'vue'
import { fetchAnalysisFlows, requestAnalysisFlows } from '@/api/analyses'
import { errorMessage } from '@/api/http'
import { usePolling } from '@/composables/usePolling'
import AnalysisFlowCard from '@/components/analysis/AnalysisFlowCard.vue'
import FilePath from '@/components/analysis/FilePath.vue'
import MethodBadge from '@/components/analysis/MethodBadge.vue'

const POLL_INTERVAL_MS = 3000
const MAX_FLOWS = 3
const STATUS_LABELS = { PENDING: '생성 중', DONE: '완료', FAILED: '실패' }

const props = defineProps({
  analysisId: { type: Number, required: true },
  apis: { type: Array, required: true },
})

const emit = defineEmits(['select-file'])

const flows = ref(null)
const loadError = ref('')
const selected = ref([])
const submitting = ref(false)
const requestError = ref('')
let lastRequest = []

const key = (item) => `${item.method} ${item.path}`

const flowByKey = computed(() => new Map((flows.value ?? []).map((f) => [key(f), f])))
const remaining = computed(
  () => MAX_FLOWS - (flows.value ?? []).filter((f) => f.status !== 'FAILED').length,
)
const hasPending = (list) => list.some((f) => f.status === 'PENDING')

function isDisabled(api) {
  const k = key(api)
  if (flowByKey.value.has(k) || submitting.value) return true
  return !selected.value.includes(k) && selected.value.length >= remaining.value
}

async function poll(isCurrent) {
  try {
    const result = await fetchAnalysisFlows(props.analysisId)
    if (!isCurrent()) return false
    flows.value = result
    loadError.value = ''
    return hasPending(result)
  } catch (error) {
    if (isCurrent()) loadError.value = errorMessage(error)
    return false
  }
}

const polling = usePolling(poll, { interval: POLL_INTERVAL_MS })

onMounted(() => polling.start())

async function submit(endpoints) {
  lastRequest = endpoints
  submitting.value = true
  requestError.value = ''
  try {
    const result = await requestAnalysisFlows(props.analysisId, endpoints)
    flows.value = result
    loadError.value = ''
    const made = new Set(result.map(key))
    selected.value = selected.value.filter((k) => !made.has(k))
    // 방금 받은 목록이 최신이므로 다음 조회는 한 주기 뒤에 한다
    if (hasPending(result)) polling.start({ delay: POLL_INTERVAL_MS })
  } catch (error) {
    requestError.value = errorMessage(error)
  } finally {
    submitting.value = false
  }
}

function requestSelected() {
  const endpoints = props.apis
    .filter((api) => selected.value.includes(key(api)))
    .map(({ method, path }) => ({ method, path }))
  submit(endpoints)
}

function retryFlow(flow) {
  submit([{ method: flow.method, path: flow.path }])
}
</script>

<template>
  <div class="flows-tab">
    <p v-if="flows === null && !loadError" class="hint">실행 흐름을 불러오는 중…</p>

    <div v-if="loadError" class="error-box">
      <p class="form-error" role="alert">{{ loadError }}</p>
      <button type="button" class="retry-button" data-action="retry-load" @click="polling.start()">
        다시 시도
      </button>
    </div>

    <div v-if="flows !== null" class="layout">
      <section class="picker" aria-label="API 선택">
        <h2>API 고르기</h2>
        <p v-if="apis.length === 0" class="hint" data-message="no-apis">
          탐지된 API가 없어 실행 흐름을 만들 수 없습니다.
        </p>
        <template v-else>
          <p v-if="remaining <= 0" class="hint" data-message="limit">
            실행 흐름은 분석 하나에 3개까지 만들 수 있습니다.
          </p>
          <p v-else class="hint">
            요청이 들어와 응답이 나갈 때까지의 흐름을 설명합니다. 실행 흐름을 {{ remaining }}개 더
            만들 수 있습니다.
          </p>

          <ul class="apis">
            <li v-for="api in apis" :key="key(api)" class="api" :data-api-row="key(api)">
              <label>
                <input
                  v-model="selected"
                  type="checkbox"
                  :value="key(api)"
                  :data-api="key(api)"
                  :disabled="isDisabled(api)"
                />
                <MethodBadge :method="api.method" />
                <span class="path"><FilePath :path="api.path" /></span>
                <span
                  v-if="flowByKey.has(key(api))"
                  class="status"
                  :data-status="flowByKey.get(key(api)).status"
                >
                  {{ STATUS_LABELS[flowByKey.get(key(api)).status] }}
                </span>
              </label>
            </li>
          </ul>

          <button
            type="button"
            class="submit"
            data-action="request-flows"
            :disabled="selected.length === 0 || submitting"
            @click="requestSelected"
          >
            실행 흐름 만들기 ({{ selected.length }}개)
          </button>
        </template>

        <div v-if="requestError" class="error-box">
          <p class="form-error" role="alert">{{ requestError }}</p>
          <button
            type="button"
            class="retry-button"
            data-action="retry-request"
            :disabled="submitting"
            @click="submit(lastRequest)"
          >
            다시 시도
          </button>
        </div>
      </section>

      <section class="cards" aria-label="실행 흐름">
        <p v-if="flows.length === 0" class="hint">아직 만든 실행 흐름이 없습니다.</p>
        <AnalysisFlowCard
          v-for="flow in flows"
          :key="flow.id"
          :flow="flow"
          :busy="submitting"
          @retry="retryFlow"
          @select-file="emit('select-file', $event)"
        />
      </section>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 3fr);
  gap: var(--space-6);
  align-items: start;
}

.picker,
.cards {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  min-width: 0;
}

.picker h2 {
  margin: 0;
  font-size: 1.05em;
}

.apis {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin: 0;
  padding: 0;
  list-style: none;
}

.api label {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
  cursor: pointer;
}

.api input {
  flex: none;
}

.path {
  min-width: 0;
  font-size: 0.9em;
}

.status {
  flex: none;
  margin-left: auto;
  padding: 0 var(--space-2);
  border-radius: 999px;
  font-size: 0.8em;
  color: var(--color-text-muted);
  background: var(--color-border);
}

.status[data-status='DONE'] {
  color: #065f46;
  background: #d1fae5;
}

.status[data-status='FAILED'] {
  color: #991b1b;
  background: #fee2e2;
}

.submit {
  align-self: flex-start;
  padding: var(--space-2) var(--space-4);
  border: none;
  border-radius: var(--radius);
  color: #fff;
  background: var(--color-primary);
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.submit:disabled {
  opacity: 0.5;
  cursor: default;
}

.error-box {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
}

.error-box .form-error {
  margin: 0;
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

.hint {
  margin: 0;
  color: var(--color-text-muted);
}

@media (max-width: 768px) {
  .layout {
    grid-template-columns: 1fr;
  }
}
</style>
