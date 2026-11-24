<script setup>
// SC-07 "구조" 탭 (F-21a AC14·AC15). 파일 의존 그래프를 1회 불러와 mermaid 그림, 연결 목록, 확인하지 못한 연결을 보여 준다.
// 그림에는 간선이 있는 파일만 넣고, 나머지는 "연결 없는 파일"에 경로로 나열한다. 파일 이름은 파일 탭 링크다.
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchAnalysisStructure } from '@/api/analyses'
import { errorMessage } from '@/api/http'
import { groupByLayer } from '@/utils/layers'
import { buildStructureMermaid } from '@/utils/mermaid'
import { edgeTypeLabel, unresolvedReasonLabel } from '@/utils/structure'
import MermaidDiagram from '@/components/analysis/MermaidDiagram.vue'

const props = defineProps({
  analysisId: { type: Number, required: true },
})

const structure = ref(null)
const loading = ref(true)
const loadError = ref('')

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    structure.value = await fetchAnalysisStructure(props.analysisId)
  } catch (error) {
    loadError.value = errorMessage(error)
  } finally {
    loading.value = false
  }
}

onMounted(load)

const nodesById = computed(() => new Map((structure.value?.nodes ?? []).map((n) => [n.fileId, n])))
const hasEdges = computed(() => (structure.value?.edges.length ?? 0) > 0)
const mermaidCode = computed(() => (hasEdges.value ? buildStructureMermaid(structure.value) : ''))
const isolatedNodes = computed(() => {
  if (!structure.value) return []
  const linked = new Set(structure.value.edges.flatMap((e) => [e.from, e.to]))
  return structure.value.nodes.filter((n) => !linked.has(n.fileId))
})
const layerGroups = computed(() => groupByLayer(structure.value?.nodes ?? []))

function fileName(fileId) {
  return nodesById.value.get(fileId)?.name ?? `파일 ${fileId}`
}

function fileLink(fileId) {
  return { query: { tab: 'files', file: String(fileId) } }
}
</script>

<template>
  <div class="structure-tab">
    <p v-if="loading" class="hint">구조를 불러오는 중입니다…</p>

    <div v-else-if="loadError" class="error-box">
      <p class="form-error" role="alert">{{ loadError }}</p>
      <button type="button" class="retry-button" data-action="retry-load" @click="load">
        다시 시도
      </button>
    </div>

    <template v-else-if="structure">
      <template v-if="hasEdges">
        <p class="legend" data-role="legend">
          실선 = 코드 참조 · 점선 = API 호출 · 코드에서 확정한 연결만 그립니다
        </p>
        <p
          v-if="structure.edgesTruncated"
          class="notice"
          role="status"
          data-message="edges-truncated"
        >
          연결이 많아 150개까지만 보여 줍니다.
        </p>
        <MermaidDiagram :code="mermaidCode" label="파일 의존 그래프" />

        <details v-if="isolatedNodes.length > 0" class="fold" data-section="isolated">
          <summary>연결 없는 파일 {{ isolatedNodes.length }}개</summary>
          <ul class="rows">
            <li v-for="n in isolatedNodes" :key="n.fileId" class="path">{{ n.path }}</li>
          </ul>
        </details>

        <details class="fold" data-section="edges">
          <summary>연결 목록</summary>
          <ul class="rows">
            <li v-for="(edge, index) in structure.edges" :key="index">
              <RouterLink :to="fileLink(edge.from)">{{ fileName(edge.from) }}</RouterLink>:{{
                edge.line
              }}
              →
              <RouterLink :to="fileLink(edge.to)">{{ fileName(edge.to) }}</RouterLink>
              ({{ edgeTypeLabel(edge.type) }})
            </li>
          </ul>
        </details>
      </template>

      <template v-else>
        <p class="hint" data-message="no-edges">파일 사이 연결을 찾지 못했습니다.</p>
        <section
          v-for="group in layerGroups"
          :key="group.layer"
          class="layer-group"
          data-layer-group
        >
          <h3>{{ group.label }}</h3>
          <ul class="rows">
            <li v-for="n in group.items" :key="n.fileId" class="path">{{ n.path }}</li>
          </ul>
        </section>
      </template>

      <details v-if="structure.unresolved.length > 0" class="fold" data-section="unresolved">
        <summary>확인하지 못한 연결 {{ structure.unresolved.length }}개</summary>
        <ul class="rows">
          <li v-for="(item, index) in structure.unresolved" :key="index">
            {{ fileName(item.fileId) }}:{{ item.line }} · {{ unresolvedReasonLabel(item.reason)
            }}<template v-if="item.detail">
              · <code>{{ item.detail }}</code></template
            >
          </li>
        </ul>
      </details>
    </template>
  </div>
</template>

<style scoped>
.structure-tab {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  min-width: 0;
}

.legend,
.notice {
  margin: 0;
  font-size: 0.9em;
}

.legend {
  color: var(--color-text-muted);
}

.notice {
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius);
  background: var(--color-background);
}

.fold summary {
  cursor: pointer;
  font-weight: 600;
}

.rows {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  margin: var(--space-2) 0 0;
  padding-left: var(--space-4);
  font-size: 0.9em;
}

.rows li {
  overflow-wrap: anywhere;
}

.rows code {
  font-size: 0.95em;
}

.layer-group h3 {
  margin: 0;
  font-size: 1em;
}

.hint {
  margin: 0;
  color: var(--color-text-muted);
}

.form-error {
  color: var(--color-danger);
}

.error-box {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-3);
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
</style>
