<script setup>
// 헤더 종 버튼·배지·알림 드롭다운 (NT-1 AC13~AC16). 회원 셸(AppShell)의 #notifications 슬롯에만 들어간다.
// 안 읽은 수는 진입 때 1회, 이후 60초마다 다시 센다 (문서가 숨겨져 있으면 건너뛰고 다시 보이면 바로 1회).
// 목록은 열 때마다 첫 20개를 새로 부르고 "더 보기"로 이어 붙인다. Reka Popover가 Esc·바깥 클릭·포커스 되돌리기를 맡는다.
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Bell } from 'lucide-vue-next'
import { PopoverContent, PopoverPortal, PopoverRoot, PopoverTrigger } from 'reka-ui'
import AsyncState from '@/components/common/AsyncState.vue'
import { errorMessage } from '@/api/http'
import { NOTIFICATION_PAGE_SIZE, fetchNotifications } from '@/api/notifications'
import { useMoreList } from '@/composables/useMoreList'
import { usePolling } from '@/composables/usePolling'
import { useAuthStore } from '@/stores/auth'
import { useNotificationsStore } from '@/stores/notifications'
import NotificationItem from './NotificationItem.vue'

const POLL_INTERVAL = 60_000

const auth = useAuthStore()
const store = useNotificationsStore()
const router = useRouter()

// ── 배지 (AC13·AC14) ──

const badge = computed(() => {
  const n = store.unreadCount
  if (n <= 0) return ''
  return n >= 10 ? '9+' : String(n)
})
const label = computed(() =>
  store.unreadCount > 0 ? `알림, 안 읽음 ${store.unreadCount}개` : '알림',
)

const polling = usePolling(
  async () => {
    if (document.visibilityState === 'hidden') return true
    try {
      await store.refreshCount()
    } catch {
      // 배지를 그대로 두고 다음 주기에 다시 센다 (에러 문구 없음)
    }
    return true
  },
  { interval: POLL_INTERVAL },
)

function onVisibilityChange() {
  if (document.visibilityState === 'visible' && auth.isLoggedIn) polling.start()
}

onMounted(() => {
  if (auth.isLoggedIn) polling.start()
  document.addEventListener('visibilitychange', onVisibilityChange)
})
onBeforeUnmount(() => document.removeEventListener('visibilitychange', onVisibilityChange))

watch(
  () => auth.isLoggedIn,
  (loggedIn) => (loggedIn ? polling.start() : polling.stop()),
)

// ── 드롭다운 목록 (AC15) ──

const open = ref(false)
const list = useMoreList(
  (page) => fetchNotifications({ page, size: NOTIFICATION_PAGE_SIZE }),
  NOTIFICATION_PAGE_SIZE,
)
const listError = computed(() => (list.error.value ? errorMessage(list.error.value) : ''))
const firstLoading = computed(() => list.loading.value && list.items.value.length === 0)
const hasUnread = computed(
  () => store.unreadCount > 0 || list.items.value.some((item) => !item.read),
)

watch(open, (isOpen) => {
  if (isOpen) {
    markAllError.value = ''
    list.reset()
  }
})

// ── 읽음·이동 (AC16) ──

function select(notification) {
  if (!notification.read) {
    notification.read = true
    store.markRead(notification.id).catch(() => {})
  }
  open.value = false
  router.push(notification.link)
}

const markingAll = ref(false)
const markAllError = ref('')

async function markAll() {
  markingAll.value = true
  markAllError.value = ''
  try {
    await store.markAllRead()
    list.items.value.forEach((item) => {
      item.read = true
    })
  } catch (e) {
    markAllError.value = errorMessage(e)
  } finally {
    markingAll.value = false
  }
}

const iconButton =
  'relative flex size-9 shrink-0 items-center justify-center rounded-sm text-text-muted hover:bg-primary-soft focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-primary'
</script>

<template>
  <PopoverRoot v-model:open="open">
    <PopoverTrigger :class="iconButton" :aria-label="label" data-testid="notification-bell">
      <Bell class="size-5" aria-hidden="true" />
      <span
        v-if="badge"
        data-testid="notification-badge"
        aria-hidden="true"
        class="absolute top-1 right-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-danger px-1 text-[10px] leading-none font-bold text-white"
      >
        {{ badge }}
      </span>
    </PopoverTrigger>
    <PopoverPortal>
      <PopoverContent
        align="end"
        :side-offset="4"
        :collision-padding="8"
        aria-label="알림 목록"
        data-testid="notification-panel"
        class="z-50 flex max-h-[min(32rem,80vh)] w-[min(22rem,calc(100vw-1rem))] flex-col rounded-md border border-border bg-surface shadow-2 dark:bg-surface-raised"
      >
        <div class="flex items-center justify-between gap-2 border-b border-border px-3 py-2">
          <h2 class="text-sm font-semibold text-text">알림</h2>
          <div class="flex items-center gap-2">
            <span v-if="markAllError" role="alert" class="text-xs text-danger">
              {{ markAllError }}
            </span>
            <button
              type="button"
              data-testid="mark-all-read"
              class="rounded-sm px-2 py-1 text-xs font-semibold text-primary hover:bg-primary-soft disabled:cursor-not-allowed disabled:text-text-muted disabled:hover:bg-transparent"
              :disabled="!hasUnread || markingAll"
              @click="markAll"
            >
              모두 읽음
            </button>
          </div>
        </div>
        <div class="min-h-0 flex-1 overflow-y-auto p-1">
          <AsyncState
            :loading="firstLoading"
            :error="list.items.value.length === 0 ? listError : ''"
            :empty="list.items.value.length === 0"
            empty-message="새 알림이 없습니다"
            @retry="list.reset()"
          >
            <template #skeleton>
              <div role="status" aria-label="알림 불러오는 중" class="space-y-2 p-2">
                <div
                  v-for="n in 3"
                  :key="n"
                  data-testid="notification-skeleton"
                  class="h-12 animate-pulse rounded-sm bg-surface-raised"
                />
              </div>
            </template>
            <ul class="space-y-0.5">
              <li v-for="item in list.items.value" :key="item.id">
                <NotificationItem :notification="item" @select="select" />
              </li>
            </ul>
            <p v-if="listError" role="alert" class="px-3 py-2 text-xs text-danger">
              {{ listError }}
            </p>
            <button
              v-if="list.hasMore.value"
              type="button"
              data-testid="notification-more"
              class="mt-1 w-full rounded-sm py-2 text-sm font-semibold text-primary hover:bg-primary-soft disabled:text-text-muted"
              :disabled="list.loading.value"
              @click="list.more()"
            >
              더 보기
            </button>
          </AsyncState>
        </div>
      </PopoverContent>
    </PopoverPortal>
  </PopoverRoot>
</template>
