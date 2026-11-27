<script setup>
// 640 미만 메뉴 드로어 (F-29b AC5·AC6·AC14). Reka UI Dialog가 포커스 가두기·되돌리기·Esc·바깥 클릭을 맡는다.
// 메뉴(링크)를 고르면 닫는다. 그룹 메뉴 아래에 셸마다 다른 항목(계정 메뉴·로그인)을 기본 슬롯으로 받는다.
import { RouterLink } from 'vue-router'
import { X } from 'lucide-vue-next'
import {
  DialogClose,
  DialogContent,
  DialogOverlay,
  DialogPortal,
  DialogRoot,
  DialogTitle,
} from 'reka-ui'
import { navIcon } from '@/utils/navIcons'

defineProps({
  /** navGroups() 모양 `[{ key, label, items: [{ label, to, icon }] }]` */
  groups: { type: Array, default: () => [] },
  title: { type: String, default: '메뉴' },
})
const open = defineModel('open', { type: Boolean, default: false })

// 안의 링크·버튼(data-drawer-close)을 누르면 닫는다
function closeOnPick(event) {
  if (event.target.closest('a, [data-drawer-close]')) open.value = false
}

const row =
  'flex h-11 w-full items-center gap-3 rounded-sm px-3 text-sm text-text no-underline hover:bg-primary-soft focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-primary'
</script>

<template>
  <DialogRoot v-model:open="open">
    <DialogPortal>
      <DialogOverlay class="fixed inset-0 z-40 bg-black/40" />
      <DialogContent
        :aria-describedby="undefined"
        class="fixed inset-y-0 right-0 z-50 flex w-72 max-w-[85vw] flex-col gap-2 overflow-y-auto border-l border-border bg-surface p-3 pb-[calc(12px+env(safe-area-inset-bottom))] text-text shadow-2 dark:bg-surface-raised"
        @click="closeOnPick"
      >
        <div class="flex items-center justify-between px-1">
          <DialogTitle class="text-base font-bold">{{ title }}</DialogTitle>
          <DialogClose
            aria-label="메뉴 닫기"
            class="flex size-9 items-center justify-center rounded-sm text-text-muted hover:bg-primary-soft focus-visible:outline-2 focus-visible:outline-primary"
          >
            <X class="size-5" aria-hidden="true" />
          </DialogClose>
        </div>
        <nav aria-label="전체 메뉴" class="flex flex-col gap-3">
          <section v-for="group in groups" :key="group.key" class="flex flex-col gap-1">
            <h3 class="px-3 text-xs font-semibold text-text-muted">{{ group.label }}</h3>
            <ul class="flex list-none flex-col ps-0">
              <li v-for="item in group.items" :key="item.to">
                <RouterLink :to="item.to" :class="row">
                  <component :is="navIcon(item.icon)" class="size-5" aria-hidden="true" />
                  {{ item.label }}
                </RouterLink>
              </li>
            </ul>
          </section>
        </nav>
        <div v-if="$slots.default" class="mt-2 flex flex-col gap-1 border-t border-border pt-3">
          <slot :row-class="row" />
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>
