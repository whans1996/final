<script setup>
// 헤더 다크 토글 (F-29b AC10). 순서는 themeCycle.js, 상태는 useTheme(F-29a).
import { computed } from 'vue'
import { Monitor, Moon, Sun } from 'lucide-vue-next'
import { FOLLOW_SYSTEM, useTheme } from '@/composables/useTheme'
import { nextTheme, shownTheme, THEME_LABELS } from './themeCycle'

const { theme, setTheme } = useTheme()
const shown = computed(() => shownTheme(theme.value, FOLLOW_SYSTEM))
const icon = computed(() => ({ light: Sun, dark: Moon, system: Monitor })[shown.value])
const label = computed(() => `테마: ${THEME_LABELS[shown.value]}`)
</script>

<template>
  <button
    type="button"
    :aria-label="label"
    :title="label"
    class="flex size-9 shrink-0 items-center justify-center rounded-sm text-text-muted hover:bg-primary-soft focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-primary"
    @click="setTheme(nextTheme(theme, FOLLOW_SYSTEM))"
  >
    <component :is="icon" class="size-5" aria-hidden="true" />
  </button>
</template>
