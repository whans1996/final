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
// 6분. LLM 응답 상한(180초) × 형식 오류 재시도 1회보다 길다. 넘으면 서버가 멈춘 것으로 보고 조회를 멈춘다
const MAX_POLLS = 180

const props = defineProps({
  analysisId: { type: Number, required: true },
  // 상세(GET /api/analyses/{id})의 quizStatus·quizRegenerationsLeft
  quizStatus: { type: String, default: null },
  regenerationsLeft: { type: Number, default: 0 },
})
// 탭을 떠났다 돌아오면 상세의 값으로 다시 시작하므로, 바뀐 퀴즈 상태·남은 횟수를 상세에 올린다
const emit = defineEmits(['updated'])

const quiz = ref(null)
const loading = ref(true)
const missing = ref(false)
const loadError = ref('')

const status = ref(props.quizStatus)
const left = ref(props.regenerationsLeft)
const requesting = ref(false)
const regenerateError = ref('')
const slow = ref(false)
let polls = 0

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
  polls += 1
  // 조회를 더 하지 않을 때는 "오래 걸린다" 안내와 다시 확인 버튼을 보인다
  const giveUp = () => {
    slow.value = true
    return false
  }
  try {
    const result = await fetchAnalysisStatus(props.analysisId)
    if (!isCurrent()) return false
    regenerateError.value = ''
    if (result.quizStatus === 'GENERATING') return polls < MAX_POLLS || giveUp()
    status.value = result.quizStatus
    emit('updated', { quizStatus: result.quizStatus })
    if (result.quizStatus === 'READY') load()
    else missing.value = true
    return false
  } catch (error) {
    if (!isCurrent()) return false
    // 일시적인 오류일 수 있으니 메시지를 보여 주고 다음 주기에 다시 조회한다
    regenerateError.value = errorMessage(error)
    return polls < MAX_POLLS || giveUp()
  }
}

const polling = usePolling(poll, { interval: POLL_INTERVAL_MS })

function startPolling(options) {
  polls = 0
  slow.value = false
  polling.start(options)
}

function startGenerating() {
  status.value = 'GENERATING'
  loading.value = false
  missing.value = true
  // 방금 GENERATING이 되었으므로 첫 조회도 한 주기 뒤에 한다
  startPolling({ delay: POLL_INTERVAL_MS })
}

async function regenerate() {
  requesting.value = true
  regenerateError.value = ''
  try {
    const result = await regenerateQuiz(props.analysisId)
    left.value = result.quizRegenerationsLeft
    emit('updated', {
      quizStatus: result.quizStatus,
      quizRegenerationsLeft: result.quizRegenerationsLeft,
    })
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

    <div v-else-if="status === 'GENERATING' && slow" class="error-box">
      <p data-message="generating-slow">
        퀴즈를 만드는 데 예상보다 오래 걸리고 있습니다. 잠시 뒤 상태를 다시 확인해 주세요.
      </p>
      <button type="button" class="retry-button" data-action="recheck" @click="startPolling()">
        상태 다시 확인
      </button>
    </div>

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
