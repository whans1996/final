<script setup>
// SC-24 멤버별 이해도 (F-20b AC12). 행 = 멤버(닉네임 → SC-18, 최근 점수), 열 = 모듈, 칸 = 맞힌 수/문항 수 또는 "–".
// 좁은 화면에서는 표 영역 안에서만 가로로 스크롤한다 (NF-02).
import { RouterLink } from 'vue-router'
import { layerLabel } from '@/utils/layers'

defineProps({
  modules: { type: Array, required: true },
  members: { type: Array, required: true },
})

function cell(member, layer) {
  const m = member.modules.find((x) => x.layer === layer)
  return m ? `${m.correct}/${m.total}` : '–'
}
</script>

<template>
  <div class="scroll">
    <table>
      <thead>
        <tr>
          <th scope="col">멤버</th>
          <th scope="col">최근 점수</th>
          <th v-for="m in modules" :key="m.layer" scope="col">{{ layerLabel(m.layer) }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="member in members" :key="member.userId" :data-member-row="member.userId">
          <th scope="row">
            <RouterLink :to="`/users/${member.userId}`">{{ member.nickname }}</RouterLink>
          </th>
          <td>
            <template v-if="member.lastAttempt">
              {{ member.lastAttempt.score }}/{{ member.lastAttempt.total }}
            </template>
            <span v-else class="muted">아직 풀지 않음</span>
          </td>
          <td v-for="m in modules" :key="m.layer" data-cell>{{ cell(member, m.layer) }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.scroll {
  max-width: 100%;
  overflow-x: auto;
}

table {
  min-width: 100%;
  border-collapse: collapse;
  white-space: nowrap;
}

th,
td {
  padding: var(--space-2) var(--space-3);
  border-bottom: 1px solid var(--color-border);
  text-align: left;
}

thead th {
  color: var(--color-text-muted);
  font-weight: normal;
  font-size: 0.9em;
}

.muted {
  color: var(--color-text-muted);
}
</style>
