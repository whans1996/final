<script setup>
// SC-18 프로필의 "찜한 카드 N" 절 (F-12 AC15). 누구나 볼 수 있다. 12개씩 불러와 "더 보기"로 아래에 붙인다.
// 조회가 실패해도 프로필은 그대로 두고 이 절에만 서버 message를 보여 준다.
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchUserWishes } from '@/api/concepts'
import { errorMessage } from '@/api/http'
import ConceptCard from '@/components/concept/ConceptCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useLatestRequest } from '@/composables/useLatestRequest'

const PAGE_SIZE = 12

const props = defineProps({
  userId: { type: Number, required: true },
  isMe: { type: Boolean, default: false },
})

const items = ref([])
const total = ref(null)
const page = ref(0)
const loading = ref(false)
const error = ref('')
// 서버가 한 쪽보다 적게 주면 끝이다. 그사이 찜이 바뀌어 화면 개수가 total에 못 미쳐도 버튼이 남지 않게 한다
const ended = ref(false)

const hasMore = computed(
  () => !ended.value && total.value !== null && items.value.length < total.value,
)

// userId를 빠르게 바꾸거나 컴포넌트가 사라지면 늦게 도착한 이전 요청의 응답은 버린다 (V-4)
const latest = useLatestRequest()

async function load(nextPage) {
  const request = latest.begin()
  loading.value = true
  error.value = ''
  try {
    const result = await fetchUserWishes(props.userId, { page: nextPage, size: PAGE_SIZE })
    if (!request.isCurrent()) return
    if (nextPage === 1) {
      items.value = result.items
    } else {
      // offset 페이지라 그사이 새 찜이 생기면 다음 쪽이 앞 쪽과 겹친다. 이미 보인 카드는 거른다
      const seen = new Set(items.value.map((card) => card.id))
      items.value = [...items.value, ...result.items.filter((card) => !seen.has(card.id))]
    }
    ended.value = result.items.length < PAGE_SIZE
    total.value = result.total
    page.value = nextPage
  } catch (e) {
    if (!request.isCurrent()) return
    error.value = errorMessage(e)
  } finally {
    if (request.isCurrent()) loading.value = false
  }
}

watch(
  () => props.userId,
  () => {
    items.value = []
    total.value = null
    page.value = 0
    ended.value = false
    load(1)
  },
  { immediate: true },
)
</script>

<template>
  <section class="wishes" data-section="wishes">
    <h2>찜한 카드 {{ total ?? '' }}</h2>

    <ul v-if="items.length" class="grid">
      <li v-for="card in items" :key="card.id">
        <ConceptCard :concept="card" />
      </li>
    </ul>

    <p v-if="loading" class="status">찜한 카드를 불러오는 중…</p>
    <p v-else-if="error" class="form-error" role="alert">{{ error }}</p>
    <EmptyState v-else-if="total === 0" message="아직 찜한 카드가 없습니다.">
      <template v-if="isMe" #action>
        <RouterLink to="/concepts" data-action="browse-concepts">개념 카드 둘러보기</RouterLink>
      </template>
    </EmptyState>

    <button
      v-if="hasMore && !loading"
      type="button"
      class="more"
      data-action="more-wishes"
      @click="load(page + 1)"
    >
      더 보기
    </button>
  </section>
</template>

<style scoped>
.wishes {
  min-width: 0;
}

h2 {
  margin-top: var(--space-6);
  font-size: 1.1em;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: var(--space-4);
  margin: var(--space-3) 0 0;
  padding: 0;
  list-style: none;
}

.status {
  color: var(--color-text-muted);
}

.form-error {
  color: var(--color-danger);
}

.more {
  display: block;
  margin: var(--space-4) auto 0;
  padding: var(--space-2) var(--space-6);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  font: inherit;
  cursor: pointer;
}

@media (max-width: 480px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
