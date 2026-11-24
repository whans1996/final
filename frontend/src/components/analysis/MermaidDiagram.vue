<script setup>
// mermaid 원본을 그림으로 그리고 원본 보기·복사를 붙인다 (F-21a AC16·AC17, F-21b에서 재사용).
// mermaid는 이 컴포넌트가 처음 그릴 때만 동적 import한다. strict라 라벨의 HTML·클릭 이벤트를 쓰지 않는다.
// 렌더링이 실패하면 그림 자리에만 실패 문구를 두고 원본 보기는 그대로 둔다.
import { onMounted, ref, watch } from 'vue'

const props = defineProps({
  code: { type: String, required: true },
  label: { type: String, default: '다이어그램' },
})

const COPIED = '복사했습니다.'
const COPY_FAILED = '복사하지 못했습니다. 코드를 직접 선택해 복사해 주세요.'

let mermaidLoader = null
let renderCount = 0

function loadMermaid() {
  if (!mermaidLoader) {
    mermaidLoader = import('mermaid').then(({ default: mermaid }) => {
      // 그림이 커지면 상자 안에서 가로로 스크롤한다 (줄여 맞추면 좁은 화면에서 글자를 읽을 수 없다)
      mermaid.initialize({
        startOnLoad: false,
        securityLevel: 'strict',
        flowchart: { useMaxWidth: false },
      })
      return mermaid
    })
    // 불러오기가 실패하면 다음에 다시 시도한다
    mermaidLoader.catch(() => {
      mermaidLoader = null
    })
  }
  return mermaidLoader
}

const diagramEl = ref(null)
const rendering = ref(true)
const failed = ref(false)
const copyResult = ref('')

let request = 0

async function draw() {
  const current = ++request
  rendering.value = true
  failed.value = false
  try {
    const mermaid = await loadMermaid()
    const { svg } = await mermaid.render(`mermaid-diagram-${++renderCount}`, props.code)
    if (current !== request) return
    // strict 모드의 mermaid가 DOMPurify로 정리한 SVG다
    if (diagramEl.value) diagramEl.value.innerHTML = svg
  } catch {
    if (current !== request) return
    if (diagramEl.value) diagramEl.value.innerHTML = ''
    failed.value = true
  } finally {
    if (current === request) rendering.value = false
  }
}

async function copySource() {
  try {
    if (!navigator.clipboard?.writeText) throw new Error('clipboard unavailable')
    await navigator.clipboard.writeText(props.code)
    copyResult.value = COPIED
  } catch {
    copyResult.value = COPY_FAILED
  }
}

onMounted(draw)
watch(
  () => props.code,
  () => {
    copyResult.value = ''
    draw()
  },
)
</script>

<template>
  <div class="mermaid-diagram">
    <div class="diagram-box">
      <p v-if="rendering" class="hint">그림을 그리는 중…</p>
      <p v-else-if="failed" class="form-error" role="alert" data-message="render-failed">
        다이어그램을 그리지 못했습니다.
      </p>
      <div
        v-show="!rendering && !failed"
        ref="diagramEl"
        class="diagram"
        role="img"
        :aria-label="label"
        data-role="diagram"
      ></div>
    </div>

    <details class="source" data-section="source">
      <summary>mermaid 코드 보기</summary>
      <div class="source-body">
        <button type="button" class="copy-button" data-action="copy-source" @click="copySource">
          복사
        </button>
        <p v-if="copyResult" class="copy-result" role="status" data-role="copy-result">
          {{ copyResult }}
        </p>
        <pre data-role="source">{{ code }}</pre>
      </div>
    </details>
  </div>
</template>

<style scoped>
.mermaid-diagram {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  min-width: 0;
}

.diagram-box {
  min-width: 0;
  overflow-x: auto;
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
}

.diagram :deep(svg) {
  display: block;
  margin: 0 auto;
}

.source summary {
  cursor: pointer;
  font-weight: 600;
}

.source-body {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-2);
  margin-top: var(--space-2);
}

.source pre {
  align-self: stretch;
  max-height: 320px;
  margin: 0;
  overflow: auto;
  padding: var(--space-3);
  border-radius: var(--radius);
  background: var(--color-background);
  font-size: 0.85em;
}

.copy-button {
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  color: inherit;
  background: transparent;
  font: inherit;
  cursor: pointer;
}

.copy-result {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.9em;
}

.hint {
  margin: 0;
  color: var(--color-text-muted);
}

.form-error {
  margin: 0;
  color: var(--color-danger);
}
</style>
