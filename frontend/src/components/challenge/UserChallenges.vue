<script setup>
// SC-18 프로필의 "참여 챌린지 N" 절 (F-25 AC15). 누구나 볼 수 있다. 참여 시각 최신순 6개씩 불러와 "더 보기"로 아래에 붙인다.
// 조회가 실패해도 프로필은 그대로 두고 이 절에만 서버 message를 보여 준다 (UserWishes와 같은 방식).
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchUserChallenges } from '@/api/challenges'
import { errorMessage } from '@/api/http'
import MyChallengeList from '@/components/challenge/MyChallengeList.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const PAGE_SIZE = 6

const props = defineProps({
  userId: { type: Number, required: true },
  isMe: { type: Boolean, default: false },
})

const items = ref([])
const total = ref(null)
const page = ref(0)
const loading = ref(false)
const error = ref('')
// 서버가 한 쪽보다 적게 주면 끝이다
const ended = ref(false)

const hasMore = computed(
  () => !ended.value && total.value !== null && items.value.length < total.value,
)

// userId를 빠르게 바꿀 때 늦게 도착한 이전 요청의 응답은 버린다
let lastRequest = 0

async function load(nextPage) {
  const request = ++lastRequest
  loading.value = true
  error.value = ''
  try {
    const result = await fetchUserChallenges(props.userId, { page: nextPage, size: PAGE_SIZE })
    if (request !== lastRequest) return
    if (nextPage === 1) {
      items.value = result.items
    } else {
      // offset 페이지라 그사이 새로 참여하면 다음 쪽이 앞 쪽과 겹친다. 이미 보인 챌린지는 거른다
      const seen = new Set(items.value.map((c) => c.id))
      items.value = [...items.value, ...result.items.filter((c) => !seen.has(c.id))]
    }
    ended.value = result.items.length < PAGE_SIZE
    total.value = result.total
    page.value = nextPage
  } catch (e) {
    if (request !== lastRequest) return
    error.value = errorMessage(e)
  } finally {
    if (request === lastRequest) loading.value = false
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
  <section class="user-challenges" data-section="challenges">
    <h2>참여 챌린지 {{ total ?? '' }}</h2>

    <MyChallengeList v-if="items.length" :items="items" class="list" />

    <p v-if="loading" class="status">참여 챌린지를 불러오는 중…</p>
    <p v-else-if="error" class="form-error" role="alert">{{ error }}</p>
    <EmptyState v-else-if="total === 0" message="아직 참여한 챌린지가 없습니다.">
      <template v-if="isMe" #action>
        <RouterLink to="/challenges" data-action="browse-challenges">챌린지 둘러보기</RouterLink>
      </template>
    </EmptyState>

    <button
      v-if="hasMore && !loading"
      type="button"
      class="more"
      data-action="more-challenges"
      @click="load(page + 1)"
    >
      더 보기
    </button>
  </section>
</template>

<style scoped>
.user-challenges {
  min-width: 0;
}

h2 {
  margin-top: var(--space-6);
  font-size: 1.1em;
}

.list {
  margin-top: var(--space-3);
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
</style>
