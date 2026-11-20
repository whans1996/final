<script setup>
// 추천 카드 하나 (F-16 AC8). 카드 요약(ConceptCard) 아래에 추천 이유 한 줄과 신호 배지, AI가 만든 이유면 "AI" 표시.
import { computed } from 'vue'
import ConceptCard from '@/components/concept/ConceptCard.vue'

const props = defineProps({
  /** BE RecommendationItemResponse: { concept, score, signals, reason, reasonSource } */
  item: { type: Object, required: true },
})

const badges = computed(() => {
  const s = props.item.signals
  const list = []
  if (s.wrongCount > 0) list.push({ key: 'wrong', label: `오답 ${s.wrongCount}회` })
  if (s.inMyCode) list.push({ key: 'code', label: '내 코드' })
  if (s.wishedCategory) list.push({ key: 'wish', label: '찜한 분야' })
  if (s.levelFit === 1) list.push({ key: 'level', label: '수준 맞음' })
  return list
})
</script>

<template>
  <article class="recommendation">
    <ConceptCard :concept="item.concept" />
    <div class="why">
      <p class="reason">
        <span
          v-if="item.reasonSource === 'AI'"
          class="ai"
          data-field="ai"
          title="AI가 만든 추천 이유"
          >AI</span
        >
        <span data-field="reason">{{ item.reason }}</span>
      </p>
      <ul v-if="badges.length > 0" class="badges">
        <li v-for="badge in badges" :key="badge.key" :data-signal="badge.key">{{ badge.label }}</li>
      </ul>
    </div>
  </article>
</template>

<style scoped>
.recommendation {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  min-width: 0;
}

.why {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: 0 var(--space-1);
}

.reason {
  margin: 0;
  font-size: 0.9em;
  overflow-wrap: anywhere;
}

.ai {
  display: inline-block;
  margin-right: var(--space-1);
  padding: 0 var(--space-1);
  border-radius: var(--radius);
  background: var(--color-primary);
  color: #fff;
  font-size: 0.75em;
  font-weight: 700;
  vertical-align: 1px;
}

.badges {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
  margin: 0;
  padding: 0;
  list-style: none;
}

.badges li {
  padding: 0 var(--space-2);
  border: 1px solid var(--color-border);
  border-radius: 999px;
  color: var(--color-text-muted);
  font-size: 0.8em;
}
</style>
