<script setup>
// 카드 틀 (F-29a AC10). interactive는 안에 링크·버튼이 있는 카드에 쓴다 (hover 테두리, 키보드 포커스 링).
defineProps({
  as: { type: String, default: 'div' },
  interactive: { type: Boolean, default: false },
})
</script>

<template>
  <component
    :is="as"
    class="flex flex-col gap-3 rounded-md border border-border bg-surface p-4 shadow-1 dark:bg-surface-raised"
    :class="
      interactive && [
        'transition-colors hover:border-primary',
        'focus-within:ring-2 focus-within:ring-primary focus-within:ring-offset-2 focus-within:ring-offset-bg',
      ]
    "
  >
    <header v-if="$slots.header"><slot name="header" /></header>
    <slot />
    <footer v-if="$slots.footer"><slot name="footer" /></footer>
  </component>
</template>
