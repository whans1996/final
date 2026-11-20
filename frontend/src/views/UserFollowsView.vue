<script setup>
// SC-18 팔로우 목록 (F-04 AC10). 누구나 볼 수 있다. 탭은 ?tab=followers|followings와 맞추고 잘못된 값이면 followers.
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { fetchUser } from '@/api/users'
import { FOLLOW_PAGE_SIZE, fetchFollowStatus, fetchFollowers, fetchFollowings } from '@/api/follows'
import { errorCode, errorMessage } from '@/api/http'
import { useMoreList } from '@/composables/useMoreList'
import EmptyState from '@/components/common/EmptyState.vue'
import FollowUserItem from '@/components/follow/FollowUserItem.vue'

const TABS = [
  {
    value: 'followers',
    label: '팔로워',
    countKey: 'followerCount',
    empty: '아직 팔로워가 없습니다.',
  },
  {
    value: 'followings',
    label: '팔로잉',
    countKey: 'followingCount',
    empty: '아직 팔로우한 사람이 없습니다.',
  },
]

const route = useRoute()
const router = useRouter()

const userId = computed(() => Number(route.params.id))
const tab = computed(() => TABS.find((t) => t.value === route.query.tab) ?? TABS[0])

const nickname = ref('')
const status = ref(null)
const headError = ref(null)

const list = useMoreList(
  (page) =>
    tab.value.value === 'followings'
      ? fetchFollowings(userId.value, page)
      : fetchFollowers(userId.value, page),
  FOLLOW_PAGE_SIZE,
)

// 머리(닉네임·수) 요청이나 목록 요청 중 하나라도 USER_404면 탈퇴·없는 사용자로 본다
const notFound = computed(
  () => errorCode(headError.value) === 'USER_404' || errorCode(list.error.value) === 'USER_404',
)
const errorText = computed(() => {
  const error = list.error.value ?? headError.value
  return error ? errorMessage(error) : ''
})

let lastHead = 0

async function loadHead(id) {
  const request = ++lastHead
  nickname.value = ''
  status.value = null
  headError.value = null
  try {
    const [user, followStatus] = await Promise.all([fetchUser(id), fetchFollowStatus(id)])
    if (request !== lastHead) return
    nickname.value = user.nickname
    status.value = followStatus
  } catch (e) {
    if (request === lastHead) headError.value = e
  }
}

function selectTab(value) {
  if (value !== tab.value.value) router.replace({ query: { ...route.query, tab: value } })
}

watch(
  userId,
  (id) => {
    // 프로필이 아닌 주소로 나가는 중이면 params.id가 없어 NaN이 된다
    if (Number.isInteger(id)) loadHead(id)
  },
  { immediate: true },
)
watch(
  () => [userId.value, tab.value.value],
  ([id]) => {
    if (Number.isInteger(id)) list.reset()
  },
  { immediate: true },
)
</script>

<template>
  <section class="follows">
    <RouterLink :to="`/users/${userId}`" class="back" data-link="profile">← 프로필로</RouterLink>

    <p v-if="notFound" class="status">탈퇴했거나 없는 사용자입니다.</p>
    <template v-else>
      <h1>{{ nickname ? `${nickname}님의 팔로우` : '팔로우' }}</h1>

      <div class="tabs" role="tablist">
        <button
          v-for="t in TABS"
          :key="t.value"
          type="button"
          role="tab"
          :aria-selected="t.value === tab.value ? 'true' : 'false'"
          :class="{ active: t.value === tab.value }"
          :data-tab="t.value"
          @click="selectTab(t.value)"
        >
          {{ t.label }}{{ status ? ` ${status[t.countKey]}` : '' }}
        </button>
      </div>

      <ul v-if="list.items.value.length" class="list">
        <FollowUserItem v-for="user in list.items.value" :key="user.id" :user="user" />
      </ul>

      <p v-if="list.loading.value" class="status">불러오는 중…</p>
      <p v-else-if="errorText" class="form-error" role="alert">{{ errorText }}</p>
      <EmptyState v-else-if="list.total.value === 0" :message="tab.empty" />

      <button
        v-if="list.hasMore.value && !list.loading.value"
        type="button"
        class="more"
        data-action="more"
        @click="list.more()"
      >
        더 보기
      </button>
    </template>
  </section>
</template>

<style scoped>
.follows {
  max-width: 720px;
  margin: 0 auto;
  padding: var(--space-8) 0;
}

.back {
  display: inline-block;
  margin-bottom: var(--space-4);
}

h1 {
  margin: 0 0 var(--space-4);
  overflow-wrap: anywhere;
}

.tabs {
  display: flex;
  gap: var(--space-2);
  border-bottom: 1px solid var(--color-border);
}

.tabs button {
  padding: var(--space-2) var(--space-4);
  border: none;
  border-bottom: 2px solid transparent;
  color: var(--color-text-muted);
  background: none;
  font: inherit;
  cursor: pointer;
}

.tabs button.active {
  border-bottom-color: var(--color-primary);
  color: var(--color-text);
  font-weight: 600;
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
