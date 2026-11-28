<script setup>
// SC-07 "면접 준비" 탭 (I-1a AC12~AC14). 주인 + DONE 분석에서만 보인다 (부모가 정한다).
// NONE이면 안내 + 만들기, PENDING이면 3초마다 다시 조회해 끝나면 멈춘다 (탭을 떠나면 unmount로 멈춤).
// 120회(6분) 넘게 PENDING이면 멈추고 "상태 다시 확인"을 보인다. 조회는 useLatestRequest로 늦은 응답을 버린다.
import { computed, onMounted, ref } from 'vue'
import { fetchInterview, requestInterview } from '@/api/interviews'
import { errorMessage } from '@/api/http'
import { useLatestRequest } from '@/composables/useLatestRequest'
import { usePolling } from '@/composables/usePolling'
import { groupByCategory } from '@/utils/interviewFormat'
import AsyncState from '@/components/common/AsyncState.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import InterviewQuestionCard from '@/components/interview/InterviewQuestionCard.vue'

const POLL_INTERVAL_MS = 3000
const MAX_PENDING_POLLS = 120

const props = defineProps({
  analysisId: { type: Number, required: true },
})

const interview = ref(null)
const loading = ref(true)
const loadError = ref('')
const requestError = ref('')
const submitting = ref(false)
const timedOut = ref(false)
let pendingPolls = 0

const groups = computed(() => groupByCategory(interview.value?.questions ?? []))
const latest = useLatestRequest()

async function poll(isCurrent) {
  const request = latest.begin()
  try {
    const result = await fetchInterview(props.analysisId, { signal: request.signal })
    if (!request.isCurrent() || !isCurrent()) return false
    interview.value = result
    loadError.value = ''
    if (result.status !== 'PENDING') return false
    pendingPolls += 1
    if (pendingPolls >= MAX_PENDING_POLLS) {
      timedOut.value = true
      return false
    }
    return true
  } catch (error) {
    if (request.isCurrent() && isCurrent()) loadError.value = errorMessage(error)
    return false
  } finally {
    if (request.isCurrent()) loading.value = false
  }
}

const polling = usePolling(poll, { interval: POLL_INTERVAL_MS })

function reload() {
  pendingPolls = 0
  timedOut.value = false
  requestError.value = ''
  polling.start()
}

async function create() {
  submitting.value = true
  requestError.value = ''
  try {
    const result = await requestInterview(props.analysisId)
    latest.invalidate()
    interview.value = result
    pendingPolls = 0
    timedOut.value = false
    // 방금 받은 응답이 최신이므로 다음 조회는 한 주기 뒤에 한다
    polling.start({ delay: POLL_INTERVAL_MS })
  } catch (error) {
    requestError.value = errorMessage(error)
  } finally {
    submitting.value = false
  }
}

onMounted(reload)
</script>

<template>
  <div class="flex flex-col gap-4" data-section="interview">
    <div
      v-if="requestError"
      class="flex flex-col items-start gap-2 rounded-md border border-danger/30 bg-danger-soft px-4 py-3"
      role="alert"
      data-error="request"
    >
      <p class="text-danger">{{ requestError }}</p>
      <BaseButton variant="secondary" size="sm" data-action="retry-load" @click="reload">
        다시 시도
      </BaseButton>
    </div>

    <AsyncState
      :loading="loading && !interview"
      :error="interview ? '' : loadError"
      @retry="reload"
    >
      <template v-if="interview">
        <!-- 이미 받은 상태가 있는데 다시 조회(폴링)가 실패하면 폴링이 멈추므로 다시 시도를 준다 (AC14, Codex 리뷰) -->
        <div
          v-if="loadError"
          class="flex flex-col items-start gap-2 rounded-md border border-danger/30 bg-danger-soft px-4 py-3"
          role="alert"
          data-error="load"
        >
          <p class="text-danger">{{ loadError }}</p>
          <BaseButton variant="secondary" size="sm" data-action="retry-poll" @click="reload">
            다시 시도
          </BaseButton>
        </div>

        <div
          v-if="interview.status === 'NONE'"
          class="flex flex-col items-start gap-3"
          data-state="none"
        >
          <p class="text-text-muted">
            이 프로젝트 코드로 면접 예상 질문을 만들어요. 질문마다 근거 코드와 답의 핵심 포인트를
            함께 보여 드려요.
          </p>
          <BaseButton :loading="submitting" data-action="create" @click="create">
            면접 질문 만들기
          </BaseButton>
        </div>

        <div
          v-else-if="interview.status === 'PENDING'"
          class="flex flex-col items-start gap-3"
          data-state="pending"
        >
          <p role="status" class="text-text-muted">면접 질문을 만드는 중입니다…</p>
          <template v-if="timedOut">
            <p class="text-warning" data-message="timeout">예상보다 오래 걸리고 있습니다.</p>
            <BaseButton variant="secondary" size="sm" data-action="recheck" @click="reload">
              상태 다시 확인
            </BaseButton>
          </template>
        </div>

        <div
          v-else-if="interview.status === 'FAILED'"
          class="flex flex-col items-start gap-3"
          data-state="failed"
        >
          <p class="text-danger">{{ interview.errorMessage }}</p>
          <BaseButton :loading="submitting" data-action="retry-failed" @click="create">
            다시 만들기
          </BaseButton>
        </div>

        <template v-else-if="interview.status === 'DONE'">
          <div class="flex flex-wrap items-center gap-3" data-state="done">
            <BaseButton
              variant="secondary"
              size="sm"
              :loading="submitting"
              :disabled="interview.regenerationsLeft <= 0"
              data-action="regenerate"
              @click="create"
            >
              다시 만들기 (남은 {{ interview.regenerationsLeft }}회)
            </BaseButton>
            <p
              v-if="interview.regenerationsLeft <= 0"
              class="text-sm text-text-muted"
              data-message="no-regenerations"
            >
              다시 만들기 횟수를 모두 썼습니다.
            </p>
          </div>
          <section
            v-for="group in groups"
            :key="group.category"
            class="flex flex-col gap-3"
            :data-category="group.category"
          >
            <h3 class="text-lg">{{ group.label }}</h3>
            <InterviewQuestionCard v-for="q in group.items" :key="q.id" :question="q" />
          </section>
        </template>
      </template>
    </AsyncState>
  </div>
</template>
