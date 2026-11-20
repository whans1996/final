<script setup>
// SC-10 카드 상세의 "복습에 추가" (F-15 AC11). 내일 날짜로 직접 추가(MANUAL)하고 결과를 버튼 옆에 보여 준다. 비로그인은 숨긴다.
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { addReviewPlan } from '@/api/reviewPlans'
import { errorMessage } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { planDateLabel } from '@/utils/quizFormat'

const props = defineProps({
  conceptId: { type: Number, required: true },
})

const auth = useAuthStore()
const adding = ref(false)
const addedDate = ref('')
const error = ref('')

async function add() {
  adding.value = true
  error.value = ''
  addedDate.value = ''
  try {
    const plan = await addReviewPlan({ conceptId: props.conceptId })
    addedDate.value = plan.planDate
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    adding.value = false
  }
}
</script>

<template>
  <div v-if="auth.isLoggedIn" class="review-add">
    <button type="button" class="add" :disabled="adding" data-action="add-review-plan" @click="add">
      복습에 추가
    </button>
    <p v-if="addedDate" class="done" data-message="review-plan-added">
      {{ planDateLabel(addedDate) }} 복습 계획에 추가했습니다.
      <RouterLink to="/review-plans" data-action="review-plans">복습 계획 보기</RouterLink>
    </p>
    <p v-else-if="error" class="form-error" role="alert" data-message="review-plan-error">
      {{ error }}
    </p>
  </div>
</template>

<style scoped>
.review-add {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2) var(--space-3);
  margin-top: var(--space-3);
}

.add {
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius);
  background: var(--color-surface);
  color: var(--color-primary);
  cursor: pointer;
}

.add:disabled {
  cursor: default;
  opacity: 0.6;
}

.done,
.form-error {
  margin: 0;
  font-size: 0.9em;
}

.form-error {
  color: var(--color-danger);
}
</style>
