<script setup>
// 얇은 헤더 (F-29b AC2·AC6·AC9·AC12). 회원·비회원 셸이 같이 쓰고, 셸마다 다른 부분은 슬롯으로 받는다.
//   #nav: 로고 옆 메뉴(비회원 윗쪽 메뉴), #notifications: 알림 자리(T1이 채운다, 지금은 비어 있음),
//   #account: 닉네임 메뉴·로그인 버튼, #menu-button: 640 미만 햄버거(비회원)
// 좁은 폭에서는 검색 줄이 숨고 🔍 버튼을 누르면 헤더 아래로 펼쳐진다. 기본은 640 미만,
// 메뉴 링크·로그인 버튼까지 한 줄에 넣는 비회원 헤더는 1024 미만(inlineSearchFrom="lg").
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { Search, X } from 'lucide-vue-next'
import HeaderSearch from './HeaderSearch.vue'
import ThemeToggle from './ThemeToggle.vue'

const props = defineProps({
  /** 검색 줄을 헤더 안에 펼치기 시작하는 폭: 'sm'(640) | 'lg'(1024) */
  inlineSearchFrom: { type: String, default: 'sm' },
})

const searchOpen = ref(false)
// Tailwind가 찾을 수 있게 클래스는 통째로 적는다
const lg = computed(() => props.inlineSearchFrom === 'lg')
const inlineClass = computed(() => (lg.value ? 'hidden lg:flex' : 'hidden sm:flex'))
const compactOnly = computed(() => (lg.value ? 'lg:hidden' : 'sm:hidden'))

const iconButton =
  'flex size-9 shrink-0 items-center justify-center rounded-sm text-text-muted hover:bg-primary-soft focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-primary'
</script>

<template>
  <header class="sticky top-0 z-30 border-b border-border bg-surface">
    <div class="flex h-14 items-center gap-2 px-3 sm:gap-4 sm:px-4">
      <slot name="menu-button" />
      <RouterLink
        to="/"
        class="shrink-0 rounded-sm text-lg font-bold whitespace-nowrap text-primary no-underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
      >
        바이브노트
      </RouterLink>
      <slot name="nav" />
      <div :class="[inlineClass, 'max-w-xl min-w-0 flex-1']">
        <HeaderSearch />
      </div>
      <div class="ml-auto flex shrink-0 items-center gap-1">
        <button
          type="button"
          :class="[iconButton, compactOnly]"
          aria-controls="mobile-search"
          :aria-expanded="searchOpen ? 'true' : 'false'"
          :aria-label="searchOpen ? '검색 닫기' : '검색 열기'"
          @click="searchOpen = !searchOpen"
        >
          <X v-if="searchOpen" class="size-5" aria-hidden="true" />
          <Search v-else class="size-5" aria-hidden="true" />
        </button>
        <slot name="notifications" />
        <ThemeToggle />
        <slot name="account" />
      </div>
    </div>
    <div
      v-if="searchOpen"
      id="mobile-search"
      :class="['border-t border-border px-3 py-2', compactOnly]"
    >
      <HeaderSearch @searched="searchOpen = false" />
    </div>
  </header>
</template>
