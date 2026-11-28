<script setup>
// SC-15 상세의 첨부 상자 (F-17 AC15). 분석·카드 중 하나만 온다. viewable이 아니면 안내만 보여 준다.
import { computed } from 'vue'
import { LEVELS } from '@/utils/authValidation'
import { categoryLabel } from '@/utils/conceptCategories'
import { ATTACHMENT_ICONS } from './boardTypes'

const props = defineProps({
  analysis: { type: Object, default: null },
  concept: { type: Object, default: null },
})

const difficultyLabel = computed(
  () =>
    LEVELS.find((l) => l.value === props.concept?.difficulty)?.label ?? props.concept?.difficulty,
)
</script>

<template>
  <section v-if="analysis" class="attachment" data-attachment="analysis">
    <template v-if="analysis.viewable">
      <span class="kind">{{ ATTACHMENT_ICONS.ANALYSIS }} 분석</span>
      <strong>{{ analysis.title || analysis.projectName }}</strong>
      <span v-if="analysis.title && analysis.projectName" class="muted">{{
        analysis.projectName
      }}</span>
      <RouterLink :to="`/analyses/${analysis.id}`" class="go">분석 보기</RouterLink>
    </template>
    <p v-else class="muted">{{ ATTACHMENT_ICONS.ANALYSIS }} 볼 수 없는 분석입니다</p>
  </section>

  <section v-else-if="concept" class="attachment" data-attachment="concept">
    <RouterLink v-if="concept.viewable" :to="`/concepts/${concept.id}`" class="concept">
      <span class="kind">{{ ATTACHMENT_ICONS.CONCEPT }} 개념 카드</span>
      <strong>{{ concept.name }}</strong>
      <span class="tags">
        <span class="tag">{{ categoryLabel(concept.category) }}</span>
        <span class="tag">{{ difficultyLabel }}</span>
      </span>
      <span class="summary">{{ concept.summary }}</span>
    </RouterLink>
    <p v-else class="muted">{{ ATTACHMENT_ICONS.CONCEPT }} 볼 수 없는 개념 카드입니다</p>
  </section>
</template>

<style scoped>
.attachment {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2) var(--space-3);
  margin-top: var(--space-6);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-background);
}

.attachment p {
  margin: 0;
}

.kind,
.muted {
  color: var(--color-text-muted);
}

.go {
  margin-left: auto;
  color: var(--color-primary);
}

.concept {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  width: 100%;
  color: var(--color-text);
  text-decoration: none;
}

.concept:hover strong {
  text-decoration: underline;
}

.tags {
  display: flex;
  gap: var(--space-2);
}

.tag {
  padding: 0 var(--space-2);
  border-radius: var(--radius);
  background: var(--color-surface);
  color: var(--color-text-muted);
  font-size: 0.85em;
}

.summary {
  color: var(--color-text-muted);
}
</style>
