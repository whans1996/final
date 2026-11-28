<script setup>
// SC-27 오답 노트의 문항 하나 (L-1b AC12). 질문, 선택지(내 마지막 답은 오답 표시, 정답 표시), 해설, 틀린 횟수,
// 분석 제목과 "다시 풀기"(그 분석의 퀴즈 전체, ?from=wrong-notes)를 보여 준다. 선택지는 저장된 순서다.
import BaseButton from '@/components/common/BaseButton.vue'
import { attemptTime } from '@/utils/quizFormat'

defineProps({
  /** GET /api/users/me/wrong-notes groups[].items[] */
  item: { type: Object, required: true },
})
</script>

<template>
  <li
    class="flex min-w-0 flex-col gap-3 rounded-md border border-border bg-surface p-4"
    :data-wrong-note="item.questionId"
  >
    <p class="whitespace-pre-wrap break-words font-semibold" data-part="question">
      {{ item.question }}
    </p>
    <ol class="m-0 flex min-w-0 list-none flex-col gap-2 p-0">
      <li
        v-for="(choice, index) in item.choices"
        :key="index"
        class="flex min-w-0 items-start gap-2 rounded-md border px-3 py-2 break-words"
        :class="
          index === item.answerIndex
            ? 'border-success bg-success-soft'
            : index === item.selectedIndex
              ? 'border-danger bg-danger-soft'
              : 'border-border'
        "
        :data-choice="index"
        :data-answer="index === item.answerIndex ? '' : undefined"
        :data-selected="index === item.selectedIndex ? '' : undefined"
      >
        <span class="min-w-0">{{ choice }}</span>
        <span v-if="index === item.answerIndex" class="ms-auto shrink-0 text-sm text-success"
          >정답</span
        >
        <span v-if="index === item.selectedIndex" class="ms-auto shrink-0 text-sm text-danger"
          >내 답 (오답)</span
        >
      </li>
    </ol>
    <p class="break-words" data-part="explanation">{{ item.explanation }}</p>
    <div class="flex flex-wrap items-center gap-x-3 gap-y-2 text-sm text-text-muted">
      <span class="font-semibold text-danger" data-part="wrong-count"
        >{{ item.wrongCount }}번 틀림</span
      >
      <span data-part="answered-at">{{ attemptTime(item.lastAnsweredAt) }}</span>
      <span class="min-w-0 break-words" data-part="analysis">{{ item.analysisTitle }}</span>
      <BaseButton
        class="ms-auto"
        variant="secondary"
        size="sm"
        :to="{ path: `/analyses/${item.analysisId}/quiz`, query: { from: 'wrong-notes' } }"
        data-action="retry-quiz"
        >다시 풀기</BaseButton
      >
    </div>
  </li>
</template>
