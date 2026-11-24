<script setup>
// SC-23 공유 분석 영역 (F-20b AC10·AC11). 목록을 직접 불러오고, 공유(TeamAnalysisPicker)·해제 후 다시 불러온다.
// 목록 조회가 실패해도 팀 정보·멤버(부모)는 그대로 보이게 이 영역 안에서만 에러를 보여 준다.
// 해제에 성공하면 `unshared`를 보낸다 (그 분석의 팀 목표가 함께 지워지므로 부모가 목표 목록을 다시 불러온다, F-20c AC11).
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { MAX_TEAM_ANALYSES, fetchTeamAnalyses, unshareTeamAnalysis } from '@/api/teams'
import { errorMessage } from '@/api/http'
import EmptyState from '@/components/common/EmptyState.vue'
import TeamAnalysisPicker from './TeamAnalysisPicker.vue'
import { formatDate } from './teamLabels'

const props = defineProps({
  teamId: { type: Number, required: true },
  memberCount: { type: Number, required: true },
})

const emit = defineEmits(['unshared'])

const items = ref(null)
const loadError = ref('')
const busy = ref(false)
const actionError = ref('')

const sharedIds = computed(() => (items.value ?? []).map((a) => a.analysisId))
// 불러오는 중·실패에는 개수를 모르므로 "0/10"으로 오해하지 않게 숫자를 빼고 보여 준다
const countLabel = computed(() =>
  items.value ? `공유 분석 ${items.value.length}/${MAX_TEAM_ANALYSES}` : '공유 분석',
)
const full = computed(() => (items.value?.length ?? 0) >= MAX_TEAM_ANALYSES)

let lastRequest = 0

async function load() {
  const request = ++lastRequest
  loadError.value = ''
  try {
    const result = await fetchTeamAnalyses(props.teamId)
    if (request === lastRequest) items.value = result
  } catch (error) {
    if (request !== lastRequest) return
    items.value = null
    loadError.value = errorMessage(error)
  }
}

watch(
  () => props.teamId,
  () => {
    items.value = null
    actionError.value = ''
    busy.value = false
    load()
  },
  { immediate: true },
)

// 팀원을 내보낸 뒤처럼 부모가 목록을 다시 불러와야 할 때 쓴다 (내보낸 사람의 공유는 서버가 지운다, F-20b AC8)
defineExpose({ load })

async function onUnshare(item) {
  if (
    !window.confirm(
      '이 분석의 공유를 해제할까요? 팀원의 풀이 기록은 남고, 이 분석의 팀 목표는 함께 삭제됩니다.',
    )
  )
    return
  const teamId = props.teamId
  busy.value = true
  actionError.value = ''
  try {
    await unshareTeamAnalysis(teamId, item.analysisId)
    if (teamId !== props.teamId) return
    emit('unshared', item.analysisId)
    await load()
  } catch (error) {
    if (teamId === props.teamId) actionError.value = errorMessage(error)
  } finally {
    if (teamId === props.teamId) busy.value = false
  }
}
</script>

<template>
  <section class="team-analyses">
    <div class="head">
      <h2 data-role="shared-count">
        {{ countLabel }}
      </h2>
      <TeamAnalysisPicker
        v-if="items"
        :team-id="teamId"
        :shared-ids="sharedIds"
        :full="full || busy"
        @shared="load"
      />
    </div>

    <div v-if="loadError" class="load-error">
      <p class="form-error" role="alert" data-error="team-analyses">{{ loadError }}</p>
      <button type="button" class="secondary" data-action="retry-team-analyses" @click="load">
        다시 시도
      </button>
    </div>

    <p v-else-if="items === null" class="hint">불러오는 중…</p>

    <EmptyState
      v-else-if="items.length === 0"
      message="아직 공유한 분석이 없습니다. 내 분석을 공유해 팀원과 함께 퀴즈를 풀어 보세요."
    />

    <ul v-else class="list">
      <li
        v-for="item in items"
        :key="item.analysisId"
        class="item"
        :data-team-analysis="item.analysisId"
      >
        <div class="info">
          <strong class="title">{{ item.title || item.projectName }}</strong>
          <span class="meta">
            {{ item.sharedBy.nickname }} 님 공유 · {{ formatDate(item.sharedAt) }}
          </span>
          <span class="meta">
            {{ item.solvedMemberCount }}/{{ memberCount }}명 풀이 ·
            <template v-if="item.myLastAttempt">
              내 점수 {{ item.myLastAttempt.score }}/{{ item.myLastAttempt.total }}
            </template>
            <template v-else>아직 풀지 않음</template>
          </span>
        </div>
        <div class="buttons">
          <RouterLink
            :to="{ path: `/analyses/${item.analysisId}/quiz`, query: { team: String(teamId) } }"
            class="link primary"
            data-action="solve"
            >퀴즈 풀기</RouterLink
          >
          <RouterLink
            :to="`/teams/${teamId}/analyses/${item.analysisId}`"
            class="link"
            data-action="understanding"
            >이해도 보기</RouterLink
          >
          <button
            v-if="item.canUnshare"
            type="button"
            class="danger"
            data-action="unshare"
            :disabled="busy"
            @click="onUnshare(item)"
          >
            공유 해제
          </button>
        </div>
      </li>
    </ul>
    <p v-if="actionError" class="form-error" role="alert" data-error="unshare">
      {{ actionError }}
    </p>
  </section>
</template>

<style scoped>
.team-analyses {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  min-width: 0;
}

.head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-2);
}

h2 {
  margin: 0;
  font-size: 1.1em;
}

.list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin: 0;
  padding: 0;
  list-style: none;
}

.item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
}

.info {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  min-width: 0;
}

.title {
  overflow-wrap: anywhere;
}

.meta,
.hint {
  color: var(--color-text-muted);
  font-size: 0.9em;
}

.hint {
  margin: 0;
}

.buttons {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.link,
.secondary,
.danger {
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: transparent;
  color: inherit;
  font: inherit;
  text-decoration: none;
  cursor: pointer;
}

.link.primary {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.danger {
  border-color: var(--color-danger);
  color: var(--color-danger);
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.load-error {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
}

.form-error {
  margin: 0;
  color: var(--color-danger);
}
</style>
