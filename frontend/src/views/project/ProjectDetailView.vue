<script setup>
// SC-26 프로젝트 상세 (P-1a AC15). 버전별로 묶은 분석, 저장소 프로젝트의 새 커밋 배지(따로 불러온다),
// 새 버전 분석(저장소 "최신 커밋 분석"·ZIP "ZIP 다시 올리기" → /analyses/new?projectId=), 이름 바꾸기.
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ExternalLink, Pencil } from 'lucide-vue-next'
import { PROJECT_NAME_MAX, fetchProject, updateProject } from '@/api/projects'
import { errorCode, errorDetails, errorMessage } from '@/api/http'
import { useLatestRequest } from '@/composables/useLatestRequest'
import AsyncState from '@/components/common/AsyncState.vue'
import BaseBadge from '@/components/common/BaseBadge.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import LatestCommitBadge from '@/components/project/LatestCommitBadge.vue'
import ProjectVersionList from '@/components/project/ProjectVersionList.vue'
import { SOURCE_LABELS, safeSourceLink } from '@/components/project/projectLabels'

const route = useRoute()

const project = ref(null)
const loading = ref(true)
const notFound = ref(false)
const loadError = ref('')

const editing = ref(false)
const nameInput = ref('')
const saving = ref(false)
const renameError = ref('')
const nameError = ref('')

const projectId = computed(() => Number(route.params.id))
const isRepo = computed(() => project.value?.sourceType === 'URL')
const sourceLink = computed(() => safeSourceLink(project.value?.sourceUrl))
const trimmedName = computed(() => nameInput.value.trim())
const canRename = computed(
  () =>
    trimmedName.value.length > 0 && trimmedName.value.length <= PROJECT_NAME_MAX && !saving.value,
)

// 다른 프로젝트로 옮기면 이전 요청을 취소하고 늦게 온 응답은 버린다 (V-4)
const latest = useLatestRequest()

async function load(id) {
  const request = latest.begin()
  loading.value = true
  notFound.value = false
  loadError.value = ''
  try {
    const result = await fetchProject(id, { signal: request.signal })
    if (!request.isCurrent()) return
    project.value = result
  } catch (error) {
    if (!request.isCurrent()) return
    if (errorCode(error) === 'PROJECT_404') notFound.value = true
    else loadError.value = errorMessage(error)
  } finally {
    if (request.isCurrent()) loading.value = false
  }
}

watch(
  projectId,
  (id) => {
    // 상세가 아닌 주소로 나가는 중이면 params.id가 없어 NaN이 된다
    if (!Number.isInteger(id)) return
    project.value = null
    cancelRename()
    load(id)
  },
  { immediate: true },
)

function startRename() {
  nameInput.value = project.value.name
  renameError.value = ''
  nameError.value = ''
  editing.value = true
}

function cancelRename() {
  editing.value = false
  renameError.value = ''
  nameError.value = ''
}

async function onRename() {
  if (!canRename.value) return
  const id = project.value.id
  saving.value = true
  renameError.value = ''
  nameError.value = ''
  try {
    const result = await updateProject(id, { name: trimmedName.value })
    if (project.value?.id !== result.id) return
    project.value = { ...project.value, name: result.name }
    editing.value = false
  } catch (error) {
    // 저장 중에 다른 프로젝트로 옮겼으면 그 화면에 이전 프로젝트의 오류를 띄우지 않는다
    if (project.value?.id !== id) return
    const details = errorDetails(error)
    if (details.name) nameError.value = details.name
    else renameError.value = errorMessage(error)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="flex flex-col gap-6 py-8">
    <RouterLink :to="{ name: 'projects' }" class="text-sm" data-action="back"
      >← 내 프로젝트</RouterLink
    >

    <p
      v-if="notFound"
      class="rounded-md border border-border bg-surface px-4 py-6 text-center text-text-muted"
      data-message="not-found"
    >
      프로젝트를 찾을 수 없어요.
    </p>

    <AsyncState v-else :loading="loading" :error="loadError" @retry="load(projectId)">
      <template v-if="project">
        <header class="flex flex-col gap-3">
          <div v-if="!editing" class="flex flex-wrap items-center gap-3">
            <h1 class="min-w-0 break-all">{{ project.name }}</h1>
            <BaseBadge :tone="isRepo ? 'info' : 'neutral'">
              {{ SOURCE_LABELS[project.sourceType] ?? project.sourceType }}
            </BaseBadge>
            <BaseButton variant="ghost" size="sm" data-action="rename" @click="startRename">
              <Pencil class="size-4" aria-hidden="true" />
              이름 바꾸기
            </BaseButton>
          </div>

          <form
            v-else
            class="flex flex-col gap-2"
            data-form="rename"
            novalidate
            @submit.prevent="onRename"
          >
            <label for="project-name" class="text-sm font-semibold">프로젝트 이름</label>
            <div class="flex flex-wrap items-center gap-2">
              <input
                id="project-name"
                v-model="nameInput"
                name="name"
                type="text"
                :maxlength="PROJECT_NAME_MAX"
                autocomplete="off"
                class="h-10 min-w-0 flex-1 rounded-sm border border-border bg-surface px-3 text-text aria-[invalid=true]:border-danger focus-visible:outline-2 focus-visible:outline-primary"
                :aria-invalid="nameError ? 'true' : undefined"
                :aria-describedby="nameError ? 'project-name-error' : undefined"
              />
              <BaseButton type="submit" :disabled="!canRename" :loading="saving">저장</BaseButton>
              <BaseButton variant="secondary" data-action="cancel-rename" @click="cancelRename">
                취소
              </BaseButton>
            </div>
            <p
              v-if="nameError"
              id="project-name-error"
              class="text-sm text-danger"
              data-error="name"
            >
              {{ nameError }}
            </p>
            <p v-if="renameError" class="text-sm text-danger" role="alert">{{ renameError }}</p>
          </form>

          <a
            v-if="sourceLink"
            :href="sourceLink"
            target="_blank"
            rel="noopener noreferrer"
            class="inline-flex items-center gap-1 self-start text-sm break-all"
            data-action="source"
          >
            {{ sourceLink }}
            <ExternalLink class="size-3.5 shrink-0" aria-hidden="true" />
          </a>

          <div class="flex flex-wrap items-center gap-3">
            <LatestCommitBadge v-if="isRepo" :project-id="project.id" />
            <BaseButton
              :to="{ name: 'analysis-new', query: { projectId: String(project.id) } }"
              data-action="new-version"
            >
              {{ isRepo ? '최신 커밋 분석' : 'ZIP 다시 올리기' }}
            </BaseButton>
          </div>
        </header>

        <div class="flex flex-col gap-3">
          <h2 class="text-lg">버전</h2>
          <ProjectVersionList :versions="project.versions" :analyses="project.analyses" />
        </div>
      </template>
    </AsyncState>
  </section>
</template>
