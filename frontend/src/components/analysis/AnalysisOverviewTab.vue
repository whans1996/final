<script setup>
// SC-07 "개요" 탭 (F-06 AC13): 요약, 기술 스택, 레이어별 역할, 읽는 순서, API 목록.
// 읽는 순서·API의 파일 링크는 같은 화면의 쿼리(?tab=files&file=id)로 이동해 파일 탭에서 그 파일을 고른다.
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { layerLabel, sortByLayer } from '@/utils/layers'
import FilePath from '@/components/analysis/FilePath.vue'
import MethodBadge from '@/components/analysis/MethodBadge.vue'

const props = defineProps({
  overview: { type: Object, default: null },
})

const layers = computed(() => sortByLayer(props.overview?.layers ?? []))
const readingOrder = computed(() => props.overview?.readingOrder ?? [])
const apis = computed(() => props.overview?.apis ?? [])

function fileLink(fileId) {
  return { query: { tab: 'files', file: String(fileId) } }
}
</script>

<template>
  <div v-if="!overview" class="overview">
    <p class="hint">개요가 아직 없습니다.</p>
  </div>

  <div v-else class="overview">
    <section data-section="summary">
      <h2>프로젝트 요약</h2>
      <p class="summary">{{ overview.summary }}</p>
    </section>

    <section v-if="overview.techStack?.length" data-section="tech-stack">
      <h2>기술 스택</h2>
      <ul class="chips">
        <li v-for="tech in overview.techStack" :key="tech" class="chip">{{ tech }}</li>
      </ul>
    </section>

    <section v-if="layers.length" data-section="layers">
      <h2>레이어별 역할</h2>
      <ul class="layers">
        <li v-for="item in layers" :key="item.layer">
          <span class="layer-label">{{ layerLabel(item.layer) }}</span>
          <span>{{ item.role }}</span>
        </li>
      </ul>
    </section>

    <section v-if="readingOrder.length" data-section="reading-order">
      <h2>이 순서로 읽어 보세요</h2>
      <ol class="reading">
        <li v-for="item in readingOrder" :key="item.fileId">
          <RouterLink :to="fileLink(item.fileId)" class="path"
            ><FilePath :path="item.path"
          /></RouterLink>
          <p class="hint">{{ item.reason }}</p>
        </li>
      </ol>
    </section>

    <section data-section="apis">
      <h2>API 목록</h2>
      <p v-if="!apis.length" class="hint">탐지된 API가 없습니다.</p>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th scope="col">메서드</th>
              <th scope="col">경로</th>
              <th scope="col">파일</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="api in apis" :key="`${api.method} ${api.path}`">
              <td>
                <MethodBadge :method="api.method" />
              </td>
              <td class="path">{{ api.path }}</td>
              <td>
                <RouterLink
                  v-if="api.fileId != null"
                  :to="fileLink(api.fileId)"
                  :aria-label="`${api.method} ${api.path} 코드 보기`"
                >
                  코드 보기
                </RouterLink>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.overview {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

h2 {
  margin-bottom: var(--space-2);
  font-size: 1.05em;
}

.hint {
  color: var(--color-text-muted);
  font-size: 0.9em;
}

.summary {
  white-space: pre-line;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  padding: 0;
  list-style: none;
}

.chip {
  padding: 2px var(--space-3);
  border-radius: 999px;
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 10%, transparent);
  font-size: 0.85em;
  font-weight: 600;
}

.layers {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: 0;
  list-style: none;
}

.layers li {
  display: grid;
  grid-template-columns: 7em 1fr;
  gap: var(--space-3);
}

.layer-label {
  font-weight: 700;
}

.reading {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding-left: 1.4em;
}

.path {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 0.9em;
  overflow-wrap: anywhere;
}

.table-wrap {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9em;
}

th,
td {
  padding: var(--space-2);
  border-bottom: 1px solid var(--color-border);
  text-align: left;
  vertical-align: top;
}

th {
  color: var(--color-text-muted);
  font-weight: 600;
}

td:last-child {
  white-space: nowrap;
}

@media (max-width: 480px) {
  .layers li {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
