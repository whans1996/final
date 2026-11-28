<script setup>
// SC-07 퀴즈 탭의 "풀이 기록" (L-1b AC10). 내 풀이 회차를 최신순으로 보여 주고, 회차를 누르면 그 아래에 문항별 정오·내 답·
// 정답·해설·관련 카드를 펼친다(다시 누르면 접힘, 한 번에 하나). 상세는 처음 펼칠 때 한 번만 부르고 다시 펼치면 받아 둔 것을 쓴다.
// 풀이가 없으면 아무것도 그리지 않는다. 선택지는 저장된 순서로 보인다.
import { ref, watch } from 'vue'
import { fetchQuizAttempt, fetchQuizAttempts } from '@/api/quizzes'
import { errorMessage } from '@/api/http'
import { useLatestRequest } from '@/composables/useLatestRequest'
import AsyncState from '@/components/common/AsyncState.vue'
import QuizQuestion from '@/components/quiz/QuizQuestion.vue'
import { attemptTime } from '@/utils/quizFormat'

const props = defineProps({
  quizId: { type: Number, required: true },
})

const attempts = ref([])
const loading = ref(true)
const loadError = ref('')
const listRequest = useLatestRequest()

/** 펼친 회차 id */
const openId = ref(null)
/** attemptId → { loading, error, data } */
const details = ref({})
const detailRequest = useLatestRequest()

async function load() {
  const req = listRequest.begin()
  loading.value = true
  loadError.value = ''
  try {
    const result = await fetchQuizAttempts(props.quizId)
    if (!req.isCurrent()) return
    attempts.value = result
  } catch (error) {
    if (!req.isCurrent()) return
    loadError.value = errorMessage(error)
  } finally {
    if (req.isCurrent()) loading.value = false
  }
}

async function loadDetail(attemptId) {
  const req = detailRequest.begin()
  details.value = { ...details.value, [attemptId]: { loading: true, error: '', data: null } }
  try {
    const data = await fetchQuizAttempt(props.quizId, attemptId)
    if (!req.isCurrent()) return
    details.value = { ...details.value, [attemptId]: { loading: false, error: '', data } }
  } catch (error) {
    if (!req.isCurrent()) return
    details.value = {
      ...details.value,
      [attemptId]: { loading: false, error: errorMessage(error), data: null },
    }
  } finally {
    // 다른 회차를 펼쳐 버려진 요청은 다음에 펼칠 때 다시 부르게 비운다
    if (!req.isCurrent() && details.value[attemptId]?.loading) {
      const rest = { ...details.value }
      delete rest[attemptId]
      details.value = rest
    }
  }
}

function toggle(attemptId) {
  if (openId.value === attemptId) {
    openId.value = null
    return
  }
  openId.value = attemptId
  if (!details.value[attemptId]) loadDetail(attemptId)
}

watch(
  () => props.quizId,
  () => {
    detailRequest.invalidate()
    openId.value = null
    details.value = {}
    attempts.value = []
    load()
  },
  { immediate: true },
)
</script>

<template>
  <section
    v-if="loading || loadError || attempts.length > 0"
    class="flex w-full min-w-0 flex-col gap-3"
    data-section="attempt-history"
  >
    <h3 class="text-base">풀이 기록</h3>
    <AsyncState :loading="loading" :error="loadError" @retry="load">
      <ol class="m-0 flex list-none flex-col gap-2 p-0">
        <li
          v-for="item in attempts"
          :key="item.attemptId"
          class="min-w-0 rounded-md border border-border bg-surface"
          :data-attempt="item.attemptId"
        >
          <button
            type="button"
            class="flex w-full items-center justify-between gap-3 rounded-md px-4 py-2 text-start hover:bg-bg"
            :aria-expanded="openId === item.attemptId ? 'true' : 'false'"
            data-action="toggle-attempt"
            @click="toggle(item.attemptId)"
          >
            <span class="text-text-muted" data-part="time">{{ attemptTime(item.createdAt) }}</span>
            <span class="font-semibold" data-part="score">{{ item.score }}/{{ item.total }}</span>
          </button>

          <div
            v-if="openId === item.attemptId"
            class="border-t border-border p-4"
            data-part="detail"
          >
            <AsyncState
              :loading="details[item.attemptId]?.loading ?? true"
              :error="details[item.attemptId]?.error ?? ''"
              @retry="loadDetail(item.attemptId)"
            >
              <ol
                v-if="details[item.attemptId]?.data"
                class="m-0 flex list-none flex-col gap-4 p-0"
              >
                <QuizQuestion
                  v-for="r in details[item.attemptId].data.results"
                  :key="r.questionId"
                  :question="{
                    id: r.questionId,
                    no: r.no,
                    question: r.question,
                    choices: r.choices,
                  }"
                  :result="r"
                />
              </ol>
            </AsyncState>
          </div>
        </li>
      </ol>
    </AsyncState>
  </section>
</template>
