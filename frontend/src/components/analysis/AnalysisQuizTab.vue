<script setup>
// SC-07 "퀴즈" 탭 (F-14 AC9). 퀴즈를 1회 불러와 문항 수와 지난 풀이를 보여 주고, SC-08 풀이 화면으로 보낸다.
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchAnalysisQuiz } from '@/api/quizzes'
import { errorCode, errorMessage } from '@/api/http'
import EmptyState from '@/components/common/EmptyState.vue'
import { attemptTime } from '@/utils/quizFormat'

const props = defineProps({
  analysisId: { type: Number, required: true },
})

const quiz = ref(null)
const loading = ref(true)
const missing = ref(false)
const loadError = ref('')

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

onMounted(load)
</script>

<template>
  <div class="quiz-tab">
    <p v-if="loading" class="hint">퀴즈를 불러오는 중…</p>

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
