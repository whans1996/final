<script setup>
// 비회원 셸 (F-29b AC6): 옆 메뉴 없이 윗쪽 메뉴(로고·개념 카드·커뮤니티·챌린지·검색·다크 토글·로그인·회원가입).
// 640 미만에서는 메뉴와 로그인·회원가입이 햄버거 드로어로 접힌다.
// 메뉴는 group이 있는 meta.nav(navGroups), 로그인·회원가입은 group 없는 show: 'guest' 항목이다.
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { Menu } from 'lucide-vue-next'
import BaseButton from '@/components/common/BaseButton.vue'
import AppHeader from '@/components/shell/AppHeader.vue'
import NavDrawer from '@/components/shell/NavDrawer.vue'
import { useAuthStore } from '@/stores/auth'
import { activeNavPath, navGroups, navItems } from '@/utils/navItems'
import SkipLink from './SkipLink.vue'

defineProps({
  /** 본문 최대 폭 1440px (meta.wide), 아니면 1080px */
  wide: { type: Boolean, default: false },
})

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const groups = computed(() => navGroups(router.options.routes, auth))
const links = computed(() => groups.value.flatMap((g) => g.items))
const accountLinks = computed(() => {
  const grouped = new Set(links.value.map((i) => i.to))
  return navItems(router.options.routes, auth).filter((i) => !grouped.has(i.to))
})
const activeTo = computed(() => activeNavPath(route.path, links.value))
const drawerOpen = ref(false)
</script>

<template>
  <SkipLink />
  <AppHeader inline-search-from="lg">
    <template #menu-button>
      <button
        type="button"
        aria-label="메뉴 열기"
        aria-haspopup="dialog"
        :aria-expanded="drawerOpen ? 'true' : 'false'"
        class="flex size-9 shrink-0 items-center justify-center rounded-sm text-text-muted hover:bg-primary-soft focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-primary sm:hidden"
        @click="drawerOpen = true"
      >
        <Menu class="size-5" aria-hidden="true" />
      </button>
    </template>
    <template #nav>
      <nav aria-label="주 메뉴" class="hidden shrink-0 items-center gap-1 sm:flex">
        <RouterLink
          v-for="item in links"
          :key="item.to"
          :to="item.to"
          :aria-current="item.to === activeTo ? 'page' : undefined"
          :class="[
            'rounded-sm px-2 py-1 text-sm whitespace-nowrap no-underline hover:bg-primary-soft focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-primary',
            item.to === activeTo ? 'font-semibold text-primary' : 'text-text',
          ]"
        >
          {{ item.label }}
        </RouterLink>
      </nav>
    </template>
    <template #account>
      <div class="hidden items-center gap-1 sm:flex">
        <BaseButton
          v-for="(item, index) in accountLinks"
          :key="item.to"
          :to="item.to"
          size="sm"
          :variant="index === accountLinks.length - 1 ? 'primary' : 'ghost'"
        >
          {{ item.label }}
        </BaseButton>
      </div>
    </template>
  </AppHeader>
  <main id="main" tabindex="-1" class="px-4 pb-8 outline-none sm:px-6">
    <div :class="['mx-auto w-full', wide ? 'max-w-[1440px]' : 'max-w-[1080px]']">
      <slot />
    </div>
  </main>
  <NavDrawer v-model:open="drawerOpen" :groups="groups">
    <template #default="{ rowClass }">
      <RouterLink v-for="item in accountLinks" :key="item.to" :to="item.to" :class="rowClass">
        {{ item.label }}
      </RouterLink>
    </template>
  </NavDrawer>
</template>
