<script setup>
// 헤더 검색 (F-29b AC8·AC9). 검색 결과 화면은 따로 없고 기존 목록 화면으로 쿼리를 넘긴다.
// 대상 목록은 utils/searchTargets.js 한 곳에서 온다. 카테고리는 대상의 filters에 'category'가 있을 때만 보인다.
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from 'lucide-vue-next'
import { CONCEPT_CATEGORIES } from '@/utils/conceptCategories'
import { SEARCH_TARGETS, searchLocation } from '@/utils/searchTargets'

const emit = defineEmits(['searched'])
const router = useRouter()

const targetKey = ref(SEARCH_TARGETS[0].key)
const category = ref('')
const keyword = ref('')

const hasCategory = computed(() =>
  (SEARCH_TARGETS.find((t) => t.key === targetKey.value)?.filters ?? []).includes('category'),
)

async function onSubmit() {
  const filters = hasCategory.value ? { category: category.value } : {}
  const location = searchLocation(targetKey.value, keyword.value, filters)
  if (!location) return
  await router.push(location)
  emit('searched')
}

const field =
  'h-9 rounded-sm border border-border bg-surface px-2 text-sm text-text focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-primary'
</script>

<template>
  <form role="search" class="flex w-full min-w-0 items-center gap-1" @submit.prevent="onSubmit">
    <select v-model="targetKey" aria-label="검색 대상" :class="[field, 'shrink-0']">
      <option v-for="target in SEARCH_TARGETS" :key="target.key" :value="target.key">
        {{ target.label }}
      </option>
    </select>
    <select
      v-if="hasCategory"
      v-model="category"
      aria-label="카테고리"
      :class="[field, 'shrink-0']"
    >
      <option value="">전체</option>
      <option v-for="c in CONCEPT_CATEGORIES" :key="c.value" :value="c.value">{{ c.label }}</option>
    </select>
    <input
      v-model="keyword"
      type="search"
      aria-label="검색어"
      placeholder="검색어"
      :class="[field, 'min-w-28 flex-1']"
    />
    <button
      type="submit"
      aria-label="검색"
      class="flex size-9 shrink-0 items-center justify-center rounded-sm text-text-muted hover:bg-primary-soft focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-primary"
    >
      <Search class="size-5" aria-hidden="true" />
    </button>
  </form>
</template>
