<script setup>
// SC-23 "분석 공유" (F-20b AC11). 내 분석 중 DONE이고 이 팀에 아직 공유하지 않은 것을 골라 공유한다.
// 퀴즈 유무는 목록에 없으므로 서버가 TEAM_009로 알려 준다 (고르기 목록 아래 message, 선택 유지).
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchAnalyses } from '@/api/analyses'
import { shareTeamAnalysis } from '@/api/teams'
import { errorMessage } from '@/api/http'
import { analysisLabel } from '@/components/community/boardTypes'

const props = defineProps({
  teamId: { type: Number, required: true },
  sharedIds: { type: Array, default: () => [] },
  /** 공유가 10개면 true */
  full: { type: Boolean, default: false },
})

const emit = defineEmits(['shared'])

const ANALYSIS_PAGE = { page: 1, size: 100 }

const open = ref(false)
const analyses = ref(null)
const loadError = ref('')
const selected = ref('')
const sharing = ref(false)
const error = ref('')

const candidates = computed(() =>
  (analyses.value ?? []).filter((a) => a.status === 'DONE' && !props.sharedIds.includes(a.id)),
)

async function toggle() {
  if (open.value) {
    open.value = false
    return
  }
  open.value = true
  selected.value = ''
  error.value = ''
  await loadAnalyses()
}

async function loadAnalyses() {
  analyses.value = null
  loadError.value = ''
  try {
    const result = await fetchAnalyses(ANALYSIS_PAGE)
    analyses.value = result.items
  } catch (e) {
    analyses.value = []
    loadError.value = errorMessage(e)
  }
}

async function share() {
  if (!selected.value || sharing.value) return
  sharing.value = true
  error.value = ''
  try {
    await shareTeamAnalysis(props.teamId, Number(selected.value))
    open.value = false
    selected.value = ''
    emit('shared')
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    sharing.value = false
  }
}
</script>

<template>
  <div class="picker">
    <button
      type="button"
      class="secondary"
      data-action="open-share"
      :disabled="full || sharing"
      :aria-expanded="open"
      @click="toggle"
    >
      분석 공유
    </button>

    <div v-if="open" class="panel">
      <p v-if="analyses === null" class="hint">불러오는 중…</p>
      <div v-else-if="loadError" class="row">
        <p class="form-error" role="alert">{{ loadError }}</p>
        <button type="button" class="secondary" data-action="retry-picker" @click="loadAnalyses">
          다시 시도
        </button>
      </div>
      <p v-else-if="candidates.length === 0" class="hint" data-role="no-candidates">
        공유할 수 있는 완료된 분석이 없습니다.
        <RouterLink to="/analyses/new">분석 요청하기</RouterLink>
      </p>
      <div v-else class="row">
        <select v-model="selected" data-field="analysis" aria-label="공유할 분석">
          <option value="" disabled>분석을 고르세요</option>
          <option v-for="a in candidates" :key="a.id" :value="String(a.id)">
            {{ analysisLabel(a) }}
          </option>
        </select>
        <button
          type="button"
          class="primary"
          data-action="share"
          :disabled="!selected || sharing"
          @click="share"
        >
          {{ sharing ? '공유 중…' : '공유' }}
        </button>
      </div>
      <p v-if="error" class="form-error" role="alert" data-error="share">{{ error }}</p>
    </div>
  </div>
</template>

<style scoped>
.picker {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-2);
  min-width: 0;
}

.panel {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  width: 100%;
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
}

.row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
}

select {
  flex: 1 1 12rem;
  min-width: 0;
  max-width: 100%;
  padding: var(--space-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  font: inherit;
}

.hint {
  margin: 0;
  color: var(--color-text-muted);
}

.secondary,
.primary {
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius);
  font: inherit;
  cursor: pointer;
}

.secondary {
  border: 1px solid var(--color-border);
  background: transparent;
  color: inherit;
}

.primary {
  border: 1px solid var(--color-primary);
  background: var(--color-primary);
  color: #fff;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.form-error {
  margin: 0;
  color: var(--color-danger);
}
</style>
