<script setup>
// 예제 코드 + 복사 버튼 (F-10 AC9). 복사하면 2초간 "복사됨", 실패하면 안내를 보여 준다.
import { onBeforeUnmount, ref } from 'vue'

const props = defineProps({
  code: { type: String, required: true },
})

const COPIED_MS = 2000

const copied = ref(false)
const failed = ref(false)
let timer = null

async function copy() {
  failed.value = false
  try {
    await navigator.clipboard.writeText(props.code)
    copied.value = true
    clearTimeout(timer)
    timer = setTimeout(() => {
      copied.value = false
    }, COPIED_MS)
  } catch {
    copied.value = false
    failed.value = true
  }
}

onBeforeUnmount(() => clearTimeout(timer))
</script>

<template>
  <div class="code-block">
    <div class="toolbar">
      <span v-if="failed" class="form-error" role="alert">복사하지 못했습니다.</span>
      <button type="button" class="copy" data-action="copy-code" @click="copy">
        {{ copied ? '복사됨' : '복사' }}
      </button>
    </div>
    <pre><code>{{ code }}</code></pre>
  </div>
</template>

<style scoped>
.code-block {
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-background);
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3) 0;
  font-size: 0.9em;
}

.copy {
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  cursor: pointer;
}

pre {
  margin: 0;
  padding: var(--space-3);
  overflow-x: auto;
  font-size: 0.9em;
  line-height: 1.5;
}
</style>
