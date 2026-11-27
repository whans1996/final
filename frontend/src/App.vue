<script setup>
// 셸 선택 (F-29b AC7): meta.layout === 'bare'면 셸 없이, 로그인했으면 AppShell, 아니면 GuestShell.
// 회원이 개념 카드·커뮤니티 같은 공개 화면을 봐도 AppShell이다. 본문 폭은 meta.wide면 1440px, 아니면 1080px.
// 메뉴는 각 route의 meta.nav로 정한다 (F-00b·F-29b). 여기에 링크를 직접 추가하지 않는다.
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AppShell from '@/layouts/AppShell.vue'
import GuestShell from '@/layouts/GuestShell.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()

const shell = computed(() => {
  if (route.meta.layout === 'bare') return null
  return auth.isLoggedIn ? AppShell : GuestShell
})
</script>

<template>
  <component :is="shell" v-if="shell" :wide="Boolean(route.meta.wide)">
    <RouterView />
  </component>
  <RouterView v-else />
</template>
