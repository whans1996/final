<script setup>
// 회원 셸 (F-29b AC2~AC5·AC13·AC14): 얇은 헤더 + 옆 메뉴(640 이상) + 본문 + 하단 탭·드로어(640 미만).
// 메뉴는 route meta.nav의 group으로 만든다 (navGroups). 셸은 데이터를 부르지 않는다.
import { computed, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { LogOut, Settings, Shield, User } from 'lucide-vue-next'
import AppBottomTabs from '@/components/shell/AppBottomTabs.vue'
import AppHeader from '@/components/shell/AppHeader.vue'
import AppSidebar from '@/components/shell/AppSidebar.vue'
import NavDrawer from '@/components/shell/NavDrawer.vue'
import UserMenu from '@/components/shell/UserMenu.vue'
import { withoutTabs } from '@/components/shell/bottomTabs'
import { useLogout } from '@/components/shell/useLogout'
import { useAuthStore } from '@/stores/auth'
import { navGroups } from '@/utils/navItems'
import SkipLink from './SkipLink.vue'

defineProps({
  /** 본문 최대 폭 1440px (meta.wide), 아니면 1080px */
  wide: { type: Boolean, default: false },
})

const auth = useAuthStore()
const router = useRouter()
const logout = useLogout()
const groups = computed(() => navGroups(router.options.routes, auth))
const drawerGroups = computed(() => withoutTabs(groups.value))
const drawerOpen = ref(false)
</script>

<template>
  <SkipLink />
  <AppHeader>
    <template #account>
      <div class="hidden sm:block"><UserMenu /></div>
    </template>
  </AppHeader>
  <div class="flex">
    <AppSidebar :groups="groups" />
    <main
      id="main"
      tabindex="-1"
      class="min-w-0 flex-1 px-4 pb-[calc(72px+env(safe-area-inset-bottom))] outline-none sm:px-6 sm:pb-8"
    >
      <div :class="['mx-auto w-full', wide ? 'max-w-[1440px]' : 'max-w-[1080px]']">
        <slot />
      </div>
    </main>
  </div>
  <AppBottomTabs :more-open="drawerOpen" @more="drawerOpen = true" />
  <NavDrawer v-model:open="drawerOpen" :groups="drawerGroups">
    <template #default="{ rowClass }">
      <template v-if="auth.user">
        <RouterLink :to="{ name: 'user-profile', params: { id: auth.user.id } }" :class="rowClass">
          <User class="size-5" aria-hidden="true" />프로필
        </RouterLink>
        <RouterLink :to="{ name: 'my-settings' }" :class="rowClass">
          <Settings class="size-5" aria-hidden="true" />설정
        </RouterLink>
        <RouterLink v-if="auth.isAdmin" to="/admin" :class="rowClass">
          <Shield class="size-5" aria-hidden="true" />관리자
        </RouterLink>
      </template>
      <button type="button" data-drawer-close :class="rowClass" @click="logout">
        <LogOut class="size-5" aria-hidden="true" />로그아웃
      </button>
    </template>
  </NavDrawer>
</template>
