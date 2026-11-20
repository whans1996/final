<script setup>
// SC-08 문항 하나 (F-14 AC10). result가 없으면 풀이(라디오), 있으면 결과(정오·내 답·정답·해설·관련 카드)를 보여 준다.
import ConceptCard from '@/components/concept/ConceptCard.vue'

const props = defineProps({
  /** { id, no, question, choices } */
  question: { type: Object, required: true },
  /** 고른 선택지 위치 (0~3). 풀이 모드에서 v-model로 쓴다 */
  modelValue: { type: Number, default: null },
  /** 제출 응답의 results 항목. 있으면 결과 모드 */
  result: { type: Object, default: null },
})

const emit = defineEmits(['update:modelValue'])

function choiceClass(index) {
  if (!props.result) return {}
  return {
    answer: index === props.result.answerIndex,
    selected: index === props.result.selectedIndex,
    wrong: index === props.result.selectedIndex && !props.result.correct,
  }
}
</script>

<template>
  <li class="item" :data-question="question.id">
    <p class="head">
      <span class="no">{{ question.no }}.</span>
      <span
        v-if="result"
        class="mark"
        :class="result.correct ? 'correct' : 'incorrect'"
        data-result
        >{{ result.correct ? '정답' : '오답' }}</span
      >
    </p>
    <p class="question">{{ question.question }}</p>

    <fieldset v-if="!result" class="choices">
      <legend class="sr-only">{{ question.no }}번 선택지</legend>
      <label v-for="(choice, index) in question.choices" :key="index" class="choice">
        <input
          type="radio"
          :name="`q-${question.id}`"
          :value="index"
          :checked="modelValue === index"
          @change="emit('update:modelValue', index)"
        />
        <span>{{ choice }}</span>
      </label>
    </fieldset>

    <template v-else>
      <ol class="choices result-choices">
        <li
          v-for="(choice, index) in result.choices"
          :key="index"
          class="choice"
          :class="choiceClass(index)"
        >
          <span>{{ choice }}</span>
          <span v-if="index === result.answerIndex" class="tag">정답</span>
          <span v-if="index === result.selectedIndex" class="tag">내 답</span>
        </li>
      </ol>
      <p class="explanation">{{ result.explanation }}</p>
      <ConceptCard v-if="result.concept" :concept="result.concept" />
    </template>
  </li>
</template>

<style scoped>
.item {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  min-width: 0;
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
}

.item p {
  margin: 0;
}

.head {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-weight: 700;
}

.mark {
  padding: 0 var(--space-2);
  border-radius: var(--radius);
  font-size: 0.85em;
}

.mark.correct {
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
}

.mark.incorrect {
  color: var(--color-danger);
  border: 1px solid var(--color-danger);
}

.question {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.choices {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  min-width: 0;
  margin: 0;
  padding: 0;
  border: none;
  list-style: none;
}

.choice {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  min-width: 0;
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  overflow-wrap: anywhere;
  cursor: pointer;
}

.choice input {
  flex-shrink: 0;
  margin-top: 0.3em;
}

.result-choices .choice {
  cursor: default;
}

.choice.answer {
  border-color: var(--color-primary);
}

.choice.wrong {
  border-color: var(--color-danger);
}

.tag {
  flex-shrink: 0;
  margin-left: auto;
  color: var(--color-text-muted);
  font-size: 0.85em;
}

.explanation {
  overflow-wrap: anywhere;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
}
</style>
