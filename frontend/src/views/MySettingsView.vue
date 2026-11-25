<script setup>
// SC-19 내 정보 수정·탈퇴 (F-03 AC10). 저장하면 헤더 닉네임·수준(store)에 바로 반영한다.
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { fetchMe, updateMe, withdraw } from '@/api/users'
import { errorDetails, errorMessage } from '@/api/http'
import { BIO_MAX, LEVELS, validateProfile } from '@/utils/authValidation'

const auth = useAuthStore()
const router = useRouter()

const loading = ref(true)
const loadError = ref('')
const email = ref('')

const form = reactive({ nickname: '', bio: '', level: '' })
const fieldErrors = ref({})
const formError = ref('')
const saved = ref(false)
const saving = ref(false)

const leave = reactive({ password: '', confirmed: false })
const leaveErrors = ref({})
const leaveError = ref('')
const leaving = ref(false)
const canLeave = computed(() => !!leave.password && leave.confirmed && !leaving.value)

function fillForm(me) {
  email.value = me.email
  form.nickname = me.nickname
  form.bio = me.bio ?? ''
  form.level = me.level
}

onMounted(async () => {
  try {
    fillForm(await fetchMe())
  } catch (error) {
    loadError.value = errorMessage(error)
  } finally {
    loading.value = false
  }
})

async function onSave() {
  formError.value = ''
  saved.value = false
  fieldErrors.value = validateProfile(form)
  if (Object.keys(fieldErrors.value).length > 0) return

  saving.value = true
  try {
    const me = await updateMe({ nickname: form.nickname.trim(), bio: form.bio, level: form.level })
    fillForm(me)
    auth.updateUser({ nickname: me.nickname, level: me.level })
    saved.value = true
  } catch (error) {
    fieldErrors.value = errorDetails(error)
    if (Object.keys(fieldErrors.value).length === 0) formError.value = errorMessage(error)
  } finally {
    saving.value = false
  }
}

async function onWithdraw() {
  if (!canLeave.value) return
  leaveErrors.value = {}
  leaveError.value = ''
  leaving.value = true
  try {
    await withdraw(leave.password)
  } catch (error) {
    leaveErrors.value = errorDetails(error)
    if (!leaveErrors.value.password) leaveError.value = errorMessage(error)
    leaving.value = false
    return
  }
  // 서버가 refresh 쿠키와 모든 기기의 토큰을 이미 폐기했으므로 이 기기의 상태만 비운다
  auth.clear()
  await router.push({ name: 'home' })
}
</script>

<template>
  <section class="settings">
    <h1>내 정보</h1>
    <p v-if="loading" class="status">불러오는 중…</p>
    <p v-else-if="loadError" class="form-error" role="alert">{{ loadError }}</p>

    <template v-else>
      <form class="card" data-form="profile" novalidate @submit.prevent="onSave">
        <h2>정보 수정</h2>
        <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>

        <label class="field">
          <span>이메일</span>
          <input :value="email" name="email" type="email" disabled />
        </label>

        <label class="field">
          <span>닉네임</span>
          <input
            v-model="form.nickname"
            name="nickname"
            type="text"
            autocomplete="nickname"
            :aria-invalid="!!fieldErrors.nickname"
          />
          <small v-if="fieldErrors.nickname" class="field-error" data-error="nickname">
            {{ fieldErrors.nickname }}
          </small>
          <small v-else class="hint">2~20자, 한글·영문·숫자·_</small>
        </label>

        <label class="field">
          <span>소개</span>
          <textarea
            v-model="form.bio"
            name="bio"
            rows="4"
            :aria-invalid="!!fieldErrors.bio"
          ></textarea>
          <small v-if="fieldErrors.bio" class="field-error" data-error="bio">
            {{ fieldErrors.bio }}
          </small>
          <small v-else class="hint">{{ form.bio.trim().length }} / {{ BIO_MAX }}</small>
        </label>

        <fieldset class="levels">
          <legend>내 학습 수준</legend>
          <p class="hint">새로 요청하는 분석 설명의 기본 난이도가 됩니다.</p>
          <label
            v-for="item in LEVELS"
            :key="item.value"
            class="level"
            :class="{ selected: form.level === item.value }"
          >
            <input v-model="form.level" type="radio" name="level" :value="item.value" />
            <strong>{{ item.label }}</strong>
            <span>{{ item.description }}</span>
          </label>
          <small v-if="fieldErrors.level" class="field-error" data-error="level">
            {{ fieldErrors.level }}
          </small>
        </fieldset>

        <p v-if="saved" class="saved" data-message="profile-saved" role="status">저장했습니다.</p>
        <button class="submit" type="submit" :disabled="saving">
          {{ saving ? '저장 중…' : '저장하기' }}
        </button>
      </form>

      <form class="card danger" data-form="withdraw" novalidate @submit.prevent="onWithdraw">
        <h2>회원 탈퇴</h2>
        <p class="hint">
          탈퇴하면 이메일·닉네임·소개가 지워지고 모든 기기에서 로그아웃됩니다. 내 분석(업로드한
          코드와 결과)은 모두 삭제됩니다. 작성한 글과 댓글은 "탈퇴한 사용자"로 남습니다.
        </p>
        <p v-if="leaveError" class="form-error" role="alert">{{ leaveError }}</p>

        <label class="field">
          <span>현재 비밀번호</span>
          <input
            v-model="leave.password"
            name="withdraw-password"
            type="password"
            autocomplete="current-password"
            :aria-invalid="!!leaveErrors.password"
          />
          <small v-if="leaveErrors.password" class="field-error" data-error="withdraw-password">
            {{ leaveErrors.password }}
          </small>
        </label>

        <label class="confirm">
          <input v-model="leave.confirmed" type="checkbox" name="withdraw-confirm" />
          <span>탈퇴하면 되돌릴 수 없다는 것을 확인했습니다.</span>
        </label>

        <button class="withdraw" type="submit" data-action="withdraw" :disabled="!canLeave">
          {{ leaving ? '탈퇴 중…' : '탈퇴하기' }}
        </button>
      </form>
    </template>
  </section>
</template>

<style scoped>
.settings {
  max-width: 560px;
  margin: 0 auto;
  padding: var(--space-8) 0;
}

.status,
.hint {
  color: var(--color-text-muted);
  font-weight: 400;
}

.card {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  margin-top: var(--space-6);
  padding: var(--space-6);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
}

.card h2 {
  margin: 0;
  font-size: 1.1em;
}

.card.danger {
  border-color: color-mix(in srgb, var(--color-danger) 40%, var(--color-border));
}

.field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  font-weight: 600;
}

.field input,
.field textarea {
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  font: inherit;
  font-weight: 400;
  background: var(--color-surface);
}

.field textarea {
  resize: vertical;
}

.field input:disabled {
  color: var(--color-text-muted);
  background: var(--color-background);
}

.field input[aria-invalid='true'],
.field textarea[aria-invalid='true'] {
  border-color: var(--color-danger);
}

.field-error,
.form-error {
  color: var(--color-danger);
  font-weight: 400;
}

.levels {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: 0;
  border: none;
}

.levels legend {
  font-weight: 600;
}

.level {
  display: grid;
  grid-template-columns: auto 1fr;
  column-gap: var(--space-2);
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  cursor: pointer;
}

.level input {
  grid-row: span 2;
  align-self: center;
}

.level span {
  color: var(--color-text-muted);
  font-size: 0.9em;
}

.level.selected {
  border-color: var(--color-primary);
}

.saved {
  color: var(--color-primary);
}

.confirm {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
}

.submit,
.withdraw {
  padding: var(--space-3);
  border: none;
  border-radius: var(--radius);
  color: #fff;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.submit {
  background: var(--color-primary);
}

.withdraw {
  background: var(--color-danger);
}

.submit:disabled,
.withdraw:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
