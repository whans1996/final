<script setup>
// 개념 카드 요약 (F-00b AC10). 카드 목록(F-09)·찜(F-12)·추천(F-16) 등이 BE ConceptSummaryResponse를 그대로 넘긴다.
// 상세 화면(/concepts/{id})은 F-10이 만든다.
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { LEVELS } from '@/utils/authValidation'
import { categoryLabel } from '@/utils/conceptCategories'

const props = defineProps({
  /** { id, name, category, difficulty, summary, avgRating, wishCount } */
  concept: { type: Object, required: true },
})

const difficultyLabel = computed(
  () => LEVELS.find((l) => l.value === props.concept.difficulty)?.label ?? props.concept.difficulty,
)
const rating = computed(() => Number(props.concept.avgRating ?? 0).toFixed(1))
</script>

<template>
  <RouterLink :to="`/concepts/${concept.id}`" class="card" :data-concept-id="concept.id">
    <span class="tags">
      <span class="category">{{ categoryLabel(concept.category) }}</span>
      <span class="difficulty">{{ difficultyLabel }}</span>
    </span>
    <strong class="name">{{ concept.name }}</strong>
    <p class="summary">{{ concept.summary }}</p>
    <span class="stats">
      <span class="rating" :aria-label="`평점 ${rating}`">★ {{ rating }}</span>
      <span class="wish" :aria-label="`찜 ${concept.wishCount}`">♥ {{ concept.wishCount }}</span>
    </span>
  </RouterLink>
</template>

<style scoped>
.card {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  height: 100%;
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  color: inherit;
  background: var(--color-surface);
  text-decoration: none;
}

.card:hover {
  border-color: var(--color-primary);
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  font-size: 0.8em;
}

.category,
.difficulty {
  padding: 0 var(--space-2);
  border-radius: var(--radius);
  background: var(--color-background);
  color: var(--color-text-muted);
}

.name {
  overflow-wrap: anywhere;
}

.summary {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--color-text-muted);
  font-size: 0.9em;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
}

.stats {
  display: flex;
  gap: var(--space-4);
  margin-top: auto;
  font-size: 0.9em;
}

.rating {
  color: #b45309;
}

.wish {
  color: var(--color-danger);
}
</style>
