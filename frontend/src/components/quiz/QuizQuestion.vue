<script setup>
// SC-08 문항 하나 (F-14 AC10). result가 없으면 풀이(라디오), 있으면 결과(정오·내 답·정답·해설·관련 카드)를 보여 준다.
// order는 표시 순서다: order[화면 위치] = 저장된 선지 위치 (L-1b AC11 선지 다시 섞기). 라디오 값·v-model과
// result의 selectedIndex·answerIndex는 모두 저장된 위치라, 섞어 보여도 제출·정답 강조가 맞는 선지에 붙는다.
import { computed } from 'vue'
import ConceptCard from '@/components/concept/ConceptCard.vue'
import { identityOrder } from '@/utils/quizShuffle'

const props = defineProps({
  /** { id, no, question, choices } */
  question: { type: Object, required: true },
  /** 고른 선택지의 저장된 위치 (0~3). 풀이 모드에서 v-model로 쓴다 */
  modelValue: { type: Number, default: null },
  /** 제출 응답·풀이 상세의 results 항목. 있으면 결과 모드 */
  result: { type: Object, default: null },
  /** 표시 순서 (저장된 위치의 순열). 없으면 저장된 순서 */
  order: { type: Array, default: null },
})

const emit = defineEmits(['update:modelValue'])

const choices = computed(() => props.result?.choices ?? props.question.choices)
/** [{ index: 저장된 위치, text }] 화면 순서 */
const shown = computed(() =>
  (props.order ?? identityOrder(choices.value.length)).map((index) => ({
    index,
    text: choices.value[index],
  })),
)

function choiceClass(index) {
  const { answerIndex, selectedIndex, correct } = props.result
  if (index === answerIndex) return 'border-success bg-success-soft'
  if (index === selectedIndex && !correct) return 'border-danger bg-danger-soft'
  return 'border-border'
}
</script>

<template>
  <li
    class="flex min-w-0 flex-col gap-3 rounded-md border border-border bg-surface p-4"
    :data-question="question.id"
  >
    <p class="flex items-center gap-2 font-bold">
      <span>{{ question.no }}.</span>
      <span
        v-if="result"
        class="rounded-sm border px-2 text-sm"
        :class="result.correct ? 'border-success text-success' : 'border-danger text-danger'"
        data-result
        >{{ result.correct ? '정답' : '오답' }}</span
      >
    </p>
    <p class="whitespace-pre-wrap break-words" data-part="question">{{ question.question }}</p>

    <fieldset v-if="!result" class="m-0 flex min-w-0 flex-col gap-2 border-0 p-0">
      <legend class="sr-only">{{ question.no }}번 선택지</legend>
      <label
        v-for="choice in shown"
        :key="choice.index"
        class="flex min-w-0 cursor-pointer items-start gap-2 rounded-md border border-border px-3 py-2 break-words hover:bg-bg has-[:checked]:border-primary has-[:checked]:bg-primary-soft"
        :data-choice="choice.index"
      >
        <input
          type="radio"
          class="mt-1 shrink-0 accent-primary"
          :name="`q-${question.id}`"
          :value="choice.index"
          :checked="modelValue === choice.index"
          @change="emit('update:modelValue', choice.index)"
        />
        <span>{{ choice.text }}</span>
      </label>
    </fieldset>

    <template v-else>
      <ol class="m-0 flex min-w-0 list-none flex-col gap-2 p-0">
        <li
          v-for="choice in shown"
          :key="choice.index"
          class="flex min-w-0 items-start gap-2 rounded-md border px-3 py-2 break-words"
          :class="choiceClass(choice.index)"
          :data-choice="choice.index"
          :data-answer="choice.index === result.answerIndex ? '' : undefined"
          :data-selected="choice.index === result.selectedIndex ? '' : undefined"
        >
          <span class="min-w-0">{{ choice.text }}</span>
          <span class="ms-auto flex shrink-0 gap-2 text-sm">
            <span v-if="choice.index === result.answerIndex" class="text-success">정답</span>
            <span
              v-if="choice.index === result.selectedIndex"
              :class="result.correct ? 'text-success' : 'text-danger'"
              >내 답</span
            >
          </span>
        </li>
      </ol>
      <p class="break-words" data-part="explanation">{{ result.explanation }}</p>
      <ConceptCard v-if="result.concept" :concept="result.concept" />
    </template>
  </li>
</template>
