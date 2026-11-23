<script setup>
// SC-23 팀 상세 (F-20a AC14·AC15). 멤버만 볼 수 있다 (남의 팀·없는 팀은 404).
// 멤버 아래에 공유 분석 영역(F-20b AC10·AC11, TeamAnalysisList)이 있다.
// 팀장: 초대 코드 상자, 수정, 팀 삭제, 팀원 내보내기. 팀원: 팀 탈퇴. 파괴적 동작은 window.confirm으로 확인한다.
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  deleteTeam,
  fetchTeam,
  leaveTeam,
  regenerateInviteCode,
  removeTeamMember,
  updateTeam,
} from '@/api/teams'
import { errorCode, errorDetails, errorMessage } from '@/api/http'
import InviteCodeBox from '@/components/team/InviteCodeBox.vue'
import TeamForm from '@/components/team/TeamForm.vue'
import TeamMemberList from '@/components/team/TeamMemberList.vue'
import TeamAnalysisList from '@/components/team/TeamAnalysisList.vue'

const route = useRoute()
const router = useRouter()

const team = ref(null)
const loading = ref(true)
const notFound = ref(false)
const loadError = ref('')
const actionError = ref('')
const busy = ref(false)

const editing = ref(false)
const editFieldErrors = ref({})
const editError = ref('')

const analysisList = ref(null)

const isOwner = computed(() => team.value?.myRole === 'OWNER')

let lastRequest = 0
// 화면이 보여 주는 팀이 바뀔 때마다 올린다. 같은 컴포넌트가 다른 팀(/teams/:id)에 재사용되므로
// 변경 동작은 시작할 때의 세대를 잡아 두고, 응답이 왔을 때 세대가 바뀌었으면 결과를 버린다 (#84).
let generation = 0

/** 지금 화면의 팀에 대한 동작인지 확인하는 함수를 돌려준다. */
function currentTeamGuard() {
  const started = generation
  return () => started === generation
}

async function load(id = Number(route.params.id), { quiet = false } = {}) {
  const request = ++lastRequest
  if (!quiet) loading.value = true
  notFound.value = false
  loadError.value = ''
  try {
    const result = await fetchTeam(id)
    if (request !== lastRequest) return
    team.value = result
  } catch (error) {
    if (request !== lastRequest) return
    team.value = null
    if (errorCode(error) === 'TEAM_404') notFound.value = true
    else loadError.value = errorMessage(error)
  } finally {
    if (request === lastRequest) loading.value = false
  }
}

watch(
  () => route.params.id,
  (id) => {
    if (route.name !== 'team-detail') return
    generation++
    busy.value = false
    editing.value = false
    actionError.value = ''
    load(Number(id))
  },
  { immediate: true },
)

// 다른 화면으로 떠난 뒤 늦게 끝난 삭제·탈퇴가 사용자를 /teams로 끌고 가지 않게 한다
onBeforeUnmount(() => {
  generation++
})

/**
 * 확인을 받은 뒤 동작을 실행하고, 실패하면 서버 메시지를 보여 준다.
 * `action(teamId, isCurrent)`은 응답 뒤 화면을 바꾸기 전에 `isCurrent()`를 확인한다.
 */
async function confirmed(message, action) {
  if (!window.confirm(message)) return
  const isCurrent = currentTeamGuard()
  busy.value = true
  actionError.value = ''
  try {
    await action(team.value.id, isCurrent)
  } catch (error) {
    if (isCurrent()) actionError.value = errorMessage(error)
  } finally {
    if (isCurrent()) busy.value = false
  }
}

function onRegenerate() {
  return confirmed('이전 코드로는 더 가입할 수 없습니다.', async (teamId, isCurrent) => {
    const { inviteCode } = await regenerateInviteCode(teamId)
    if (isCurrent()) team.value = { ...team.value, inviteCode }
  })
}

function onRemove(member) {
  return confirmed(`${member.nickname} 님을 팀에서 내보낼까요?`, async (teamId, isCurrent) => {
    await removeTeamMember(teamId, member.userId)
    if (!isCurrent()) return
    await Promise.all([load(teamId, { quiet: true }), analysisList.value?.load()])
  })
}

function onDelete() {
  return confirmed('팀과 멤버 정보가 모두 삭제됩니다.', async (teamId, isCurrent) => {
    await deleteTeam(teamId)
    if (isCurrent()) await router.push('/teams')
  })
}

function onLeave() {
  return confirmed('팀에서 탈퇴할까요?', async (teamId, isCurrent) => {
    await leaveTeam(teamId)
    if (isCurrent()) await router.push('/teams')
  })
}

function startEdit() {
  editFieldErrors.value = {}
  editError.value = ''
  actionError.value = ''
  editing.value = true
}

async function onSave(body) {
  const isCurrent = currentTeamGuard()
  busy.value = true
  editFieldErrors.value = {}
  editError.value = ''
  try {
    const updated = await updateTeam(team.value.id, body)
    if (!isCurrent()) return
    team.value = updated
    editing.value = false
  } catch (error) {
    if (!isCurrent()) return
    const details = errorCode(error) === 'COMMON_400' ? errorDetails(error) : null
    if (details && Object.keys(details).length > 0) editFieldErrors.value = details
    else editError.value = errorMessage(error)
  } finally {
    if (isCurrent()) busy.value = false
  }
}
</script>

<template>
  <article class="team-detail">
    <RouterLink to="/teams" class="back" data-action="back-to-list">← 내 팀</RouterLink>

    <p v-if="loading" class="hint">불러오는 중…</p>

    <div v-else-if="notFound" class="not-found">
      <p>팀을 찾을 수 없습니다.</p>
      <RouterLink to="/teams" data-action="back-to-teams">← 내 팀으로</RouterLink>
    </div>

    <div v-else-if="loadError" class="load-error">
      <p class="form-error" role="alert">{{ loadError }}</p>
      <button type="button" class="secondary" data-action="retry" @click="load()">다시 시도</button>
    </div>

    <template v-else-if="team">
      <TeamForm
        v-if="editing"
        class="edit"
        data-form="edit"
        submit-label="저장"
        cancelable
        :initial="team"
        :busy="busy"
        :field-errors="editFieldErrors"
        :form-error="editError"
        @submit="onSave"
        @cancel="editing = false"
      />
      <header v-else>
        <h1>{{ team.name }}</h1>
        <p v-if="team.description" class="description" data-role="description">
          {{ team.description }}
        </p>
      </header>

      <InviteCodeBox
        v-if="isOwner && team.inviteCode"
        :code="team.inviteCode"
        :busy="busy"
        @regenerate="onRegenerate"
      />

      <section class="members">
        <h2 data-role="member-count">멤버 {{ team.memberCount }}/{{ team.maxMembers }}</h2>
        <TeamMemberList
          :members="team.members"
          :can-manage="isOwner"
          :busy="busy"
          @remove="onRemove"
        />
      </section>

      <TeamAnalysisList ref="analysisList" :team-id="team.id" :member-count="team.memberCount" />

      <div class="actions">
        <template v-if="isOwner">
          <button
            v-if="!editing"
            type="button"
            class="secondary"
            data-action="edit"
            :disabled="busy"
            @click="startEdit"
          >
            수정
          </button>
          <button
            type="button"
            class="danger"
            data-action="delete-team"
            :disabled="busy"
            @click="onDelete"
          >
            팀 삭제
          </button>
        </template>
        <button
          v-else
          type="button"
          class="danger"
          data-action="leave"
          :disabled="busy"
          @click="onLeave"
        >
          팀 탈퇴
        </button>
      </div>
      <p v-if="actionError" class="form-error" role="alert" data-error="action">
        {{ actionError }}
      </p>
    </template>
  </article>
</template>

<style scoped>
.team-detail {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding: var(--space-8) 0;
}

.back {
  color: var(--color-text-muted);
  text-decoration: none;
}

.hint {
  color: var(--color-text-muted);
}

.not-found,
.load-error {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
}

h1 {
  margin: 0;
  overflow-wrap: anywhere;
}

h2 {
  margin: 0 0 var(--space-2);
  font-size: 1.1em;
}

.description {
  margin: var(--space-2) 0 0;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--space-2);
}

.secondary,
.danger {
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius);
  background: transparent;
  font: inherit;
  cursor: pointer;
}

.secondary {
  border: 1px solid var(--color-border);
  color: inherit;
}

.danger {
  border: 1px solid var(--color-danger);
  color: var(--color-danger);
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.form-error {
  margin: 0;
  color: var(--color-danger);
}
</style>
