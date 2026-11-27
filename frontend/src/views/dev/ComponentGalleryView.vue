<script setup>
// 공통 컴포넌트 미리보기 (F-29a 5.3절, AC13). 개발 서버에서만 /dev/components로 열린다 (router/index.js).
// ui-verifier가 이 화면을 라이트·다크 × 1280·375로 찍는다.
import { ref } from 'vue'
import { Moon, Sun, Monitor } from 'lucide-vue-next'
import AsyncState from '@/components/common/AsyncState.vue'
import BaseBadge from '@/components/common/BaseBadge.vue'
import BaseButton from '@/components/common/BaseButton.vue'
import BaseCard from '@/components/common/BaseCard.vue'
import BaseTabs from '@/components/common/BaseTabs.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import FormField from '@/components/common/FormField.vue'
import PageNav from '@/components/common/PageNav.vue'
import { useTheme } from '@/composables/useTheme'

const { theme, setTheme } = useTheme()
const themeOptions = [
  { value: 'light', label: '라이트', icon: Sun },
  { value: 'dark', label: '다크', icon: Moon },
  { value: 'system', label: '시스템', icon: Monitor },
]

const tones = ['neutral', 'primary', 'success', 'warning', 'info', 'danger', 'highlight']
const level = ref('easy')
const levels = [
  { value: 'easy', label: '쉬움' },
  { value: 'normal', label: '보통' },
  { value: 'hard', label: '어려움' },
]
const email = ref('')
const asyncMode = ref('loading')
const dialogOpen = ref(false)
const dialogLoading = ref(false)
const page = ref(1)

function confirmDelete() {
  dialogLoading.value = true
  setTimeout(() => {
    dialogLoading.value = false
    dialogOpen.value = false
  }, 800)
}

const inputClass =
  'h-10 w-full rounded-sm border border-border bg-surface px-3 text-text aria-[invalid=true]:border-danger focus-visible:outline-2 focus-visible:outline-primary'
</script>

<template>
  <main class="mx-auto flex max-w-4xl flex-col gap-8 px-4 py-8">
    <header class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h1 class="text-xl font-bold">공통 컴포넌트</h1>
        <p class="text-sm text-text-muted">F-29a 디자인 기반 · 개발 서버 전용</p>
      </div>
      <div role="group" aria-label="테마" class="flex gap-1">
        <BaseButton
          v-for="option in themeOptions"
          :key="option.value"
          size="sm"
          :variant="theme === option.value ? 'primary' : 'secondary'"
          :aria-pressed="theme === option.value"
          :data-theme-option="option.value"
          @click="setTheme(option.value)"
        >
          <component :is="option.icon" class="size-4" aria-hidden="true" />
          {{ option.label }}
        </BaseButton>
      </div>
    </header>

    <section class="flex flex-col gap-3">
      <h2 class="text-lg font-bold">토큰</h2>
      <p class="text-hero font-bold">바이브노트</p>
      <p>
        본문 Pretendard. 코드는
        <code class="rounded-sm bg-code-bg px-1 text-code-text">useTheme()</code> JetBrains Mono.
        <mark class="bg-highlight text-text">형광펜 강조</mark>와
        <span class="text-accent">코랄 포인트</span>.
      </p>
      <pre
        class="overflow-x-auto rounded-md bg-code-bg p-4 text-sm text-code-text"
      ><code>const level = ref('easy') // 수준별 설명</code></pre>
    </section>

    <section class="flex flex-col gap-3">
      <h2 class="text-lg font-bold">Button</h2>
      <div class="flex flex-wrap gap-2">
        <BaseButton>기본</BaseButton>
        <BaseButton variant="secondary">보조</BaseButton>
        <BaseButton variant="ghost">고스트</BaseButton>
        <BaseButton variant="danger">삭제</BaseButton>
        <BaseButton size="sm">작게</BaseButton>
        <BaseButton loading>저장 중</BaseButton>
        <BaseButton disabled>비활성</BaseButton>
        <BaseButton to="/concepts" variant="secondary">링크 버튼</BaseButton>
      </div>
    </section>

    <section class="flex flex-col gap-3">
      <h2 class="text-lg font-bold">FormField</h2>
      <div class="grid gap-4 sm:grid-cols-2">
        <FormField
          v-slot="{ id, describedBy, invalid, required }"
          label="이메일"
          hint="로그인에 씁니다."
          required
        >
          <input
            :id="id"
            v-model="email"
            type="email"
            :class="inputClass"
            :aria-describedby="describedBy"
            :aria-invalid="invalid"
            :required="required"
          />
        </FormField>
        <FormField
          v-slot="{ id, describedBy, invalid }"
          label="닉네임"
          error="이미 쓰는 닉네임입니다."
        >
          <input
            :id="id"
            :class="inputClass"
            :aria-describedby="describedBy"
            :aria-invalid="invalid"
          />
        </FormField>
      </div>
    </section>

    <section class="flex flex-col gap-3">
      <h2 class="text-lg font-bold">AsyncState</h2>
      <div class="flex flex-wrap gap-2">
        <BaseButton
          v-for="mode in ['loading', 'error', 'empty', 'content']"
          :key="mode"
          size="sm"
          :variant="asyncMode === mode ? 'primary' : 'secondary'"
          @click="asyncMode = mode"
        >
          {{ mode }}
        </BaseButton>
      </div>
      <AsyncState
        :loading="asyncMode === 'loading'"
        :error="asyncMode === 'error' ? '목록을 불러오지 못했습니다.' : ''"
        :empty="asyncMode === 'empty'"
        empty-message="아직 찜한 카드가 없습니다."
        @retry="asyncMode = 'content'"
      >
        <template #empty-action>
          <BaseButton to="/concepts" size="sm">카드 둘러보기</BaseButton>
        </template>
        <p>불러온 내용</p>
      </AsyncState>
    </section>

    <section class="flex flex-col gap-3">
      <h2 class="text-lg font-bold">Badge · Card</h2>
      <div class="flex flex-wrap gap-2">
        <BaseBadge v-for="tone in tones" :key="tone" :tone="tone">{{ tone }}</BaseBadge>
      </div>
      <div class="grid gap-4 sm:grid-cols-2">
        <BaseCard as="article">
          <template #header>
            <h3 class="font-semibold">클로저</h3>
          </template>
          <p class="text-sm text-text-muted">함수가 만들어질 때의 변수를 기억하는 것.</p>
          <template #footer>
            <BaseBadge tone="primary">JavaScript</BaseBadge>
          </template>
        </BaseCard>
        <BaseCard as="article" interactive>
          <template #header>
            <h3 class="font-semibold">
              <a href="#" class="text-text no-underline">interactive 카드</a>
            </h3>
          </template>
          <p class="text-sm text-text-muted">
            마우스를 올리거나 Tab으로 들어오면 테두리·링이 보인다.
          </p>
        </BaseCard>
      </div>
      <EmptyState message="EmptyState (기존 API 그대로)" />
      <PageNav :page="page" :size="10" :total="35" @change="page = $event" />
    </section>

    <section class="flex flex-col gap-3">
      <h2 class="text-lg font-bold">Tabs</h2>
      <BaseTabs v-model="level" :items="levels" label="설명 수준">
        <template #easy><p>쉬운 설명: 상자에 이름표를 붙이는 것.</p></template>
        <template #normal><p>보통 설명: 변수에 값을 담는다.</p></template>
        <template #hard><p>어려운 설명: 스코프 체인과 렉시컬 환경.</p></template>
      </BaseTabs>
    </section>

    <section class="flex flex-col gap-3">
      <h2 class="text-lg font-bold">ConfirmDialog</h2>
      <div>
        <BaseButton variant="danger" @click="dialogOpen = true">카드 삭제</BaseButton>
      </div>
      <ConfirmDialog
        v-model:open="dialogOpen"
        title="카드를 삭제할까요?"
        message="삭제하면 되돌릴 수 없습니다."
        confirm-label="삭제"
        danger
        :loading="dialogLoading"
        @confirm="confirmDelete"
      />
    </section>
  </main>
</template>
