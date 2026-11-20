<script setup>
// SC-17 활동 피드 (F-04 AC11). 로그인 필요. 팔로우한 사람의 활동을 20개씩 "더 보기"로 붙인다 (이미 보인 활동 id는 거름).
// 빈 피드면 내 팔로잉 수(AC4)로 "팔로우한 사람이 없음"과 "새 활동 없음"을 나눈다.
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { FEED_PAGE_SIZE, fetchFeed } from '@/api/feed'
import { fetchFollowStatus } from '@/api/follows'
import { errorMessage } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { useMoreList } from '@/composables/useMoreList'
import { isKnownFeedType } from '@/components/feed/feedTypes'
import EmptyState from '@/components/common/EmptyState.vue'
import FeedItem from '@/components/feed/FeedItem.vue'

const auth = useAuthStore()
const list = useMoreList(fetchFeed, FEED_PAGE_SIZE)
// 빈 피드일 때만 센다. 실패하면 null로 두고 "새 활동이 없습니다"를 보인다
const followingCount = ref(null)
// 빈 피드의 팔로잉 수를 세는 동안에도 "불러오는 중…"을 유지해 틀린 빈 안내가 잠깐 보이지 않게 한다
const checking = ref(false)
const now = ref(new Date())
const loading = computed(() => list.loading.value || checking.value)

const visibleItems = computed(() => list.items.value.filter((item) => isKnownFeedType(item.type)))

async function load() {
  now.value = new Date()
  checking.value = true
  try {
    await list.reset()
    if (list.total.value === 0 && auth.user?.id != null) {
      try {
        followingCount.value = (await fetchFollowStatus(auth.user.id)).followingCount
      } catch {
        followingCount.value = null
      }
    }
  } finally {
    checking.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="feed">
    <h1>피드</h1>

    <ul v-if="visibleItems.length" class="list">
      <FeedItem v-for="item in visibleItems" :key="item.id" :item="item" :now="now" />
    </ul>

    <p v-if="loading" class="status">불러오는 중…</p>
    <p v-else-if="list.error.value" class="form-error" role="alert">
      {{ errorMessage(list.error.value) }}
    </p>
    <template v-else-if="list.total.value === 0">
      <EmptyState v-if="followingCount === 0" message="아직 팔로우한 사람이 없어요">
        <template #action>
          <RouterLink to="/community" data-action="browse-community">커뮤니티 둘러보기</RouterLink>
        </template>
      </EmptyState>
      <EmptyState v-else message="새 활동이 없습니다" />
    </template>

    <button
      v-if="list.hasMore.value && !loading"
      type="button"
      class="more"
      data-action="more"
      @click="list.more()"
    >
      더 보기
    </button>
  </section>
</template>

<style scoped>
.feed {
  max-width: 720px;
  margin: 0 auto;
  padding: var(--space-8) 0;
}

h1 {
  margin: 0 0 var(--space-4);
}

.list {
  margin: 0;
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
</style>
