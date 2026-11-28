<script setup>
// SC-27 오답 노트 (L-1b AC12). 지금 열 수 있는 분석에서 마지막 답이 틀린 문항을 개념별로 묶어 보여 준다.
// 개념 묶음은 서버 순서 그대로(문항 수가 많은 개념부터, 개념 없는 묶음은 맨 뒤). 항목마다 그 분석의 퀴즈를 다시 풀 수 있다.
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchWrongNotes } from '@/api/quizzes'
import { errorMessage } from '@/api/http'
import { useLatestRequest } from '@/composables/useLatestRequest'
import AsyncState from '@/components/common/AsyncState.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import WrongNoteItem from '@/components/quiz/WrongNoteItem.vue'

const note = ref(null)
const loading = ref(true)
const loadError = ref('')
const latest = useLatestRequest()

const shownCount = computed(
  () => note.value?.groups.reduce((sum, group) => sum + group.items.length, 0) ?? 0,
)

async function load() {
  const req = latest.begin()
  loading.value = true
  loadError.value = ''
  try {
    const result = await fetchWrongNotes()
    if (!req.isCurrent()) return
    note.value = result
  } catch (error) {
    if (!req.isCurrent()) return
    loadError.value = errorMessage(error)
  } finally {
    if (req.isCurrent()) loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="mx-auto flex max-w-[720px] flex-col gap-4 py-8">
    <h1>오답 노트</h1>

    <AsyncState
      :loading="loading"
      :error="loadError"
      :empty="note?.totalCount === 0"
      empty-message="틀린 문항이 없어요."
      @retry="load"
    >
      <template #empty-action>
        <BaseButton to="/analyses" variant="secondary" size="sm" data-action="analyses"
          >내 분석 보기</BaseButton
        >
      </template>

      <template v-if="note">
        <div class="flex flex-col gap-1">
          <p class="text-lg font-semibold" data-message="total">
            다시 볼 문항 {{ note.totalCount }}개
          </p>
          <p
            v-if="note.totalCount > shownCount"
            class="text-sm text-text-muted"
            data-message="truncated"
          >
            최근 {{ shownCount }}개를 보여 줘요.
          </p>
        </div>

        <section
          v-for="group in note.groups"
          :key="group.concept?.id ?? 'none'"
          class="flex min-w-0 flex-col gap-3"
          :data-group="group.concept?.id ?? 'none'"
        >
          <h2 class="flex flex-wrap items-baseline gap-2 text-lg">
            <RouterLink
              v-if="group.concept"
              :to="`/concepts/${group.concept.id}`"
              data-part="concept"
              >{{ group.concept.name }}</RouterLink
            >
            <span v-else data-part="concept">관련 카드 없음</span>
            <span class="text-sm font-normal text-text-muted" data-part="count"
              >{{ group.items.length }}문항</span
            >
          </h2>
          <ol class="m-0 flex list-none flex-col gap-3 p-0">
            <WrongNoteItem v-for="item in group.items" :key="item.questionId" :item="item" />
          </ol>
        </section>
      </template>
    </AsyncState>
  </section>
</template>
