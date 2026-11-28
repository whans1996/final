<script setup>
// 면접 질문 하나 (I-1a AC13). 스스로 답해 본 뒤 "핵심 포인트 보기"를 눌러 포인트를 펼친다.
// 근거 칩은 파일 탭의 그 줄들(?tab=files&file=..&lines=12-30)로, 카드 칩은 카드 상세로 간다.
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { formatLines, lineLabel } from '@/utils/interviewFormat'
import BaseButton from '@/components/common/BaseButton.vue'
import FilePath from '@/components/analysis/FilePath.vue'

const props = defineProps({
  question: { type: Object, required: true },
})

const open = ref(false)
const pointsId = `interview-points-${props.question.id}`

function evidenceTo(evidence) {
  return {
    query: { tab: 'files', file: String(evidence.fileId), lines: formatLines(evidence) },
  }
}
</script>

<template>
  <article
    class="flex min-w-0 flex-col gap-3 rounded-md border border-border bg-surface p-4"
    :data-question="question.no"
  >
    <h4 class="flex gap-2 text-base font-semibold">
      <span class="shrink-0 text-primary">Q{{ question.no }}.</span>
      <span class="min-w-0 wrap-anywhere">{{ question.question }}</span>
    </h4>

    <div class="flex flex-wrap items-center gap-2" data-section="evidence">
      <span class="text-sm text-text-muted">근거</span>
      <RouterLink
        v-for="(evidence, index) in question.evidence"
        :key="index"
        :to="evidenceTo(evidence)"
        class="inline max-w-full rounded-sm border border-border bg-code-bg px-2 py-0.5 font-mono text-xs text-code-text no-underline box-decoration-clone hover:border-primary"
        data-evidence
      >
        <FilePath :path="evidence.path" /><span class="whitespace-nowrap"
          >:{{ lineLabel(evidence) }}</span
        >
      </RouterLink>
    </div>

    <div v-if="question.concepts.length" class="flex flex-wrap items-center gap-2">
      <span class="text-sm text-text-muted">관련 카드</span>
      <RouterLink
        v-for="concept in question.concepts"
        :key="concept.id"
        :to="{ name: 'concept-detail', params: { id: concept.id } }"
        class="rounded-full bg-primary-soft px-2 py-0.5 text-xs font-semibold text-primary no-underline"
        :data-concept="concept.id"
      >
        {{ concept.name }}
      </RouterLink>
    </div>

    <div class="flex flex-col items-start gap-2">
      <BaseButton
        variant="ghost"
        size="sm"
        :aria-expanded="open ? 'true' : 'false'"
        :aria-controls="pointsId"
        data-action="toggle-points"
        @click="open = !open"
      >
        {{ open ? '핵심 포인트 접기' : '핵심 포인트 보기' }}
      </BaseButton>
      <ul
        v-if="open"
        :id="pointsId"
        class="flex flex-col gap-1 self-stretch rounded-sm bg-bg px-4 py-3 ps-8"
        data-points
      >
        <li v-for="(point, index) in question.keyPoints" :key="index" class="wrap-anywhere">
          {{ point }}
        </li>
      </ul>
    </div>
  </article>
</template>
