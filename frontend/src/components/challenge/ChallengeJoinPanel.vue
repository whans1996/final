<script setup>
// SC-13 챌린지 참여 영역 (F-25 AC13). 참여·취소 결과(ParticipationResponse)는 change로 올려 상세 화면이 반영한다.
// 참여 중인 진행 중 챌린지를 아직 달성하지 않았으면 목표를 채우러 갈 링크를 함께 보인다 (#211).
// 취소는 브라우저 confirm 대신 화면 안 대화상자로 확인한다. 409(종료·달성)와 그 밖의 에러는 버튼 아래에 서버 메시지를 보인다.
import { computed, nextTick, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { cancelChallenge, joinChallenge } from '@/api/challenges'
import { errorMessage } from '@/api/http'
import ChallengeProgressBar from '@/components/challenge/ChallengeProgressBar.vue'
import { goalAction } from '@/components/challenge/labels'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  /** ChallengeDetailResponse */
  challenge: { type: Object, required: true },
})

const emit = defineEmits(['change'])

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const busy = ref(false)
const error = ref('')
const confirming = ref(false)
// 대화상자가 열리면 되돌릴 수 없는 취소 대신 "닫기"에 포커스를 둔다
const closeButton = ref(null)

const participation = computed(() => props.challenge.participation)
const ended = computed(() => props.challenge.status === 'ENDED')
const action = computed(() =>
  props.challenge.status === 'ONGOING' && participation.value && !participation.value.completedAt
    ? goalAction(props.challenge.goalType)
    : null,
)

/** '2026-11-24T10:15:00+09:00' → '11월 24일' (서버가 준 서울 날짜 그대로) */
function monthDay(value) {
  const [, m, d] = value.slice(0, 10).split('-').map(Number)
  return `${m}월 ${d}일`
}

async function run(action) {
  if (busy.value) return
  busy.value = true
  error.value = ''
  try {
    emit('change', await action(props.challenge.id))
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    busy.value = false
  }
}

function join() {
  if (!auth.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  run(joinChallenge)
}

async function askCancel() {
  error.value = ''
  confirming.value = true
  await nextTick()
  closeButton.value?.focus()
}

function closeDialog() {
  if (!busy.value) confirming.value = false
}

async function confirmCancel() {
  await run(cancelChallenge)
  confirming.value = false
}
</script>

<template>
  <div class="join-panel">
    <template v-if="participation">
      <ChallengeProgressBar
        :progress="participation.progress"
        :goal-value="participation.goalValue"
      />
      <span v-if="participation.completedAt" class="completed" data-badge="completed">
        달성 완료 · {{ monthDay(participation.completedAt) }}
      </span>
      <p v-else-if="ended" class="hint">종료된 챌린지입니다.</p>
      <RouterLink v-if="action" :to="action.to" class="primary go-goal" data-action="go-goal">
        {{ action.label }}
      </RouterLink>
      <button
        v-if="!participation.completedAt && !ended"
        type="button"
        class="outline"
        data-action="cancel"
        :disabled="busy"
        @click="askCancel"
      >
        참여 취소
      </button>
    </template>
    <p v-else-if="ended" class="hint">종료된 챌린지입니다.</p>
    <button v-else type="button" class="primary" data-action="join" :disabled="busy" @click="join">
      참여하기
    </button>

    <p v-if="error" class="form-error" role="alert" data-testid="join-error">{{ error }}</p>

    <div v-if="confirming" class="backdrop" @click.self="closeDialog">
      <div
        class="dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="cancel-dialog-title"
        @keydown.esc="closeDialog"
      >
        <h2 id="cancel-dialog-title">참여를 취소할까요?</h2>
        <p class="target">{{ challenge.title }}</p>
        <p>지금까지의 달성도는 사라지고, 다시 참여하면 시작일부터 다시 셉니다.</p>
        <div class="dialog-actions">
          <button
            ref="closeButton"
            type="button"
            data-action="close-dialog"
            :disabled="busy"
            @click="closeDialog"
          >
            닫기
          </button>
          <button
            type="button"
            class="danger"
            data-action="confirm-cancel"
            :disabled="busy"
            @click="confirmCancel"
          >
            참여 취소
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.join-panel {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: var(--space-3);
}

button {
  padding: var(--space-2) var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  font: inherit;
  cursor: pointer;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.primary {
  border-color: var(--color-primary);
  color: #fff;
  background: var(--color-primary);
  font-weight: 700;
}

.outline {
  align-self: flex-start;
}

.go-goal {
  align-self: flex-start;
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius);
  text-decoration: none;
}

.completed {
  align-self: flex-start;
  padding: var(--space-1) var(--space-3);
  border-radius: 999px;
  color: #047857;
  background: #d1fae5;
  font-weight: 700;
}

.hint {
  margin: 0;
  color: var(--color-text-muted);
}

.form-error {
  margin: 0;
  color: var(--color-danger);
}

.backdrop {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-4);
  background: rgb(0 0 0 / 40%);
}

.dialog {
  width: min(420px, 100%);
  padding: var(--space-6);
  border-radius: var(--radius);
  background: var(--color-surface);
}

.dialog h2 {
  margin: 0 0 var(--space-3);
  font-size: 1.15em;
}

.dialog p {
  margin: 0 0 var(--space-3);
}

.dialog .target {
  font-weight: 700;
  overflow-wrap: anywhere;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
  margin-top: var(--space-4);
}

.dialog-actions .danger {
  border-color: var(--color-danger);
  color: #fff;
  background: var(--color-danger);
  font-weight: 700;
}
</style>
