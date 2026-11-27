<script setup>
// SC-10 카드 상세 헤더의 ♥ 찜 버튼 (F-12 AC12~AC14). 누르면 즉시 바꾸고(낙관적 갱신) 응답 값으로 맞추며, 실패하면 되돌린다.
// 비로그인은 요청 없이 로그인 화면으로 보낸다. 결과는 change({ wished, wishCount })로 올린다.
import { ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { unwishConcept, wishConcept } from '@/api/concepts'
import { errorMessage } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { planDateLabel } from '@/utils/quizFormat'
import { useLatestRequest } from '@/composables/useLatestRequest'

const props = defineProps({
  conceptId: { type: Number, required: true },
  wished: { type: Boolean, required: true },
  wishCount: { type: Number, required: true },
})
const emit = defineEmits(['change'])

const auth = useAuthStore()
const router = useRouter()

const on = ref(props.wished)
const count = ref(props.wishCount)
const pending = ref(false)
const planDate = ref('')
const error = ref('')

// 누른 카드에서 :id가 바뀌거나 사라지면 그 응답은 새 카드에 반영하지 않는다 (V-4 AC9)
const latest = useLatestRequest()

// 관련 카드로 :id가 바뀌거나 부모가 값을 맞추면 새 값으로 (AC12). 카드가 바뀌면 이전 카드의 찜 요청은 버리고 잠금을 푼다
watch(
  () => [props.conceptId, props.wished, props.wishCount],
  ([id, wished, wishCount], [prevId] = []) => {
    on.value = wished
    count.value = wishCount
    if (id !== prevId) {
      latest.invalidate()
      pending.value = false
      planDate.value = ''
      error.value = ''
    }
  },
)

async function toggle() {
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: `/concepts/${props.conceptId}` } })
    return
  }
  const before = { wished: on.value, wishCount: count.value }
  const next = !before.wished
  on.value = next
  count.value = Math.max(0, before.wishCount + (next ? 1 : -1))
  const request = latest.begin()
  pending.value = true
  error.value = ''
  planDate.value = ''
  try {
    const result = next ? await wishConcept(props.conceptId) : await unwishConcept(props.conceptId)
    if (!request.isCurrent()) return
    on.value = result.wished
    count.value = result.wishCount
    planDate.value = result.reviewPlanDate ?? ''
    emit('change', { wished: result.wished, wishCount: result.wishCount })
  } catch (e) {
    if (!request.isCurrent()) return
    on.value = before.wished
    count.value = before.wishCount
    error.value = errorMessage(e)
  } finally {
    if (request.isCurrent()) pending.value = false
  }
}
</script>

<template>
  <span class="wish-box">
    <button
      type="button"
      class="wish-button"
      :class="{ on }"
      :disabled="pending"
      :aria-pressed="on ? 'true' : 'false'"
      :aria-label="`${on ? '찜 해제' : '찜하기'} ${count}`"
      data-action="toggle-wish"
      @click="toggle"
    >
      {{ on ? '♥' : '♡' }} {{ count }}
    </button>
    <span v-if="planDate" class="note" data-message="wish-review-plan">
      {{ planDateLabel(planDate) }} 복습 계획에 추가했습니다.
      <RouterLink to="/review-plans">복습 계획 보기</RouterLink>
    </span>
    <span v-else-if="error" class="form-error" role="alert" data-message="wish-error">
      {{ error }}
    </span>
  </span>
</template>

<style scoped>
/* 버튼과 안내를 부모(헤더 .stats, flex-wrap) 줄에 바로 올린다. 안내가 버튼 상자를 넓혀 옆 평점을 밀지 않고 아래 한 줄을 차지한다 */
.wish-box {
  display: contents;
}

.wish-button {
  min-height: 32px;
  min-width: 32px;
  padding: 0 var(--space-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  color: var(--color-text-muted);
  font: inherit;
  cursor: pointer;
}

.wish-button.on {
  border-color: var(--color-danger);
  color: var(--color-danger);
}

.wish-button:disabled {
  cursor: default;
  opacity: 0.7;
}

.note,
.form-error {
  flex-basis: 100%;
  order: 1;
  font-size: 0.85em;
}

.form-error {
  color: var(--color-danger);
}
</style>
