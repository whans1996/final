<script setup>
// 상태·분류 배지 (F-29a AC10). tone별 글자/바탕 쌍은 토큰이 라이트·다크에서 바꾼다.
import { computed } from 'vue'

const props = defineProps({
  tone: {
    type: String,
    default: 'neutral',
    validator: (v) =>
      ['neutral', 'primary', 'success', 'warning', 'info', 'danger', 'highlight'].includes(v),
  },
})

const TONES = {
  neutral: 'text-text-muted bg-bg border-border',
  primary: 'text-primary bg-primary-soft border-transparent',
  success: 'text-success bg-success-soft border-transparent',
  warning: 'text-warning bg-warning-soft border-transparent',
  info: 'text-info bg-info-soft border-transparent',
  danger: 'text-danger bg-danger-soft border-transparent',
  highlight: 'text-text bg-highlight border-transparent',
}

const toneClass = computed(() => TONES[props.tone])
</script>

<template>
  <span
    :data-tone="tone"
    class="inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs font-semibold whitespace-nowrap"
    :class="toneClass"
  >
    <slot />
  </span>
</template>
