<script setup>
// SC-24 팀 이해도 (F-20b AC12). 팀에 공유된 분석 하나의 모듈별 정답률과 멤버 × 모듈 표.
// 멤버마다 가장 최근 풀이만 센다 (서버가 계산). 404는 TEAM_404(팀)·TEAM_006(공유 분석)을 나눠 안내한다.
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { fetchTeamUnderstanding } from '@/api/teams'
import { errorCode, errorMessage } from '@/api/http'
import ModuleUnderstandingBars from '@/components/team/ModuleUnderstandingBars.vue'
import MemberUnderstandingTable from '@/components/team/MemberUnderstandingTable.vue'

const route = useRoute()
const teamId = computed(() => Number(route.params.id))
const analysisId = computed(() => Number(route.params.analysisId))

const data = ref(null)
const loading = ref(true)
/** 'team' | 'analysis' | null */
const notFound = ref(null)
const loadError = ref('')

let lastRequest = 0

async function load() {
  const request = ++lastRequest
  loading.value = true
  notFound.value = null
  loadError.value = ''
  try {
    const result = await fetchTeamUnderstanding(teamId.value, analysisId.value)
    if (request === lastRequest) data.value = result
  } catch (error) {
    if (request !== lastRequest) return
    data.value = null
    const code = errorCode(error)
    if (code === 'TEAM_404') notFound.value = 'team'
    else if (code === 'TEAM_006') notFound.value = 'analysis'
    else loadError.value = errorMessage(error)
  } finally {
    if (request === lastRequest) loading.value = false
  }
}

watch(
  [teamId, analysisId],
  () => {
    if (route.name === 'team-understanding') load()
  },
  { immediate: true },
)
</script>

<template>
  <article class="team-understanding">
    <RouterLink
      v-if="notFound !== 'team'"
      :to="`/teams/${teamId}`"
      class="back"
      data-action="back-to-team"
      >← 팀으로</RouterLink
    >

    <p v-if="loading" class="hint">불러오는 중…</p>

    <div v-else-if="notFound === 'team'" class="not-found">
      <p>팀을 찾을 수 없습니다.</p>
      <RouterLink to="/teams" data-action="back-to-teams">← 내 팀으로</RouterLink>
    </div>

    <div v-else-if="notFound === 'analysis'" class="not-found">
      <p>팀에 공유된 분석을 찾을 수 없습니다.</p>
    </div>

    <div v-else-if="loadError" class="load-error">
      <p class="form-error" role="alert">{{ loadError }}</p>
      <button type="button" class="secondary" data-action="retry" @click="load">다시 시도</button>
    </div>

    <template v-else-if="data">
      <header>
        <h1>{{ data.title || `분석 #${data.analysisId}` }}</h1>
        <p class="meta">{{ data.sharedBy.nickname }} 님 공유</p>
        <p class="meta">
          {{ data.solvedMemberCount }}/{{ data.memberCount }}명 풀이 · 객관식
          {{ data.questionCount }}문항
        </p>
        <RouterLink
          :to="{ path: `/analyses/${data.analysisId}/quiz`, query: { team: String(teamId) } }"
          class="solve"
          data-action="solve"
          >퀴즈 풀기</RouterLink
        >
      </header>

      <section>
        <h2>모듈별 이해도</h2>
        <ModuleUnderstandingBars :modules="data.modules" />
      </section>

      <section>
        <h2>멤버별</h2>
        <p v-if="data.solvedMemberCount === 0" class="hint">
          아직 이 분석의 퀴즈를 푼 팀원이 없습니다.
        </p>
        <MemberUnderstandingTable v-else :modules="data.modules" :members="data.members" />
      </section>
    </template>
  </article>
</template>

<style scoped>
.team-understanding {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
  min-width: 0;
  padding: var(--space-8) 0;
}

.back {
  color: var(--color-text-muted);
  text-decoration: none;
}

header {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-1);
}

h1 {
  margin: 0;
  overflow-wrap: anywhere;
}

h2 {
  margin: 0 0 var(--space-3);
  font-size: 1.1em;
}

section {
  min-width: 0;
}

.meta,
.hint {
  margin: 0;
  color: var(--color-text-muted);
}

.solve {
  margin-top: var(--space-2);
  padding: var(--space-2) var(--space-4);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius);
  background: var(--color-primary);
  color: #fff;
  text-decoration: none;
}

.not-found,
.load-error {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
}

.secondary {
  padding: var(--space-2) var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: transparent;
  color: inherit;
  font: inherit;
  cursor: pointer;
}

.form-error {
  margin: 0;
  color: var(--color-danger);
}
</style>
