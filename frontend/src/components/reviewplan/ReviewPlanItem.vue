<script setup>
// SC-11 복습 계획 항목 하나 (F-15 AC9·AC10): 완료 체크박스 · 카드 요약 · 이유 배지 · 삭제 · 항목 에러.
// 체크 상태는 부모가 정한다 (요청 중에는 누른 값, 끝나면 서버 상태라 실패하면 되돌아간다).
import ConceptCard from '@/components/concept/ConceptCard.vue'

const REASON_LABELS = { WRONG: '오답', WISH: '찜', MANUAL: '직접' }

defineProps({
  /** ReviewPlanItemResponse */
  item: { type: Object, required: true },
  checked: { type: Boolean, required: true },
  busy: { type: Boolean, default: false },
  error: { type: String, default: '' },
})

const emit = defineEmits(['toggle', 'remove'])
</script>

<template>
  <li
    class="plan"
    :class="{ done: item.status === 'DONE' }"
    :data-plan-id="item.id"
    :data-status="item.status"
  >
    <div class="row">
      <input
        type="checkbox"
        class="check"
        :checked="checked"
        :disabled="busy"
        :aria-label="`${item.concept.name} 복습 완료`"
        @change="emit('toggle', $event.target.checked)"
      />
      <div class="card">
        <ConceptCard :concept="item.concept" />
      </div>
      <div class="side">
        <span class="badge" :class="`reason-${item.reason}`" data-reason>
          {{ REASON_LABELS[item.reason] ?? item.reason }}
        </span>
        <button
          type="button"
          class="remove"
          :disabled="busy"
          data-action="remove"
          @click="emit('remove')"
        >
          삭제
        </button>
      </div>
    </div>
    <p v-if="error" class="form-error" role="alert" data-message="item-error">{{ error }}</p>
  </li>
</template>

<style scoped>
.plan {
  list-style: none;
}

.row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.check {
  flex: none;
  width: 20px;
  height: 20px;
}

.card {
  flex: 1;
  min-width: 0;
}

.side {
  display: flex;
  flex: none;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--space-2);
}

.done .card {
  opacity: 0.55;
}

.badge {
  padding: 0 var(--space-2);
  border-radius: var(--radius);
  background: var(--color-background);
  color: var(--color-text-muted);
  font-size: 0.8em;
  white-space: nowrap;
}

.reason-WRONG {
  color: var(--color-danger);
}

.reason-WISH {
  color: #b45309;
}

.reason-MANUAL {
  color: var(--color-primary);
}

.remove {
  padding: var(--space-1) var(--space-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  color: var(--color-text-muted);
  font-size: 0.85em;
  cursor: pointer;
}

.remove:disabled {
  cursor: default;
  opacity: 0.6;
}

.form-error {
  margin: var(--space-1) 0 0 calc(20px + var(--space-3));
  color: var(--color-danger);
  font-size: 0.9em;
}

@media (max-width: 480px) {
  .row {
    flex-wrap: wrap;
  }

  .card {
    flex-basis: calc(100% - 20px - var(--space-3));
  }

  .side {
    flex-direction: row;
    align-items: center;
    margin-left: calc(20px + var(--space-3));
  }
}
</style>
