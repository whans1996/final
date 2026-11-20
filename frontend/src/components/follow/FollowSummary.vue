<script setup>
// SC-18 프로필 닉네임 아래 "팔로워 N · 팔로잉 N"과 팔로우 버튼 (F-04 AC9).
// 응답을 받은 뒤에 버튼·숫자를 바꾼다 (요청 중 비활성). 비로그인은 요청 없이 로그인 화면으로 보낸다.
// 상태 조회·요청이 실패해도 프로필은 그대로 두고 이 영역에만 서버 message를 보인다.
import { computed, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { fetchFollowStatus, follow, unfollow } from '@/api/follows'
import { errorMessage } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  userId: { type: Number, required: true },
  isMe: { type: Boolean, default: false },
})

const auth = useAuthStore()
const router = useRouter()

const status = ref(null)
const loadError = ref('')
const pending = ref(false)
const actionError = ref('')
const hovering = ref(false)

const label = computed(() => {
  if (!status.value?.following) return '팔로우'
  return hovering.value ? '언팔로우' : '팔로잉'
})

// userId를 빠르게 바꿀 때 늦게 도착한 이전 요청의 응답은 버린다
let lastRequest = 0

async function load(id) {
  const request = ++lastRequest
  status.value = null
  loadError.value = ''
  actionError.value = ''
  hovering.value = false
  try {
    const result = await fetchFollowStatus(id)
    if (request === lastRequest) status.value = result
  } catch (e) {
    if (request === lastRequest) loadError.value = errorMessage(e)
  }
}

async function toggle() {
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: `/users/${props.userId}` } })
    return
  }
  const id = props.userId
  pending.value = true
  actionError.value = ''
  try {
    const result = status.value.following ? await unfollow(id) : await follow(id)
    if (id === props.userId) status.value = result
  } catch (e) {
    if (id === props.userId) actionError.value = errorMessage(e)
  } finally {
    pending.value = false
  }
}

watch(() => props.userId, load, { immediate: true })
</script>

<template>
  <div class="follow-summary" data-section="follow">
    <p v-if="loadError" class="form-error" role="alert">{{ loadError }}</p>
    <template v-else-if="status">
      <p class="counts">
        <RouterLink
          :to="{ path: `/users/${userId}/follows`, query: { tab: 'followers' } }"
          data-link="followers"
          >팔로워 {{ status.followerCount }}</RouterLink
        >
        <span aria-hidden="true"> · </span>
        <RouterLink
          :to="{ path: `/users/${userId}/follows`, query: { tab: 'followings' } }"
          data-link="followings"
          >팔로잉 {{ status.followingCount }}</RouterLink
        >
      </p>
      <div v-if="!isMe" class="action">
        <button
          type="button"
          class="follow-button"
          :class="{ on: status.following }"
          :disabled="pending"
          :aria-pressed="status.following ? 'true' : 'false'"
          data-action="follow"
          @click="toggle"
          @mouseenter="hovering = true"
          @mouseleave="hovering = false"
          @focus="hovering = true"
          @blur="hovering = false"
        >
          {{ label }}
        </button>
        <span v-if="actionError" class="form-error" role="alert">{{ actionError }}</span>
      </div>
    </template>
  </div>
</template>

<style scoped>
.follow-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
}

.counts {
  margin: 0;
}

.action {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
}

.follow-button {
  min-width: 6.5em;
  padding: var(--space-1) var(--space-4);
  border: 1px solid var(--color-primary);
  border-radius: 999px;
  color: #fff;
  background: var(--color-primary);
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.follow-button.on {
  color: var(--color-text);
  background: var(--color-surface);
  border-color: var(--color-border);
}

.follow-button.on:hover,
.follow-button.on:focus-visible {
  color: var(--color-danger);
  border-color: var(--color-danger);
}

.follow-button:disabled {
  opacity: 0.6;
  cursor: default;
}

.form-error {
  margin: 0;
  color: var(--color-danger);
}

@media (max-width: 480px) {
  .follow-summary {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
