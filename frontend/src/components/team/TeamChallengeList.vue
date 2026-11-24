<script setup>
// SC-23 팀 목표 영역 (F-20c AC11). 목록을 직접 불러오고, 팀장의 삭제·공유 해제(부모가 load 호출) 뒤 다시 불러온다.
// 조회가 실패해도 팀 정보·멤버·공유 분석은 그대로 보이게 이 영역 안에서만 에러를 보여 준다.
import { ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { deleteTeamChallenge, fetchTeamChallenges } from '@/api/teams'
import { errorMessage } from '@/api/http'
import EmptyState from '@/components/common/EmptyState.vue'
import { challengeStatusLabel, shortDate } from './teamLabels'

const props = defineProps({
  teamId: { type: Number, required: true },
  isOwner: { type: Boolean, default: false },
})

const items = ref(null)
const loadError = ref('')
const busy = ref(false)
const actionError = ref('')

let lastRequest = 0

async function load() {
  const request = ++lastRequest
  loadError.value = ''
  try {
    const result = await fetchTeamChallenges(props.teamId)
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

// 공유 해제·팀원 내보내기 뒤 부모가 부른다 (그 분석의 목표는 서버가 CASCADE로 지운다, F-20c AC8)
defineExpose({ load })

async function onDelete(item) {
  if (!window.confirm('이 팀 목표를 삭제할까요?')) return
  const teamId = props.teamId
  busy.value = true
  actionError.value = ''
  try {
    await deleteTeamChallenge(teamId, item.challengeId)
    if (teamId === props.teamId) await load()
  } catch (error) {
    if (teamId === props.teamId) actionError.value = errorMessage(error)
  } finally {
    if (teamId === props.teamId) busy.value = false
  }
}
</script>

<template>
  <section class="team-challenges">
    <h2>팀 목표</h2>

    <div v-if="loadError" class="load-error">
      <p class="form-error" role="alert" data-error="team-challenges">{{ loadError }}</p>
      <button type="button" class="secondary" data-action="retry-team-challenges" @click="load">
        다시 시도
      </button>
    </div>

    <p v-else-if="items === null" class="hint">불러오는 중…</p>

    <EmptyState
      v-else-if="items.length === 0"
      :message="
        isOwner
          ? '아직 팀 목표가 없습니다. 공유 분석의 이해도 화면에서 목표를 만들 수 있습니다.'
          : '아직 팀 목표가 없습니다.'
      "
    />

    <ul v-else class="list">
      <li
        v-for="item in items"
        :key="item.challengeId"
        class="item"
        :data-team-challenge="item.challengeId"
      >
        <div class="info">
          <strong class="title">
            {{ item.title || item.projectName }} · 정답률 {{ item.targetRate }}% 이상
          </strong>
          <span class="meta">
            {{ shortDate(item.startDate) }} ~ {{ shortDate(item.endDate) }}
            <span class="chip" :class="item.status.toLowerCase()" data-role="status">
              {{ challengeStatusLabel(item) }}
            </span>
          </span>
          <span class="meta">
            <span data-role="progress"
              >{{ item.achievedMemberCount }}/{{ item.memberCount }}명 달성</span
            >
            <span v-if="item.achieved" class="chip achieved" data-role="achieved">달성</span>
            <span v-else-if="item.status === 'ENDED'" class="chip missed" data-role="missed"
              >미달성</span
            >
          </span>
        </div>
        <div class="buttons">
          <RouterLink
            :to="`/teams/${teamId}/analyses/${item.analysisId}`"
            class="link"
            data-action="challenge-understanding"
            >이해도 보기</RouterLink
          >
          <button
            v-if="isOwner"
            type="button"
            class="danger"
            data-action="delete-challenge"
            :disabled="busy"
            @click="onDelete(item)"
          >
            삭제
          </button>
        </div>
      </li>
    </ul>
    <p v-if="actionError" class="form-error" role="alert" data-error="delete-challenge">
      {{ actionError }}
    </p>
  </section>
</template>

<style scoped>
.team-challenges {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  min-width: 0;
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
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text-muted);
  font-size: 0.9em;
}

.hint {
  margin: 0;
}

.chip {
  padding: 0 var(--space-2);
  border: 1px solid var(--color-border);
  border-radius: 999px;
  font-size: 0.85em;
}

.chip.ongoing,
.chip.achieved {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.chip.missed {
  border-color: var(--color-danger);
  color: var(--color-danger);
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
