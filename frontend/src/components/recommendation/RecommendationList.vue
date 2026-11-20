<script setup>
// 맞춤 추천 목록 (F-16 AC8·AC10). 대시보드(SC-04)와 복습 계획 빈 상태(SC-11)가 함께 쓴다.
// 마운트할 때 한 번 부른다. hideOnEmptyOrError면 결과가 있을 때만 절 전체(제목 포함)를 보인다.
import { computed, ref } from 'vue'
import { fetchRecommendations } from '@/api/recommendations'
import { errorMessage } from '@/api/http'
import RecommendationCard from '@/components/recommendation/RecommendationCard.vue'

const props = defineProps({
  title: { type: String, required: true },
  size: { type: Number, default: 5 },
  hideOnEmptyOrError: { type: Boolean, default: false },
})

const items = ref([])
const loading = ref(true)
const loadError = ref('')

const visible = computed(() => !props.hideOnEmptyOrError || items.value.length > 0)

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await fetchRecommendations({ size: props.size })
    items.value = data?.items ?? []
  } catch (error) {
    items.value = []
    loadError.value = errorMessage(error)
  } finally {
    loading.value = false
  }
}

load()
</script>

<template>
  <div v-if="visible" class="recommendations">
    <h2>{{ title }}</h2>
    <p v-if="loading" class="hint">추천을 만드는 중…</p>
    <div v-else-if="loadError" class="error-box">
      <p class="form-error" role="alert">{{ loadError }}</p>
      <button
        type="button"
        class="outline-button"
        data-action="retry-recommendations"
        @click="load"
      >
        다시 시도
      </button>
    </div>
    <p v-else-if="items.length === 0" class="hint">추천할 카드가 없습니다.</p>
    <div v-else class="grid">
      <RecommendationCard v-for="item in items" :key="item.concept.id" :item="item" />
    </div>
  </div>
</template>

<style scoped>
h2 {
  margin: 0 0 var(--space-3);
  font-size: 1.1em;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(260px, 100%), 1fr));
  gap: var(--space-4);
}

.hint {
  color: var(--color-text-muted);
}

.form-error {
  margin: 0 0 var(--space-2);
  color: var(--color-danger);
}

.outline-button {
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  color: var(--color-text);
  cursor: pointer;
}
</style>
