<script setup>
// 확인 대화상자 (F-29a AC12). Reka UI Dialog가 포커스 가두기·되돌리기·Esc·바깥 클릭을 맡는다.
// 확인은 confirm만 내보내고 닫지 않는다 (요청이 끝나면 부모가 open을 false로. 그동안 loading).
// 취소·Esc·바깥 클릭은 cancel을 내보내고 닫는다. loading 동안은 닫히지 않는다.
import {
  DialogContent,
  DialogDescription,
  DialogOverlay,
  DialogPortal,
  DialogRoot,
  DialogTitle,
} from 'reka-ui'
import BaseButton from './BaseButton.vue'

const props = defineProps({
  title: { type: String, required: true },
  message: { type: String, default: '' },
  confirmLabel: { type: String, default: '확인' },
  cancelLabel: { type: String, default: '취소' },
  danger: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
})

const open = defineModel('open', { type: Boolean, default: false })
const emit = defineEmits(['confirm', 'cancel'])

function onOpenChange(value) {
  if (value || props.loading) return
  emit('cancel')
  open.value = false
}

function blockWhileLoading(event) {
  if (props.loading) event.preventDefault()
}
</script>

<template>
  <DialogRoot :open="open" @update:open="onOpenChange">
    <DialogPortal>
      <DialogOverlay class="fixed inset-0 z-40 bg-black/40" />
      <DialogContent
        class="fixed top-1/2 left-1/2 z-50 flex w-[calc(100%-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 flex-col gap-4 rounded-md border border-border bg-surface p-6 text-text shadow-2 dark:bg-surface-raised"
        @escape-key-down="blockWhileLoading"
        @interact-outside="blockWhileLoading"
      >
        <DialogTitle class="text-lg font-bold">{{ title }}</DialogTitle>
        <DialogDescription v-if="message" class="text-text-muted">{{ message }}</DialogDescription>
        <slot />
        <div class="flex justify-end gap-2">
          <BaseButton variant="secondary" :disabled="loading" @click="onOpenChange(false)">
            {{ cancelLabel }}
          </BaseButton>
          <BaseButton
            :variant="danger ? 'danger' : 'primary'"
            :loading="loading"
            @click="emit('confirm')"
          >
            {{ confirmLabel }}
          </BaseButton>
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>
