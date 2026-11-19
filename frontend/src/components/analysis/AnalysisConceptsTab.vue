<script setup>
// SC-07 "개념" 탭 (F-08 AC8·AC9). 분석 코드에 쓰인 공개 카드를 1회 불러와 카드와 위치·맥락을 보여 준다.
// 카드를 누르면 상세(/concepts/{id}, F-10)로, 위치를 누르면 파일 탭으로 간다.
import { onMounted, ref } from 'vue'
import { fetchAnalysisConcepts } from '@/api/analyses'
import { errorMessage } from '@/api/http'
import ConceptCard from '@/components/concept/ConceptCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import FilePath from '@/components/analysis/FilePath.vue'

const props = defineProps({
  analysisId: { type: Number, required: true },
})

const emit = defineEmits(['select-file'])

const result = ref(null)
const loading = ref(true)
const loadError = ref('')

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    result.value = await fetchAnalysisConcepts(props.analysisId)
  } catch (error) {
    loadError.value = errorMessage(error)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="concepts-tab">
    <p v-if="loading" class="hint">개념을 불러오는 중…</p>

    <div v-else-if="loadError" class="error-box">
      <p class="form-error" role="alert">{{ loadError }}</p>
      <button type="button" class="retry-button" data-action="retry-load" @click="load">
        다시 시도
      </button>
    </div>

    <template v-else-if="result">
      <p v-if="result.pendingCount > 0" class="pending" data-message="pending" role="status">
        새 개념 {{ result.pendingCount }}개는 관리자 검수 후 카드로 공개됩니다.
      </p>
      <EmptyState
        v-if="result.items.length === 0"
        message="이 분석에서 연결된 개념 카드가 없습니다."
      />
      <ul v-else class="list">
        <li v-for="item in result.items" :key="item.concept.id" class="item" data-concept-item>
          <ConceptCard :concept="item.concept" />
          <button
            v-if="item.fileId"
            type="button"
            class="location"
            data-action="open-file"
            @click="emit('select-file', item.fileId)"
          >
            <FilePath v-if="item.filePath" :path="item.filePath" /><span v-if="item.lineHint">
              · {{ item.lineHint }}</span
            >
          </button>
          <p v-if="item.contextNote" class="note">{{ item.contextNote }}</p>
        </li>
      </ul>
    </template>
  </div>
</template>

<style scoped>
.concepts-tab {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  min-width: 0;
}

.list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 320px), 1fr));
  gap: var(--space-4);
  margin: 0;
  padding: 0;
  list-style: none;
}

.item {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  min-width: 0;
}

.location {
  align-self: flex-start;
  max-width: 100%;
  padding: 0;
  border: none;
  color: var(--color-primary);
  background: none;
  font: inherit;
  font-size: 0.9em;
  text-align: left;
  cursor: pointer;
  overflow-wrap: anywhere;
}

.location:hover {
  text-decoration: underline;
}

.note {
  margin: 0;
  font-size: 0.9em;
  overflow-wrap: anywhere;
}

.pending {
  margin: 0;
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius);
  background: var(--color-background);
}

.hint {
  color: var(--color-text-muted);
}

.form-error {
  color: var(--color-danger);
}

.error-box {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-3);
}

.retry-button {
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius);
  color: var(--color-primary);
  background: var(--color-surface);
  font: inherit;
  cursor: pointer;
}
</style>
