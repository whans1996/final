<script setup>
// SC-03 회원가입 (F-01). 가입에 성공하면 같은 자격 증명으로 로그인한 뒤 redirect 쿼리 또는 홈으로 이동한다.
import { reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { signup } from '@/api/auth'
import { errorDetails, errorMessage } from '@/api/http'
import { redirectTarget } from '@/router'
import { LEVELS, validateSignup } from '@/utils/authValidation'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const form = reactive({ email: '', password: '', nickname: '', level: '' })
const fieldErrors = ref({})
const formError = ref('')
const submitting = ref(false)

async function onSubmit() {
  formError.value = ''
  fieldErrors.value = validateSignup(form)
  if (Object.keys(fieldErrors.value).length > 0) return

  submitting.value = true
  try {
    await signup({ ...form })
  } catch (error) {
    fieldErrors.value = errorDetails(error)
    if (Object.keys(fieldErrors.value).length === 0) formError.value = errorMessage(error)
    submitting.value = false
    return
  }
  try {
    await auth.login({ email: form.email, password: form.password })
    await router.push(redirectTarget(route.query))
  } catch {
    // 가입은 끝났으므로 다시 제출하게 두지 않고(409) 로그인 화면에서 이어 가게 한다
    await router.push({ name: 'login', query: { ...route.query, joined: '1' } })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="auth">
    <h1>회원가입</h1>
    <form class="auth-form" novalidate @submit.prevent="onSubmit">
      <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>

      <label class="field">
        <span>이메일</span>
        <input
          v-model="form.email"
          name="email"
          type="email"
          autocomplete="email"
          :aria-invalid="!!fieldErrors.email"
        />
        <small v-if="fieldErrors.email" class="field-error" data-error="email">
          {{ fieldErrors.email }}
        </small>
      </label>

      <label class="field">
        <span>비밀번호</span>
        <input
          v-model="form.password"
          name="password"
          type="password"
          autocomplete="new-password"
          :aria-invalid="!!fieldErrors.password"
        />
        <small v-if="fieldErrors.password" class="field-error" data-error="password">
          {{ fieldErrors.password }}
        </small>
        <small v-else class="hint">8~64자 영문·숫자·특수문자, 영문과 숫자 모두 포함</small>
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

      <fieldset class="levels">
        <legend>내 학습 수준</legend>
        <p class="hint">설명의 기본 난이도가 됩니다. 분석할 때마다 바꿀 수 있어요.</p>
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

      <button class="submit" type="submit" :disabled="submitting">
        {{ submitting ? '가입 중…' : '가입하기' }}
      </button>
    </form>
    <p class="switch">
      이미 계정이 있나요?
      <RouterLink :to="{ name: 'login', query: route.query }">로그인</RouterLink>
    </p>
  </section>
</template>

<style scoped>
.auth {
  max-width: 480px;
  margin: 0 auto;
  padding: var(--space-8) 0;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  margin-top: var(--space-6);
}

.field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  font-weight: 600;
}

.field input {
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  font: inherit;
  font-weight: 400;
  background: var(--color-surface);
}

.field input[aria-invalid='true'] {
  border-color: var(--color-danger);
}

.hint {
  color: var(--color-text-muted);
  font-weight: 400;
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
  background: var(--color-surface);
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

.submit {
  padding: var(--space-3);
  border: none;
  border-radius: var(--radius);
  color: #fff;
  background: var(--color-primary);
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.submit:disabled {
  opacity: 0.6;
  cursor: wait;
}

.switch {
  margin-top: var(--space-6);
  text-align: center;
  color: var(--color-text-muted);
}
</style>
