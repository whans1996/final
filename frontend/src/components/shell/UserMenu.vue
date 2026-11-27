<script setup>
// 닉네임 메뉴 (F-29b AC11). Reka UI DropdownMenu가 키보드 이동·Esc·바깥 클릭·포커스 되돌리기를 맡는다.
// auth.user가 아직 없으면(세션 복원 중) 닉네임 자리만 둔다 (5.2 상태별 UI).
import { RouterLink } from 'vue-router'
import { ChevronDown, LogOut, Settings, Shield, User } from 'lucide-vue-next'
import {
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuPortal,
  DropdownMenuRoot,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from 'reka-ui'
import { useAuthStore } from '@/stores/auth'
import { useLogout } from './useLogout'

const auth = useAuthStore()
const onLogout = useLogout()

const item =
  'flex cursor-pointer items-center gap-2 rounded-sm px-3 py-2 text-sm text-text no-underline outline-none data-highlighted:bg-primary-soft'
</script>

<template>
  <span
    v-if="!auth.user"
    data-testid="user-menu-placeholder"
    class="h-9 w-20 shrink-0 animate-pulse rounded-sm bg-surface-raised"
    aria-hidden="true"
  />
  <DropdownMenuRoot v-else :modal="false">
    <DropdownMenuTrigger
      class="flex h-9 max-w-40 shrink-0 items-center gap-1 rounded-sm px-2 text-sm font-semibold text-text hover:bg-primary-soft focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-primary"
      :aria-label="`${auth.user.nickname} 메뉴`"
    >
      <span class="truncate">{{ auth.user.nickname }}</span>
      <ChevronDown class="size-4 shrink-0" aria-hidden="true" />
    </DropdownMenuTrigger>
    <DropdownMenuPortal>
      <DropdownMenuContent
        align="end"
        :side-offset="4"
        class="z-50 min-w-44 rounded-md border border-border bg-surface p-1 shadow-2 dark:bg-surface-raised"
      >
        <DropdownMenuItem as-child>
          <RouterLink :to="{ name: 'user-profile', params: { id: auth.user.id } }" :class="item">
            <User class="size-4" aria-hidden="true" />프로필
          </RouterLink>
        </DropdownMenuItem>
        <DropdownMenuItem as-child>
          <RouterLink :to="{ name: 'my-settings' }" :class="item">
            <Settings class="size-4" aria-hidden="true" />설정
          </RouterLink>
        </DropdownMenuItem>
        <DropdownMenuItem v-if="auth.isAdmin" as-child>
          <RouterLink to="/admin" :class="item">
            <Shield class="size-4" aria-hidden="true" />관리자
          </RouterLink>
        </DropdownMenuItem>
        <DropdownMenuSeparator class="my-1 h-px bg-border" />
        <DropdownMenuItem :class="item" @select="onLogout">
          <LogOut class="size-4" aria-hidden="true" />로그아웃
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenuPortal>
  </DropdownMenuRoot>
</template>
