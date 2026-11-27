<script setup>
// 회원 옆 메뉴 (F-29b AC2·AC3·AC4). 폭은 CSS가 정한다: <640 숨김(하단 탭), 640~1024 아이콘만 64px,
// ≥1024 240px(접으면 64px). 접힘 상태만 JS가 들고 localStorage에 저장한다.
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { PanelLeftClose, PanelLeftOpen } from 'lucide-vue-next'
import { navIcon } from '@/utils/navIcons'
import { activeNavPath } from '@/utils/navItems'
import { useSidebarState } from './sidebarState'

const props = defineProps({
  /** navGroups() 결과 `[{ key, label, items: [{ label, to, icon }] }]` */
  groups: { type: Array, required: true },
})

const route = useRoute()
const { collapsed, setCollapsed } = useSidebarState()

const activeTo = computed(() =>
  activeNavPath(
    route.path,
    props.groups.flatMap((g) => g.items),
  ),
)

// 이름: 접히면 항상 sr-only, 아니면 640~1024에서만 sr-only
const labelClass = computed(() => (collapsed.value ? 'sr-only' : 'sr-only lg:not-sr-only'))
// 이름 툴팁: 아이콘만 보일 때(640~1024, 또는 접혔을 때) hover·focus에서 보인다
const tooltipClass = computed(() =>
  collapsed.value
    ? 'group-hover:block group-focus-visible:block'
    : 'group-hover:block group-focus-visible:block lg:group-hover:hidden lg:group-focus-visible:hidden',
)
</script>

<template>
  <aside
    id="app-sidebar"
    :data-collapsed="collapsed ? 'true' : 'false'"
    :class="[
      'sticky top-14 z-20 hidden h-[calc(100dvh-3.5rem)] w-16 shrink-0 flex-col border-r border-border bg-surface sm:flex',
      collapsed ? '' : 'lg:w-60',
    ]"
  >
    <nav aria-label="주 메뉴" class="flex flex-1 flex-col gap-3 px-2 py-4">
      <section v-for="group in groups" :key="group.key" class="flex flex-col gap-1">
        <h2 :class="[labelClass, 'px-3 pb-1 text-xs font-semibold tracking-wide text-text-muted']">
          {{ group.label }}
        </h2>
        <ul class="flex list-none flex-col gap-1 ps-0">
          <li v-for="item in group.items" :key="item.to">
            <RouterLink
              :to="item.to"
              :aria-label="item.label"
              :title="collapsed ? item.label : undefined"
              :aria-current="item.to === activeTo ? 'page' : undefined"
              :class="[
                'group relative flex h-10 items-center gap-3 rounded-sm px-3 text-sm text-text no-underline hover:bg-primary-soft focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary',
                item.to === activeTo ? 'bg-primary-soft font-semibold text-primary' : '',
                collapsed ? 'justify-center' : 'justify-center lg:justify-start',
              ]"
            >
              <component :is="navIcon(item.icon)" class="size-5 shrink-0" aria-hidden="true" />
              <span :class="labelClass">{{ item.label }}</span>
              <span
                data-tooltip
                aria-hidden="true"
                :class="[
                  'pointer-events-none absolute top-1/2 left-full z-50 ml-2 hidden -translate-y-1/2 rounded-sm bg-text px-2 py-1 text-xs whitespace-nowrap text-bg shadow-1',
                  tooltipClass,
                ]"
              >
                {{ item.label }}
              </span>
            </RouterLink>
          </li>
        </ul>
      </section>
    </nav>
    <div class="hidden border-t border-border p-2 lg:flex">
      <button
        type="button"
        aria-controls="app-sidebar"
        :aria-expanded="collapsed ? 'false' : 'true'"
        :aria-label="collapsed ? '메뉴 펼치기' : '메뉴 접기'"
        :title="collapsed ? '메뉴 펼치기' : '메뉴 접기'"
        :class="[
          'flex h-10 w-full items-center rounded-sm px-3 text-text-muted hover:bg-primary-soft focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary',
          collapsed ? 'justify-center' : 'justify-start',
        ]"
        @click="setCollapsed(!collapsed)"
      >
        <PanelLeftOpen v-if="collapsed" class="size-5" aria-hidden="true" />
        <PanelLeftClose v-else class="size-5" aria-hidden="true" />
      </button>
    </div>
  </aside>
</template>
