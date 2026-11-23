<script setup>
// 팀장 전용 초대 코드 상자 (F-20a AC14·AC15). 코드 복사, 초대 링크(`/teams?code=`) 복사, 재발급(확인은 부모가 한다).
import { ref } from 'vue'

const props = defineProps({
  code: { type: String, required: true },
  busy: { type: Boolean, default: false },
})

const emit = defineEmits(['regenerate'])

const COPIED = '복사했습니다'
const COPY_FAILED = '복사하지 못했습니다. 코드를 직접 복사해 주세요.'

const copyResult = ref('')

async function copy(text) {
  try {
    if (!navigator.clipboard?.writeText) throw new Error('clipboard unavailable')
    await navigator.clipboard.writeText(text)
    copyResult.value = COPIED
  } catch {
    copyResult.value = COPY_FAILED
  }
}

const copyCode = () => copy(props.code)
const copyLink = () => copy(`${window.location.origin}/teams?code=${props.code}`)
</script>

<template>
  <section class="invite" aria-label="초대 코드">
    <div class="code-row">
      <span class="label">초대 코드</span>
      <code class="code" data-role="invite-code">{{ code }}</code>
    </div>
    <div class="buttons">
      <button type="button" data-action="copy-code" @click="copyCode">코드 복사</button>
      <button type="button" data-action="copy-link" @click="copyLink">초대 링크 복사</button>
      <button type="button" data-action="regenerate" :disabled="busy" @click="emit('regenerate')">
        재발급
      </button>
    </div>
    <p v-if="copyResult" class="result" role="status" data-role="copy-result">
      {{ copyResult }}
    </p>
  </section>
</template>

<style scoped>
.invite {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-4);
  border: 1px dashed var(--color-primary);
  border-radius: var(--radius);
}

.code-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
}

.label {
  font-weight: 600;
}

.code {
  font-size: 1.25em;
  font-weight: 700;
  letter-spacing: 0.15em;
  user-select: all;
}

.buttons {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.buttons button {
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: transparent;
  color: inherit;
  font: inherit;
  cursor: pointer;
}

.buttons button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.result {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.9em;
}
</style>
