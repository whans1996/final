<script setup>
// SC-16 첨부 고르기 (F-17 AC17). 없음 / 내 분석(공개만, #206) / 개념 카드 중 하나만 고른다.
// v-model 값은 null 또는 { type: 'ANALYSIS'|'CONCEPT', id, label } (label은 미리보기 칩에만 쓴다).
import { computed, ref, watch } from 'vue'
import { fetchAnalyses } from '@/api/analyses'
import { fetchConcepts } from '@/api/concepts'
import { errorMessage } from '@/api/http'
import { ATTACHMENT_ICONS, analysisLabel } from './boardTypes'

const props = defineProps({
  modelValue: { type: Object, default: null },
})

const emit = defineEmits(['update:modelValue'])

const MODES = [
  { value: 'none', label: '없음' },
  { value: 'analysis', label: '내 분석' },
  { value: 'concept', label: '개념 카드' },
]
const ANALYSIS_PAGE = { page: 1, size: 100 }
const CONCEPT_RESULT_SIZE = 10

const modeOf = (value) =>
  value?.type === 'ANALYSIS' ? 'analysis' : value?.type === 'CONCEPT' ? 'concept' : 'none'

const mode = ref(modeOf(props.modelValue))

const analyses = ref(null) // 처음 "내 분석"을 열 때 한 번 불러온다
const analysesError = ref('')
const keyword = ref('')
const concepts = ref(null)
const conceptsError = ref('')
const searching = ref(false)
// 비공개 분석은 독자에게 "볼 수 없는 분석"이 되므로 BE가 거절한다(POST_001). 고를 수 없게 보여 주고 안내한다 (#206)
const PRIVATE_HINT =
  '비공개 분석은 첨부할 수 없습니다. 분석 화면에서 공개로 바꾸면 첨부할 수 있습니다.'
const hasPrivate = computed(() => analyses.value?.some((a) => !a.isPublic) ?? false)

// 미리 채우기·수정 화면처럼 밖에서 첨부가 들어오면 그 종류로 맞춘다
watch(
  () => props.modelValue,
  (value) => {
    if (value) mode.value = modeOf(value)
  },
)

watch(
  [mode, () => props.modelValue],
  ([current, value]) => {
    if (current === 'analysis' && !value && analyses.value === null) loadAnalyses()
  },
  { immediate: true },
)

function selectMode(next) {
  mode.value = next
  // 종류를 바꾸면 고른 것을 푼다 (하나만 첨부)
  if (props.modelValue && modeOf(props.modelValue) !== next) emit('update:modelValue', null)
}

async function loadAnalyses() {
  analysesError.value = ''
  try {
    const result = await fetchAnalyses(ANALYSIS_PAGE)
    analyses.value = result.items.filter((a) => a.status === 'DONE')
  } catch (error) {
    analyses.value = []
    analysesError.value = errorMessage(error)
  }
}

function onAnalysisChange(event) {
  const id = Number(event.target.value)
  const analysis = analyses.value?.find((a) => a.id === id)
  if (analysis?.isPublic)
    emit('update:modelValue', { type: 'ANALYSIS', id, label: analysisLabel(analysis) })
}

// Enter는 "찾기" 버튼의 disabled를 거치지 않아 검색이 겹칠 수 있다. 마지막 검색의 응답만 반영한다 (#172)
let lastSearch = 0

async function searchConcepts() {
  const request = ++lastSearch
  searching.value = true
  conceptsError.value = ''
  try {
    const result = await fetchConcepts({ keyword: keyword.value.trim(), size: CONCEPT_RESULT_SIZE })
    if (request !== lastSearch) return
    concepts.value = result.items
  } catch (error) {
    if (request !== lastSearch) return
    concepts.value = []
    conceptsError.value = errorMessage(error)
  } finally {
    if (request === lastSearch) searching.value = false
  }
}

function chooseConcept(concept) {
  emit('update:modelValue', { type: 'CONCEPT', id: concept.id, label: concept.name })
}

function clear() {
  mode.value = 'none'
  emit('update:modelValue', null)
}
</script>

<template>
  <fieldset class="picker">
    <legend>첨부 (선택)</legend>
    <div class="modes">
      <label v-for="option in MODES" :key="option.value">
        <input
          type="radio"
          name="attachmentMode"
          :value="option.value"
          :checked="mode === option.value"
          @change="selectMode(option.value)"
        />
        {{ option.label }}
      </label>
    </div>

    <div v-if="modelValue" class="chip" data-role="attachment-chip">
      <span>{{ ATTACHMENT_ICONS[modelValue.type] }} {{ modelValue.label }}</span>
      <button type="button" data-action="clear-attachment" aria-label="첨부 해제" @click="clear">
        ✕
      </button>
    </div>

    <div v-else-if="mode === 'analysis'" class="choose">
      <p v-if="analyses === null" class="hint">불러오는 중…</p>
      <p v-else-if="analysesError" class="error">{{ analysesError }}</p>
      <p v-else-if="analyses.length === 0" class="hint">
        첨부할 수 있는 분석(완료된 분석)이 없습니다
      </p>
      <select v-else name="analysisId" aria-label="첨부할 분석" @change="onAnalysisChange">
        <option value="">분석을 고르세요</option>
        <option
          v-for="analysis in analyses"
          :key="analysis.id"
          :value="analysis.id"
          :disabled="!analysis.isPublic"
        >
          {{ analysisLabel(analysis) }}{{ analysis.isPublic ? '' : ' (비공개)' }}
        </option>
      </select>
      <p v-if="hasPrivate" class="hint" data-role="private-hint">{{ PRIVATE_HINT }}</p>
    </div>

    <div v-else-if="mode === 'concept'" class="choose">
      <div class="search">
        <input
          v-model="keyword"
          name="conceptKeyword"
          type="search"
          maxlength="50"
          placeholder="카드 이름으로 찾기"
          aria-label="개념 카드 검색어"
          @keydown.enter.prevent="searchConcepts"
        />
        <button
          type="button"
          data-action="search-concepts"
          :disabled="searching"
          @click="searchConcepts"
        >
          찾기
        </button>
      </div>
      <p v-if="conceptsError" class="error">{{ conceptsError }}</p>
      <p v-else-if="concepts && concepts.length === 0" class="hint">검색 결과가 없습니다</p>
      <ul v-else-if="concepts" class="results">
        <li v-for="concept in concepts" :key="concept.id">
          <button type="button" :data-concept-option="concept.id" @click="chooseConcept(concept)">
            {{ concept.name }} <span class="hint">{{ concept.category }}</span>
          </button>
        </li>
      </ul>
    </div>
  </fieldset>
</template>

<style scoped>
.picker {
  margin: 0;
  padding: var(--space-3) var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
}

.modes {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-4);
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  max-width: 100%;
  margin-top: var(--space-3);
  padding: var(--space-1) var(--space-3);
  border-radius: 999px;
  background: var(--color-background);
}

.chip span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chip button {
  border: none;
  background: none;
  font: inherit;
  cursor: pointer;
}

.choose {
  margin-top: var(--space-3);
}

.choose select {
  width: 100%;
  max-width: 100%;
  padding: var(--space-2);
  font: inherit;
}

.search {
  display: flex;
  gap: var(--space-2);
}

.search input {
  flex: 1;
  min-width: 0;
  padding: var(--space-2);
  font: inherit;
}

.search button,
.results button {
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  font: inherit;
  cursor: pointer;
}

.results {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin: var(--space-3) 0 0;
  padding: 0;
  list-style: none;
}

.hint {
  color: var(--color-text-muted);
}

.error {
  color: var(--color-danger);
}
</style>
