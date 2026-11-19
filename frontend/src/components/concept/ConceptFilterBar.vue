<script setup>
// SC-09 개념 카드 검색·필터 바 (F-09 AC9·AC10). 조건의 정본은 라우트 쿼리라 화면이 filters로 내려 주고,
// 사용자가 검색(Enter·버튼)하거나 선택을 바꾸면 입력란 전체 값을 change로 올린다.
import { reactive, watch } from 'vue'
import { LEVELS } from '@/utils/authValidation'
import { CONCEPT_CATEGORIES } from '@/utils/conceptCategories'

const props = defineProps({
  /** { keyword, category, difficulty, sort } */
  filters: { type: Object, required: true },
})

const emit = defineEmits(['change'])

const SORTS = [
  { value: 'POPULAR', label: '인기순' },
  { value: 'LATEST', label: '최신순' },
]

const form = reactive({ ...props.filters })

// 뒤로 가기·필터 초기화로 쿼리가 바뀌면 입력란도 따라간다
watch(
  () => props.filters,
  (filters) => Object.assign(form, filters),
)

function apply() {
  emit('change', { ...form, keyword: form.keyword.trim() })
}
</script>

<template>
  <form class="filter-bar" role="search" @submit.prevent="apply">
    <div class="search">
      <input
        v-model="form.keyword"
        name="keyword"
        type="search"
        maxlength="50"
        placeholder="개념 이름이나 요약으로 검색"
        aria-label="검색어"
      />
      <button type="submit">검색</button>
    </div>
    <select v-model="form.category" name="category" aria-label="카테고리" @change="apply">
      <option value="">전체</option>
      <option v-for="c in CONCEPT_CATEGORIES" :key="c.value" :value="c.value">
        {{ c.label }}
      </option>
    </select>
    <select v-model="form.difficulty" name="difficulty" aria-label="난이도" @change="apply">
      <option value="">전체</option>
      <option v-for="l in LEVELS" :key="l.value" :value="l.value">{{ l.label }}</option>
    </select>
    <select v-model="form.sort" name="sort" aria-label="정렬" @change="apply">
      <option v-for="s in SORTS" :key="s.value" :value="s.value">{{ s.label }}</option>
    </select>
  </form>
</template>

<style scoped>
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-4);
}

.search {
  display: flex;
  flex: 1 1 260px;
  gap: var(--space-2);
  min-width: 0;
}

.search input {
  flex: 1;
  min-width: 0;
}

input,
select,
button {
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  font: inherit;
}

button {
  color: #fff;
  background: var(--color-primary);
  border-color: var(--color-primary);
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}

select {
  flex: 0 1 auto;
  min-width: 0;
}
</style>
