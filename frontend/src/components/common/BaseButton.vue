<script setup>
// 공통 버튼 (F-29a AC7). to를 주면 RouterLink로 그린다. loading·disabled면 click을 내보내지 않는다.
import { computed } from 'vue'
import { RouterLink } from 'vue-router'

const props = defineProps({
  variant: {
    type: String,
    default: 'primary',
    validator: (v) => ['primary', 'secondary', 'ghost', 'danger'].includes(v),
  },
  size: { type: String, default: 'md', validator: (v) => ['sm', 'md'].includes(v) },
  type: { type: String, default: 'button' },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  to: { type: [String, Object], default: null },
})

const emit = defineEmits(['click'])

const VARIANTS = {
  primary: 'bg-primary text-on-solid hover:bg-primary/90',
  secondary: 'border border-border bg-surface text-text hover:bg-bg',
  ghost: 'bg-transparent text-text hover:bg-text/5',
  danger: 'bg-danger text-on-solid hover:bg-danger/90',
}

const SIZES = {
  sm: 'h-8 px-3 text-sm',
  md: 'h-10 px-4 text-base',
}

const classes = computed(() => [
  'inline-flex items-center justify-center gap-2 rounded-sm font-semibold no-underline',
  'transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary',
  'disabled:cursor-not-allowed disabled:opacity-50',
  VARIANTS[props.variant],
  SIZES[props.size],
])

function onClick(event) {
  if (props.disabled || props.loading) return
  emit('click', event)
}
</script>

<template>
  <!-- disabled·loading이면 to가 있어도 버튼으로 그린다 (RouterLink는 클릭을 막아도 이동한다) -->
  <RouterLink
    v-if="to && !disabled && !loading"
    :to="to"
    :class="classes"
    :data-variant="variant"
    :data-size="size"
    @click="onClick"
  >
    <slot />
  </RouterLink>
  <button
    v-else
    :type="type"
    :class="classes"
    :disabled="disabled || loading"
    :aria-busy="loading ? 'true' : undefined"
    :data-variant="variant"
    :data-size="size"
    @click="onClick"
  >
    <span
      v-if="loading"
      data-spinner
      aria-hidden="true"
      class="size-4 animate-spin rounded-full border-2 border-current border-t-transparent"
    />
    <slot />
  </button>
</template>
