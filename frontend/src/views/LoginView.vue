<script setup>
// SC-02 로그인 (F-02). 성공하면 redirect 쿼리 또는 대시보드(F-16)로 이동한다.
import { reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { errorDetails, errorMessage } from '@/api/http'
import { redirectTarget } from '@/router'
import { validateLogin } from '@/utils/authValidation'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const form = reactive({ email: '', password: '' })
const fieldErrors = ref({})
const formError = ref('')
const submitting = ref(false)

async function onSubmit() {
  formError.value = ''
  fieldErrors.value = validateLogin(form)
  if (Object.keys(fieldErrors.value).length > 0) return

  submitting.value = true
  try {
    await auth.login({ email: form.email, password: form.password })
    await router.push(redirectTarget(route.query))
  } catch (error) {
    fieldErrors.value = errorDetails(error)
    formError.value = errorMessage(error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="auth">
    <h1>로그인</h1>
    <form class="auth-form" novalidate @submit.prevent="onSubmit">
      <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>
      <p v-else-if="route.query.joined" class="form-notice" role="status">
        가입되었습니다. 로그인해 주세요.
      </p>

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
          autocomplete="current-password"
          :aria-invalid="!!fieldErrors.password"
        />
        <small v-if="fieldErrors.password" class="field-error" data-error="password">
          {{ fieldErrors.password }}
        </small>
      </label>

      <button class="submit" type="submit" :disabled="submitting">
        {{ submitting ? '로그인 중…' : '로그인' }}
      </button>
    </form>
    <p class="switch">
      계정이 없나요?
      <RouterLink :to="{ name: 'signup', query: route.query }">회원가입</RouterLink>
    </p>
  </section>
</template>

<style scoped>
.auth {
  max-width: 420px;
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

.field-error,
.form-error {
  color: var(--color-danger);
  font-weight: 400;
}

.form-notice {
  color: var(--color-primary);
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
