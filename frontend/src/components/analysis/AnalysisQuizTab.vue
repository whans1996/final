<script setup>
// SC-07 "퀴즈" 탭 (F-14 AC9). 퀴즈를 1회 불러와 문항 수와 지난 풀이를 보여 주고, SC-08 풀이 화면으로 보낸다.
// 생성에 실패했으면(quizStatus FAILED) 남은 횟수만큼 다시 만들 수 있고, 만드는 중(GENERATING)이면 상태를 2초마다 조회해
// READY면 퀴즈를 불러오고 FAILED면 실패 안내로 돌아간다. 탭을 떠나면 폴링을 멈춘다 (L-1a AC10).
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchAnalysisQuiz, regenerateQuiz } from '@/api/quizzes'
import { fetchAnalysisStatus } from '@/api/analyses'
import { errorCode, errorMessage } from '@/api/http'
import { usePolling } from '@/composables/usePolling'
import EmptyState from '@/components/common/EmptyState.vue'
import { attemptTime } from '@/utils/quizFormat'

const POLL_INTERVAL_MS = 2000

const props = defineProps({
  analysisId: { type: Number, required: true },
  // 상세(GET /api/analyses/{id})의 quizStatus·quizRegenerationsLeft
  quizStatus: { type: String, default: null },
  regenerationsLeft: { type: Number, default: 0 },
})

const quiz = ref(null)
const loading = ref(true)
const missing = ref(false)
const loadError = ref('')

const status = ref(props.quizStatus)
const left = ref(props.regenerationsLeft)
const requesting = ref(false)
const regenerateError = ref('')

async function load() {
  loading.value = true
  missing.value = false
  loadError.value = ''
  try {
    quiz.value = await fetchAnalysisQuiz(props.analysisId)
  } catch (error) {
    if (errorCode(error) === 'QUIZ_404') missing.value = true
    else loadError.value = errorMessage(error)
  } finally {
    loading.value = false
  }
}

async function poll(isCurrent) {
  try {
    const result = await fetchAnalysisStatus(props.analysisId)
    if (!isCurrent()) return false
    regenerateError.value = ''
    if (result.quizStatus === 'GENERATING') return true
    status.value = result.quizStatus
    if (result.quizStatus === 'READY') load()
    else missing.value = true
    return false
  } catch (error) {
    if (!isCurrent()) return false
    // 일시적인 오류일 수 있으니 메시지를 보여 주고 다음 주기에 다시 조회한다
    regenerateError.value = errorMessage(error)
    return true
  }
}

const polling = usePolling(poll, { interval: POLL_INTERVAL_MS })

function startGenerating() {
  status.value = 'GENERATING'
  loading.value = false
  missing.value = true
  // 방금 GENERATING이 되었으므로 첫 조회도 한 주기 뒤에 한다
  polling.start({ delay: POLL_INTERVAL_MS })
}

async function regenerate() {
  requesting.value = true
  regenerateError.value = ''
  try {
    const result = await regenerateQuiz(props.analysisId)
    left.value = result.quizRegenerationsLeft
    startGenerating()
  } catch (error) {
    regenerateError.value = errorMessage(error)
  } finally {
    requesting.value = false
  }
}

onMounted(() => {
  if (status.value === 'GENERATING') startGenerating()
  else load()
})
</script>

<template>
  <div class="quiz-tab">
    <p v-if="loading" class="hint">퀴즈를 불러오는 중…</p>

    <p v-else-if="status === 'GENERATING'" class="hint" role="status" data-message="generating">
      퀴즈를 만드는 중입니다…
    </p>

    <div v-else-if="missing && status === 'FAILED'" class="error-box">
      <p>퀴즈를 만들지 못했습니다.</p>
      <button
        v-if="left > 0"
        type="button"
        class="retry-button"
        data-action="regenerate"
        :disabled="requesting"
        @click="regenerate"
      >
        퀴즈 다시 만들기 (남은 {{ left }}회)
      </button>
      <p v-else class="hint" data-message="regenerations-used">다시 만들기 횟수를 모두 썼습니다.</p>
    </div>

    <EmptyState v-else-if="missing" message="이 분석은 퀴즈를 만들지 못했습니다." />

    <div v-else-if="loadError" class="error-box">
      <p class="form-error" role="alert">{{ loadError }}</p>
      <button type="button" class="retry-button" data-action="retry-load" @click="load">
        다시 시도
      </button>
    </div>

    <div v-else-if="quiz" class="summary">
      <p class="count" data-message="count">객관식 {{ quiz.questions.length }}문항</p>
      <p v-if="quiz.lastAttempt" class="hint" data-message="last-attempt">
        지난 풀이 {{ quiz.lastAttempt.score }}/{{ quiz.lastAttempt.total }} ·
        {{ attemptTime(quiz.lastAttempt.createdAt) }}
      </p>
      <RouterLink :to="`/analyses/${analysisId}/quiz`" class="start" data-action="start-quiz">
        {{ quiz.lastAttempt ? '다시 풀기' : '퀴즈 풀기' }}
      </RouterLink>
    </div>

    <p v-if="regenerateError" class="form-error" role="alert" data-error="regenerate">
      {{ regenerateError }}
    </p>
  </div>
</template>

<style scoped>
.quiz-tab {
  min-width: 0;
}

.summary {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-3);
}

.summary p {
  margin: 0;
}

.count {
  font-weight: 700;
}

.start {
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius);
  color: #fff;
  background: var(--color-primary);
  font-weight: 700;
  text-decoration: none;
}

.hint {
  color: var(--color-text-muted);
}

.form-error {
  color: var(--color-danger);
}

.error-box {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-3);
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
</style>
