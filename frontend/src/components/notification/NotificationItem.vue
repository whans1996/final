<script setup>
// 알림 한 줄 (NT-1 AC15·AC16). actor가 있으면 "{닉네임}님이 " + message, 없으면 message만.
// 누르면 select를 올린다 (읽음 처리·이동은 NotificationBell이 한다).
import { computed } from 'vue'
import { formatRelativeTime } from '@/utils/relativeTime'
import { notificationIcon } from './notificationIcons'

const props = defineProps({
  /** @type {import('vue').PropType<import('@/api/notifications').Notification>} */
  notification: { type: Object, required: true },
  /** 테스트에서 상대 시각 기준을 고정한다 */
  now: { type: Date, default: undefined },
})

const emit = defineEmits(['select'])

const icon = computed(() => notificationIcon(props.notification.type))
// 템플릿 줄바꿈이 공백을 더하지 않게 이어 붙일 문장을 여기서 만든다
const sentence = computed(() =>
  props.notification.actor ? `님이 ${props.notification.message}` : props.notification.message,
)
const time = computed(() => formatRelativeTime(props.notification.createdAt, props.now))
</script>

<template>
  <button
    type="button"
    data-testid="notification-item"
    :data-read="notification.read ? 'true' : 'false'"
    :class="[
      'flex w-full items-start gap-3 rounded-sm px-3 py-2.5 text-left text-sm hover:bg-primary-soft focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-primary',
      notification.read ? 'text-text-muted' : 'bg-primary-soft/50 text-text',
    ]"
    @click="emit('select', notification)"
  >
    <component :is="icon" class="mt-0.5 size-4 shrink-0" aria-hidden="true" />
    <span class="min-w-0 flex-1">
      <span class="block break-words"
        ><strong v-if="notification.actor" class="font-semibold">{{
          notification.actor.nickname
        }}</strong
        >{{ sentence }}</span
      >
      <span class="mt-0.5 block text-xs text-text-muted">{{ time }}</span>
    </span>
    <span
      v-if="!notification.read"
      data-testid="unread-dot"
      class="mt-1.5 size-2 shrink-0 rounded-full bg-primary"
    >
      <span class="sr-only">안 읽음</span>
    </span>
  </button>
</template>
