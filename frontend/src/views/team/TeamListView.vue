<script setup>
// SC-22 내 팀 (F-20a AC12·AC13). 내 팀 카드 목록, 팀 만들기, 초대 코드로 가입.
// `/teams?code=XXXX`로 들어오면 코드 입력란만 미리 채운다 (자동 가입하지 않는다).
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { INVITE_CODE_LENGTH, createTeam, fetchMyTeams, joinTeam } from '@/api/teams'
import { errorCode, errorDetails, errorMessage } from '@/api/http'
import EmptyState from '@/components/common/EmptyState.vue'
import TeamCard from '@/components/team/TeamCard.vue'
import TeamForm from '@/components/team/TeamForm.vue'

const route = useRoute()
const router = useRouter()

const teams = ref([])
const loading = ref(true)
const loadError = ref('')

const creating = ref(false)
const createFieldErrors = ref({})
const createError = ref('')

const joinCode = ref('')
const joining = ref(false)
const joinFieldError = ref('')
const joinError = ref('')

// 공백을 먼저 빼고 8자로 자른다. 입력란에 maxlength를 두지 않는 것은 브라우저가 붙여 넣는 순간 공백을 포함해
// 8자로 잘라 " K7M2QX9P"가 7자 코드가 되기 때문이다 (8자 제한은 이 함수가 맡는다).
function normalizeCode(value) {
  return String(value ?? '')
    .replace(/\s+/g, '')
    .toUpperCase()
    .slice(0, INVITE_CODE_LENGTH)
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    teams.value = await fetchMyTeams()
  } catch (error) {
    loadError.value = errorMessage(error)
  } finally {
    loading.value = false
  }
}

onMounted(load)

watch(
  () => route.query.code,
  (code) => {
    if (typeof code === 'string' && code) joinCode.value = normalizeCode(code)
  },
  { immediate: true },
)

/** 400이면 details를 필드별로, 그 밖에는 서버 메시지를 폼 아래에 둔다. */
function splitError(error) {
  const details = errorCode(error) === 'COMMON_400' ? errorDetails(error) : null
  if (details && Object.keys(details).length > 0) return { fields: details, form: '' }
  return { fields: {}, form: errorMessage(error) }
}

async function onCreate(body) {
  creating.value = true
  createFieldErrors.value = {}
  createError.value = ''
  try {
    const { id } = await createTeam(body)
    await router.push(`/teams/${id}`)
  } catch (error) {
    const { fields, form } = splitError(error)
    createFieldErrors.value = fields
    createError.value = form
  } finally {
    creating.value = false
  }
}

function onCodeInput(event) {
  const value = normalizeCode(event.target.value)
  joinCode.value = value
  event.target.value = value
}

async function onJoin() {
  if (!joinCode.value.trim() || joining.value) return
  joining.value = true
  joinFieldError.value = ''
  joinError.value = ''
  try {
    const { teamId } = await joinTeam(joinCode.value)
    await router.push(`/teams/${teamId}`)
  } catch (error) {
    const { fields, form } = splitError(error)
    joinFieldError.value = fields.inviteCode ?? ''
    joinError.value = form || (fields.inviteCode ? '' : errorMessage(error))
  } finally {
    joining.value = false
  }
}
</script>

<template>
  <section class="teams">
    <h1>내 팀</h1>

    <p v-if="loading" class="hint">불러오는 중…</p>
    <div v-else-if="loadError" class="load-error">
      <p class="form-error" role="alert">{{ loadError }}</p>
      <button type="button" class="secondary" data-action="retry" @click="load">다시 시도</button>
    </div>
    <EmptyState
      v-else-if="teams.length === 0"
      message="아직 속한 팀이 없습니다. 팀을 만들거나 초대 코드로 가입해 보세요."
    />
    <div v-else class="grid">
      <TeamCard v-for="team in teams" :key="team.id" :team="team" />
    </div>

    <div class="panels">
      <section class="panel" data-form="create-panel">
        <h2>팀 만들기</h2>
        <TeamForm
          data-form="create"
          submit-label="만들기"
          :busy="creating"
          :field-errors="createFieldErrors"
          :form-error="createError"
          @submit="onCreate"
        />
      </section>

      <section class="panel">
        <h2>초대 코드로 가입</h2>
        <form class="join" data-form="join" novalidate @submit.prevent="onJoin">
          <label class="field">
            <span>초대 코드</span>
            <input
              :value="joinCode"
              name="inviteCode"
              type="text"
              autocomplete="off"
              autocapitalize="characters"
              placeholder="예: K7M2QX9P"
              @input="onCodeInput"
            />
          </label>
          <p v-if="joinFieldError" class="field-error" data-error="inviteCode">
            {{ joinFieldError }}
          </p>
          <p v-if="joinError" class="form-error" role="alert" data-error="form">
            {{ joinError }}
          </p>
          <div class="actions">
            <button type="submit" class="primary" :disabled="!joinCode.trim() || joining">
              가입
            </button>
          </div>
        </form>
      </section>
    </div>
  </section>
</template>

<style scoped>
.teams {
  padding: var(--space-8) 0;
}

h1 {
  margin: 0 0 var(--space-4);
}

h2 {
  margin: 0 0 var(--space-3);
  font-size: 1.1em;
}

.hint {
  color: var(--color-text-muted);
}

.load-error {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 16rem), 1fr));
  gap: var(--space-3);
}

.panels {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 20rem), 1fr));
  gap: var(--space-4);
  margin-top: var(--space-8);
}

.panel {
  min-width: 0;
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
}

.join {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  font-weight: 600;
}

.field input {
  width: 100%;
  box-sizing: border-box;
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  font: inherit;
  font-weight: normal;
  letter-spacing: 0.1em;
}

.field-error,
.form-error {
  margin: 0;
  color: var(--color-danger);
}

.actions {
  display: flex;
  justify-content: flex-end;
}

.primary,
.secondary {
  padding: var(--space-2) var(--space-6);
  border-radius: var(--radius);
  font: inherit;
  cursor: pointer;
}

.primary {
  border: none;
  background: var(--color-primary);
  color: #fff;
}

.secondary {
  border: 1px solid var(--color-border);
  background: transparent;
  color: inherit;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
