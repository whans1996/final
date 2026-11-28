<script setup>
// SC-08 퀴즈 풀이 (F-14 AC10). 퀴즈를 불러와 모든 문항에 답하면 제출하고, 채점 결과·해설·복습 추가 안내를 보여 준다.
// 같은 퀴즈는 여러 번 풀 수 있다. "다시 풀기"는 답을 비우고 문항마다 선지 순서를 직전과 다르게 섞는다 (L-1b AC11).
// 처음 연 화면은 저장된 순서이고, 답은 늘 저장된 위치로 들고 있어 그대로 제출한다.
// 채점 결과에는 진행 중인 퀴즈 챌린지 달성도를 함께 보인다 (#211).
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { fetchAnalysisQuiz, submitQuizAttempt } from '@/api/quizzes'
import { errorCode, errorMessage } from '@/api/http'
import { useLatestRequest } from '@/composables/useLatestRequest'
import AsyncState from '@/components/common/AsyncState.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import ChallengeProgressNotice from '@/components/challenge/ChallengeProgressNotice.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import QuizQuestion from '@/components/quiz/QuizQuestion.vue'
import { planDateLabel } from '@/utils/quizFormat'
import { identityOrder, reshuffle } from '@/utils/quizShuffle'

const route = useRoute()
const analysisId = computed(() => Number(route.params.id))
// 팀 공유 분석에서 들어오면(?team=T, F-20b AC13) 팀으로, 오답 노트에서 들어오면(?from=wrong-notes, L-1b AC12)
// 오답 노트로 돌아간다. 그 밖에는 분석 상세의 퀴즈 탭이다. team이 숫자가 아니면 무시한다.
const teamId = computed(() => {
  const team = route.query.team
  return typeof team === 'string' && /^\d+$/.test(team) ? Number(team) : null
})
const backLink = computed(() => {
  if (route.query.from === 'wrong-notes') return { to: '/wrong-notes', label: '오답 노트로' }
  if (teamId.value !== null) return { to: `/teams/${teamId.value}`, label: '팀으로' }
  return {
    to: { path: `/analyses/${analysisId.value}`, query: { tab: 'quiz' } },
    label: '분석으로',
  }
})

const quiz = ref(null)
const loading = ref(true)
const missing = ref(false)
const loadError = ref('')
const loadRequest = useLatestRequest()
const submitRequest = useLatestRequest()

/** questionId → 고른 선택지의 저장된 위치 */
const answers = ref({})
/** questionId → 표시 순서 (order[화면 위치] = 저장된 위치) */
const orders = ref({})
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
  const req = loadRequest.begin()
  loading.value = true
  missing.value = false
  loadError.value = ''
  try {
    const result = await fetchAnalysisQuiz(analysisId.value)
    if (!req.isCurrent()) return
    quiz.value = result
    orders.value = Object.fromEntries(
      result.questions.map((q) => [q.id, identityOrder(q.choices.length)]),
    )
  } catch (error) {
    if (!req.isCurrent()) return
    if (errorCode(error) === 'QUIZ_404') missing.value = true
    else loadError.value = errorMessage(error)
  } finally {
    if (req.isCurrent()) loading.value = false
  }
}

function choose(questionId, index) {
  answers.value = { ...answers.value, [questionId]: index }
}

async function submit() {
  if (!canSubmit.value) return
  const req = submitRequest.begin()
  submitting.value = true
  submitError.value = ''
  try {
    const result = await submitQuizAttempt(
      quiz.value.quizId,
      quiz.value.questions.map((q) => ({ questionId: q.id, selectedIndex: answers.value[q.id] })),
    )
    if (!req.isCurrent()) return
    attempt.value = result
    window.scrollTo?.({ top: 0 })
  } catch (error) {
    if (!req.isCurrent()) return
    submitError.value = errorMessage(error)
  } finally {
    if (req.isCurrent()) submitting.value = false
  }
}

function clearAnswers() {
  attempt.value = null
  answers.value = {}
  submitError.value = ''
}

function retry() {
  clearAnswers()
  orders.value = Object.fromEntries(
    Object.entries(orders.value).map(([id, order]) => [id, reshuffle(order)]),
  )
  window.scrollTo?.({ top: 0 })
}

watch(
  analysisId,
  (id) => {
    if (!Number.isInteger(id)) return
    submitRequest.invalidate()
    submitting.value = false
    quiz.value = null
    orders.value = {}
    clearAnswers()
    load()
  },
  { immediate: true },
)
</script>

<template>
  <section class="mx-auto flex max-w-[720px] flex-col gap-4 py-8">
    <RouterLink :to="backLink.to" class="self-start text-sm" data-action="back"
      >← {{ backLink.label }} 돌아가기</RouterLink
    >
    <h1 class="break-words">코드 이해 퀴즈</h1>

    <EmptyState v-if="!loading && missing" message="이 분석은 퀴즈를 만들지 못했습니다." />

    <AsyncState v-else :loading="loading" :error="loadError" @retry="load">
      <template v-if="quiz">
        <div v-if="attempt" class="flex flex-col gap-2">
          <p>
            점수
            <strong class="text-xl" data-message="score"
              >{{ attempt.score }}/{{ attempt.total }}</strong
            >
          </p>
          <p
            v-if="attempt.reviewAdded.length > 0"
            class="rounded-md bg-bg px-4 py-3"
            data-message="review-added"
          >
            틀린 개념 {{ attempt.reviewAdded.length }}개를
            {{ planDateLabel(attempt.reviewAdded[0].planDate) }} 복습 계획에 추가했습니다.
          </p>
          <RouterLink
            v-if="attempt.reviewAdded.length > 0"
            to="/review-plans"
            class="self-start text-sm"
            data-action="review-plans"
            >복습 계획 보기</RouterLink
          >
          <ChallengeProgressNotice goal-type="QUIZ_COUNT" :refresh-key="attempt.attemptId" />
        </div>
        <p v-else class="text-text-muted" data-message="progress">
          {{ answeredCount }}/{{ quiz.questions.length }} 답함
        </p>

        <ol class="m-0 flex list-none flex-col gap-4 p-0">
          <QuizQuestion
            v-for="q in quiz.questions"
            :key="q.id"
            :question="q"
            :order="orders[q.id]"
            :model-value="answers[q.id] ?? null"
            :result="attempt ? resultsById[q.id] : null"
            @update:model-value="(index) => choose(q.id, index)"
          />
        </ol>

        <div class="mt-2 flex flex-col items-start gap-3">
          <BaseButton v-if="attempt" data-action="retry-quiz" @click="retry">다시 풀기</BaseButton>
          <template v-else>
            <p v-if="submitError" class="text-danger" role="alert">{{ submitError }}</p>
            <BaseButton
              data-action="submit"
              :disabled="!canSubmit"
              :loading="submitting"
              @click="submit"
            >
              {{ submitting ? '채점 중…' : '제출' }}
            </BaseButton>
          </template>
        </div>
      </template>
    </AsyncState>
  </section>
</template>
