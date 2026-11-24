<script setup>
// SC-18 프로필 (F-03 AC9, F-12 찜한 카드 절 AC15, F-04 팔로우 영역 AC9). 누구나 볼 수 있다. 참여 챌린지 절은 F-25 AC15.
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { fetchUser, fetchUserStats } from '@/api/users'
import { errorCode, errorMessage } from '@/api/http'
import { LEVELS } from '@/utils/authValidation'
import UserWishes from '@/components/concept/UserWishes.vue'
import FollowSummary from '@/components/follow/FollowSummary.vue'
import UserChallenges from '@/components/challenge/UserChallenges.vue'

const route = useRoute()
const auth = useAuthStore()

const profile = ref(null)
const stats = ref(null)
const loading = ref(true)
const notFound = ref(false)
const loadError = ref('')

const userId = computed(() => Number(route.params.id))
const isMe = computed(() => auth.user?.id === userId.value)
const levelLabel = computed(() => LEVELS.find((l) => l.value === profile.value?.level)?.label ?? '')
const joinedAt = computed(() => formatDate(profile.value?.createdAt))
const accuracy = computed(() =>
  stats.value?.quizAccuracy == null ? '-' : `${stats.value.quizAccuracy}%`,
)

/** '2026-11-18T10:30:00' → '2026.11.18' */
function formatDate(value) {
  return value ? value.slice(0, 10).replaceAll('-', '.') : ''
}

// 요청마다 번호를 붙여, 늦게 도착한 이전 요청의 응답은 버린다 (A→B→A처럼 같은 id로 돌아와도 구분된다)
let lastRequest = 0

async function load(id) {
  // 프로필이 아닌 주소로 나가는 중이면 params.id가 없어 NaN이 된다
  if (!Number.isInteger(id)) return
  const request = ++lastRequest
  loading.value = true
  notFound.value = false
  loadError.value = ''
  profile.value = null
  stats.value = null
  try {
    const [user, userStats] = await Promise.all([fetchUser(id), fetchUserStats(id)])
    if (request !== lastRequest) return
    profile.value = user
    stats.value = userStats
  } catch (error) {
    if (request !== lastRequest) return
    if (errorCode(error) === 'USER_404') notFound.value = true
    else loadError.value = errorMessage(error)
  } finally {
    if (request === lastRequest) loading.value = false
  }
}

watch(userId, load, { immediate: true })
</script>

<template>
  <section class="profile">
    <p v-if="loading" class="status">불러오는 중…</p>
    <p v-else-if="notFound" class="status">탈퇴했거나 없는 사용자입니다.</p>
    <p v-else-if="loadError" class="form-error" role="alert">{{ loadError }}</p>

    <template v-else-if="profile">
      <header class="profile-head">
        <div class="title">
          <h1>{{ profile.nickname }}</h1>
          <span class="level-badge">{{ levelLabel }}</span>
        </div>
        <FollowSummary :user-id="userId" :is-me="isMe" />
        <p class="joined">{{ joinedAt }} 가입</p>
        <p v-if="profile.bio" class="bio">{{ profile.bio }}</p>
        <RouterLink v-if="isMe" :to="{ name: 'my-settings' }" class="edit-link">
          정보 수정
        </RouterLink>
      </header>

      <h2>학습 통계</h2>
      <ul class="stats">
        <li data-stat="analysisCount">
          <strong>{{ stats.analysisCount }}</strong>
          <span>완료한 분석</span>
        </li>
        <li data-stat="learnedConceptCount">
          <strong>{{ stats.learnedConceptCount }}</strong>
          <span>학습한 카드</span>
        </li>
        <li data-stat="quizAccuracy">
          <strong>{{ accuracy }}</strong>
          <span>퀴즈 정답률</span>
        </li>
      </ul>

      <UserWishes :user-id="userId" :is-me="isMe" />
      <UserChallenges :user-id="userId" :is-me="isMe" />
    </template>
  </section>
</template>

<style scoped>
.profile {
  max-width: 720px;
  margin: 0 auto;
  padding: var(--space-8) 0;
}

.status {
  color: var(--color-text-muted);
}

.form-error {
  color: var(--color-danger);
}

.profile-head {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding-bottom: var(--space-6);
  border-bottom: 1px solid var(--color-border);
}

.title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
}

.title h1 {
  margin: 0;
  overflow-wrap: anywhere;
}

.level-badge {
  padding: var(--space-1) var(--space-3);
  border-radius: 999px;
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 10%, transparent);
  font-size: 0.85em;
  font-weight: 600;
}

.joined {
  color: var(--color-text-muted);
}

.bio {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.edit-link {
  align-self: flex-start;
}

h2 {
  margin-top: var(--space-6);
  font-size: 1.1em;
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-3);
  margin: var(--space-3) 0 0;
  padding: 0;
  list-style: none;
}

.stats li {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
}

.stats strong {
  font-size: 1.5em;
}

.stats span {
  color: var(--color-text-muted);
  font-size: 0.9em;
}

@media (max-width: 480px) {
  .stats {
    grid-template-columns: 1fr;
  }
}
</style>
