<script setup>
// SC-08 퀴즈 풀이 (F-14 AC10). 퀴즈를 불러와 모든 문항에 답하면 제출하고, 채점 결과·해설·복습 추가 안내를 보여 준다.
// 같은 퀴즈는 여러 번 풀 수 있다 ("다시 풀기"는 답을 비운다).
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { fetchAnalysisQuiz, submitQuizAttempt } from '@/api/quizzes'
import { errorCode, errorMessage } from '@/api/http'
import EmptyState from '@/components/common/EmptyState.vue'
import QuizQuestion from '@/components/quiz/QuizQuestion.vue'
import { planDateLabel } from '@/utils/quizFormat'

const route = useRoute()
const analysisId = computed(() => Number(route.params.id))

const quiz = ref(null)
const loading = ref(true)
const missing = ref(false)
const loadError = ref('')

/** questionId → 고른 선택지 위치 */
const answers = ref({})
const submitting = ref(false)
const submitError = ref('')
const attempt = ref(null)

const answeredCount = computed(
  () => quiz.value?.questions.filter((q) => answers.value[q.id] !== undefined).length ?? 0,
)
const canSubmit = computed(
  () => !!quiz.value && answeredCount.value === quiz.value.questions.length && !submitting.value,
)
const resultsById = computed(() =>
  Object.fromEntries((attempt.value?.results ?? []).map((r) => [r.questionId, r])),
)

async function load() {
  loading.value = true
  missing.value = false
  loadError.value = ''
  try {
    quiz.value = await fetchAnalysisQuiz(analysisId.value)
  } catch (error) {
    if (errorCode(error) === 'QUIZ_404') missing.value = true
    else loadError.value = errorMessage(error)
  } finally {
    loading.value = false
  }
}

function choose(questionId, index) {
  answers.value = { ...answers.value, [questionId]: index }
}

async function submit() {
  if (!canSubmit.value) return
  submitting.value = true
  submitError.value = ''
  try {
    attempt.value = await submitQuizAttempt(
      quiz.value.quizId,
      quiz.value.questions.map((q) => ({ questionId: q.id, selectedIndex: answers.value[q.id] })),
    )
    window.scrollTo?.({ top: 0 })
  } catch (error) {
    submitError.value = errorMessage(error)
  } finally {
    submitting.value = false
  }
}

function retry() {
  attempt.value = null
  answers.value = {}
  submitError.value = ''
}

watch(
  analysisId,
  (id) => {
    if (!Number.isInteger(id)) return
    quiz.value = null
    retry()
    load()
  },
  { immediate: true },
)
</script>

<template>
  <section class="quiz">
    <RouterLink
      :to="{ path: `/analyses/${analysisId}`, query: { tab: 'quiz' } }"
      class="back"
      data-action="back"
      >← 분석으로 돌아가기</RouterLink
    >
    <h1>코드 이해 퀴즈</h1>

    <p v-if="loading" class="hint">퀴즈를 불러오는 중…</p>

    <EmptyState v-else-if="missing" message="이 분석은 퀴즈를 만들지 못했습니다." />

    <div v-else-if="loadError" class="error-box">
      <p class="form-error" role="alert">{{ loadError }}</p>
      <button type="button" class="outline-button" data-action="retry-load" @click="load">
        다시 시도
      </button>
    </div>

    <template v-else-if="quiz">
      <div v-if="attempt" class="result-head">
        <p class="score">
          점수 <strong data-message="score">{{ attempt.score }}/{{ attempt.total }}</strong>
        </p>
        <p v-if="attempt.reviewAdded.length > 0" class="review" data-message="review-added">
          틀린 개념 {{ attempt.reviewAdded.length }}개를
          {{ planDateLabel(attempt.reviewAdded[0].planDate) }} 복습 계획에 추가했습니다.
        </p>
      </div>
      <p v-else class="hint" data-message="progress">
        {{ answeredCount }}/{{ quiz.questions.length }} 답함
      </p>

      <ol class="questions">
        <QuizQuestion
          v-for="q in quiz.questions"
          :key="q.id"
          :question="q"
          :model-value="answers[q.id] ?? null"
          :result="attempt ? resultsById[q.id] : null"
          @update:model-value="(index) => choose(q.id, index)"
        />
      </ol>

      <div class="actions">
        <template v-if="attempt">
          <button type="button" class="primary-button" data-action="retry-quiz" @click="retry">
            다시 풀기
          </button>
        </template>
        <template v-else>
          <p v-if="submitError" class="form-error" role="alert">{{ submitError }}</p>
          <button
            type="button"
            class="primary-button"
            data-action="submit"
            :disabled="!canSubmit"
            @click="submit"
          >
            {{ submitting ? '채점 중…' : '제출' }}
          </button>
        </template>
      </div>
    </template>
  </section>
</template>

<style scoped>
.quiz {
  max-width: 720px;
  margin: 0 auto;
  padding: var(--space-8) 0;
}

.quiz h1 {
  overflow-wrap: anywhere;
}

.back {
  font-size: 0.9em;
}

.questions {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  margin: var(--space-4) 0 0;
  padding: 0;
  list-style: none;
}

.result-head {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.result-head p {
  margin: 0;
}

.score strong {
  font-size: 1.4em;
}

.review {
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius);
  background: var(--color-background);
}

.actions {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-3);
  margin-top: var(--space-6);
}

.actions p {
  margin: 0;
}

.primary-button {
  padding: var(--space-2) var(--space-6);
  border: none;
  border-radius: var(--radius);
  color: #fff;
  background: var(--color-primary);
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.primary-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.outline-button {
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius);
  color: var(--color-primary);
  background: var(--color-surface);
  font: inherit;
  cursor: pointer;
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
</style>
