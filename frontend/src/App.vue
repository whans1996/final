<script setup>
// 셸 선택 (F-29b AC7): meta.layout === 'bare'면 셸 없이, 로그인했으면 AppShell, 아니면 GuestShell.
// 회원이 개념 카드·커뮤니티 같은 공개 화면을 봐도 AppShell이다. 본문 폭은 meta.wide면 1440px, 아니면 1080px.
// 메뉴는 각 route의 meta.nav로 정한다 (F-00b·F-29b). 여기에 링크를 직접 추가하지 않는다.
//
// 회원/비회원 셸은 로그인 상태가 바뀐 순간이 아니라 **내비게이션이 끝날 때** 바꾼다.
// 바로 따라가면 로그아웃·탈퇴(clear 뒤 home 이동) 사이에 지금 화면이 비회원 셸 안에서 다시 마운트되어
// 토큰 없이 API를 부르고, 401 처리로 home 대신 로그인 화면에 간다. 로그인·로그아웃·탈퇴·401은 모두 이동을 동반한다.
import { computed, onUnmounted, ref } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import AppShell from '@/layouts/AppShell.vue'
import GuestShell from '@/layouts/GuestShell.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const memberShell = ref(auth.isLoggedIn)
onUnmounted(
  router.afterEach(() => {
    memberShell.value = auth.isLoggedIn
  }),
)

const shell = computed(() => {
  if (route.meta.layout === 'bare') return null
  return memberShell.value ? AppShell : GuestShell
})
</script>

<template>
  <component :is="shell" v-if="shell" :wide="Boolean(route.meta.wide)">
    <RouterView />
  </component>
  <RouterView v-else />
</template>
