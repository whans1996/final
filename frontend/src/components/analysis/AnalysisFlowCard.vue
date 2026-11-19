<script setup>
// 실행 흐름 하나 (F-07 AC15·AC16). PENDING은 안내, DONE은 요약과 단계 목록, FAILED는 사유와 "다시 만들기".
import { layerLabel } from '@/utils/layers'
import FilePath from '@/components/analysis/FilePath.vue'
import MethodBadge from '@/components/analysis/MethodBadge.vue'

defineProps({
  flow: { type: Object, required: true },
  busy: { type: Boolean, default: false },
})

defineEmits(['retry', 'select-file'])

const STATUS_LABELS = { PENDING: '생성 중', DONE: '완료', FAILED: '실패' }
</script>

<template>
  <article class="flow-card" :data-flow="flow.id">
    <header class="head">
      <MethodBadge :method="flow.method" />
      <span class="endpoint"><FilePath :path="flow.path" /></span>
      <span class="status" :data-status="flow.status">{{ STATUS_LABELS[flow.status] }}</span>
    </header>

    <p v-if="flow.status === 'PENDING'" class="hint" role="status">실행 흐름을 만드는 중입니다…</p>

    <div v-else-if="flow.status === 'FAILED'" class="failed">
      <p class="form-error">{{ flow.errorMessage }}</p>
      <button
        type="button"
        class="retry-button"
        data-action="retry-flow"
        :disabled="busy"
        @click="$emit('retry', flow)"
      >
        다시 만들기
      </button>
    </div>

    <template v-else>
      <p class="summary">{{ flow.summary }}</p>
      <ol class="steps">
        <li v-for="step in flow.steps ?? []" :key="step.no" class="step" data-step>
          <span class="no">{{ step.no }}</span>
          <div class="body">
            <p class="title">
              <span v-if="step.layer" class="layer">{{ layerLabel(step.layer) }}</span>
              <strong>{{ step.title }}</strong>
              <code v-if="step.symbol" class="symbol">{{ step.symbol }}</code>
            </p>
            <p class="path">
              <button
                v-if="step.fileId"
                type="button"
                class="file-link"
                data-action="open-file"
                @click="$emit('select-file', step.fileId)"
              >
                <FilePath :path="step.path" />
              </button>
              <FilePath v-else :path="step.path" />
            </p>
            <p class="description">{{ step.description }}</p>
          </div>
        </li>
      </ol>
    </template>
  </article>
</template>

<style scoped>
.flow-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  min-width: 0;
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
}

.head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
}

.endpoint {
  min-width: 0;
  font-weight: 600;
}

.status {
  margin-left: auto;
  padding: 0 var(--space-2);
  border-radius: 999px;
  font-size: 0.8em;
  color: var(--color-text-muted);
  background: var(--color-border);
}

.status[data-status='DONE'] {
  color: #065f46;
  background: #d1fae5;
}

.status[data-status='FAILED'] {
  color: #991b1b;
  background: #fee2e2;
}

.summary {
  margin: 0;
}

.steps {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin: 0;
  padding: 0;
  list-style: none;
}

.step {
  display: flex;
  gap: var(--space-3);
}

.no {
  flex: none;
  width: 1.8em;
  height: 1.8em;
  border-radius: 50%;
  color: #fff;
  background: var(--color-primary);
  font-size: 0.85em;
  font-weight: 700;
  line-height: 1.8em;
  text-align: center;
}

.body {
  min-width: 0;
}

.body p {
  margin: 0 0 var(--space-1);
}

.title {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--space-2);
}

.layer {
  color: var(--color-text-muted);
  font-size: 0.85em;
}

.symbol {
  overflow-wrap: anywhere;
  font-size: 0.85em;
}

.path {
  font-size: 0.85em;
  color: var(--color-text-muted);
}

.file-link {
  padding: 0;
  border: none;
  color: var(--color-primary);
  background: none;
  font: inherit;
  text-align: left;
  text-decoration: underline;
  cursor: pointer;
}

.description {
  overflow-wrap: anywhere;
}

.hint {
  margin: 0;
  color: var(--color-text-muted);
}

.failed {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
}

.failed .form-error {
  margin: 0;
}

.retry-button {
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius);
  color: var(--color-primary);
  background: var(--color-surface);
  font: inherit;
  cursor: pointer;
}

.retry-button:disabled {
  opacity: 0.5;
  cursor: default;
}
</style>
