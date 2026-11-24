<script setup>
// SC-21 관리자 챌린지 표 (F-24 AC11). 640px 이하에서는 같은 마크업을 CSS로 카드형 행으로 바꾼다 (각 칸 앞에 data-label을 보인다).
// 삭제는 확인 대화상자를 목록 화면이 띄우므로 delete 이벤트로 올린다.
import { RouterLink } from 'vue-router'
import { goalIcon, goalLabel, periodLabel, statusLabel } from './labels'

defineProps({
  /** AdminChallengeSummaryResponse[] */
  items: { type: Array, required: true },
})

const emit = defineEmits(['delete'])
</script>

<template>
  <table class="challenges">
    <thead>
      <tr>
        <th scope="col">이미지</th>
        <th scope="col">제목</th>
        <th scope="col">기간</th>
        <th scope="col">목표</th>
        <th scope="col">상태</th>
        <th scope="col" class="num">참여자</th>
        <th scope="col"><span class="sr-only">작업</span></th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="challenge in items" :key="challenge.id" :data-challenge-id="challenge.id">
        <td class="thumb-cell">
          <img
            v-if="challenge.imageUrl"
            class="thumb"
            :src="challenge.imageUrl"
            alt=""
            loading="lazy"
          />
          <span
            v-else
            class="thumb placeholder"
            :data-thumb="challenge.goalType"
            aria-hidden="true"
            >{{ goalIcon(challenge.goalType) }}</span
          >
        </td>
        <td class="title" data-label="제목">{{ challenge.title }}</td>
        <td data-label="기간">{{ periodLabel(challenge.startDate, challenge.endDate) }}</td>
        <td data-label="목표">{{ goalLabel(challenge.goalType, challenge.goalValue) }}</td>
        <td data-label="상태">
          <span class="status" :class="challenge.status.toLowerCase()">
            {{ statusLabel(challenge.status) }}
          </span>
        </td>
        <td class="num" data-label="참여자">{{ challenge.participantCount }}</td>
        <td class="action">
          <div class="buttons">
            <RouterLink
              :to="{ name: 'admin-challenge-edit', params: { id: challenge.id } }"
              data-action="edit"
            >
              수정
            </RouterLink>
            <button
              type="button"
              class="danger"
              data-action="delete"
              @click="emit('delete', challenge)"
            >
              삭제
            </button>
          </div>
        </td>
      </tr>
    </tbody>
  </table>
</template>

<style scoped>
.challenges {
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
  vertical-align: middle;
  white-space: nowrap;
}

th {
  color: var(--color-text-muted);
  font-size: 0.9em;
  font-weight: 600;
}

.thumb {
  display: block;
  width: 64px;
  height: 40px;
  border-radius: var(--radius);
  object-fit: cover;
}

.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-background);
  font-size: 1.3em;
}

.title {
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

.status.upcoming {
  color: #1e40af;
  background: #dbeafe;
}

.status.ongoing {
  color: #047857;
  background: #d1fae5;
}

.status.ended {
  color: var(--color-text-muted);
}

.buttons {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
}

.action a,
.action button {
  display: inline-block;
  padding: var(--space-1) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  color: inherit;
  font: inherit;
  text-decoration: none;
  cursor: pointer;
}

.action button.danger {
  color: var(--color-danger);
  border-color: var(--color-danger);
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

  .challenges {
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

  .thumb {
    width: 100%;
    height: 120px;
  }

  td.title {
    font-size: 1.05em;
  }

  td.action {
    justify-content: flex-end;
    padding-top: var(--space-2);
  }
}
</style>
