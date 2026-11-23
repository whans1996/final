<script setup>
// SC-20 관리자 카드 표 (F-13 AC13). 640px 이하에서는 같은 마크업을 CSS로 카드형 행으로 바꾼다 (각 칸 앞에 data-label을 보인다).
import { RouterLink } from 'vue-router'
import { categoryLabel, difficultyLabel, sourceLabel, statusLabel } from './labels'

defineProps({
  /** AdminConceptSummaryResponse[] */
  items: { type: Array, required: true },
})

const date = (iso) => (typeof iso === 'string' ? iso.slice(0, 10) : '')
</script>

<template>
  <table class="concepts">
    <thead>
      <tr>
        <th scope="col">이름</th>
        <th scope="col">카테고리</th>
        <th scope="col">난이도</th>
        <th scope="col">출처</th>
        <th scope="col">상태</th>
        <th scope="col" class="num">찜</th>
        <th scope="col" class="num">평점</th>
        <th scope="col">수정일</th>
        <th scope="col"><span class="sr-only">작업</span></th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="concept in items" :key="concept.id" :data-concept-id="concept.id">
        <td class="name" data-label="이름">{{ concept.name }}</td>
        <td data-label="카테고리">{{ categoryLabel(concept.category) }}</td>
        <td data-label="난이도">{{ difficultyLabel(concept.difficulty) }}</td>
        <td data-label="출처">{{ sourceLabel(concept.source) }}</td>
        <td data-label="상태">
          <span class="status" :class="concept.status.toLowerCase()">
            {{ statusLabel(concept.status) }}
          </span>
        </td>
        <td class="num" data-label="찜">{{ concept.wishCount }}</td>
        <td class="num" data-label="평점">{{ concept.avgRating.toFixed(1) }}</td>
        <td data-label="수정일">{{ date(concept.updatedAt) }}</td>
        <td class="action">
          <RouterLink
            :to="{ name: 'admin-concept-edit', params: { id: concept.id } }"
            data-action="edit"
            :class="{ primary: concept.status === 'DRAFT' }"
          >
            {{ concept.status === 'DRAFT' ? '검수' : '수정' }}
          </RouterLink>
        </td>
      </tr>
    </tbody>
  </table>
</template>

<style scoped>
.concepts {
  width: 100%;
  margin-top: var(--space-4);
  border-collapse: collapse;
  background: var(--color-surface);
}

th,
td {
  padding: var(--space-2) var(--space-3);
  border-bottom: 1px solid var(--color-border);
  text-align: left;
  white-space: nowrap;
}

th {
  color: var(--color-text-muted);
  font-size: 0.9em;
  font-weight: 600;
}

.name {
  white-space: normal;
  font-weight: 600;
}

.num {
  text-align: right;
}

.status {
  padding: 2px var(--space-2);
  border-radius: 999px;
  font-size: 0.85em;
  background: var(--color-background);
}

.status.draft {
  color: #b45309;
  background: #fef3c7;
}

.status.published {
  color: #047857;
  background: #d1fae5;
}

.status.hidden {
  color: var(--color-text-muted);
}

.action a {
  display: inline-block;
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  color: inherit;
  text-decoration: none;
}

.action a.primary {
  color: #fff;
  background: var(--color-primary);
  border-color: var(--color-primary);
  font-weight: 700;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
}

@media (max-width: 640px) {
  thead {
    display: none;
  }

  table,
  tbody,
  tr,
  td {
    display: block;
  }

  .concepts {
    background: none;
  }

  tr {
    margin-bottom: var(--space-3);
    padding: var(--space-3);
    border: 1px solid var(--color-border);
    border-radius: var(--radius);
    background: var(--color-surface);
  }

  td {
    display: flex;
    justify-content: space-between;
    gap: var(--space-3);
    padding: var(--space-1) 0;
    border: 0;
    white-space: normal;
  }

  td[data-label]::before {
    content: attr(data-label);
    color: var(--color-text-muted);
    font-size: 0.9em;
    font-weight: 400;
  }

  td.name {
    font-size: 1.05em;
  }

  td.action {
    justify-content: flex-end;
    padding-top: var(--space-2);
  }
}
</style>
