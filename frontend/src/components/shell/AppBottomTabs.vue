<script setup>
// 640 미만 회원 하단 탭 5칸 (F-29b AC5). 엄지로 바로 누를 4곳(bottomTabs.js) + 더보기(드로어).
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { Ellipsis } from 'lucide-vue-next'
import { navIcon } from '@/utils/navIcons'
import { activeNavPath } from '@/utils/navItems'
import { BOTTOM_TABS } from './bottomTabs'

defineProps({
  /** 더보기 드로어가 열려 있는지 (aria-expanded) */
  moreOpen: { type: Boolean, default: false },
})
const emit = defineEmits(['more'])
const route = useRoute()
const activeTo = computed(() => activeNavPath(route.path, BOTTOM_TABS))

const tab =
  'flex flex-col items-center justify-center gap-0.5 text-xs no-underline focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-primary'
</script>

<template>
  <nav
    aria-label="하단 탭"
    class="fixed inset-x-0 bottom-0 z-30 grid h-[calc(56px+env(safe-area-inset-bottom))] grid-cols-5 border-t border-border bg-surface pb-[env(safe-area-inset-bottom)] sm:hidden"
  >
    <RouterLink
      v-for="item in BOTTOM_TABS"
      :key="item.to"
      :to="item.to"
      :aria-current="item.to === activeTo ? 'page' : undefined"
      :class="[tab, item.to === activeTo ? 'font-semibold text-primary' : 'text-text-muted']"
    >
      <component :is="navIcon(item.icon)" class="size-5" aria-hidden="true" />
      {{ item.label }}
    </RouterLink>
    <button
      type="button"
      aria-haspopup="dialog"
      :aria-expanded="moreOpen ? 'true' : 'false'"
      :class="[tab, 'text-text-muted']"
      @click="emit('more')"
    >
      <Ellipsis class="size-5" aria-hidden="true" />
      더보기
    </button>
  </nav>
</template>
