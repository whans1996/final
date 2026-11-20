<script setup>
// 피드 항목 하나 (F-04 AC11, SC-17). 1줄 이니셜·닉네임(SC-18 링크)·상대 시각, 2줄 종류별 문장·대상 링크.
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { FEED_TYPES } from '@/components/feed/feedTypes'
import { formatRelativeTime } from '@/utils/relativeTime'

const props = defineProps({
  item: { type: Object, required: true },
  now: { type: Date, default: () => new Date() },
})

const kind = computed(() => FEED_TYPES[props.item.type] ?? null)
const initial = computed(() => ([...(props.item.actor.nickname ?? '')][0] ?? '?').toUpperCase())
const when = computed(() => formatRelativeTime(props.item.createdAt, props.now))
</script>

<template>
  <li v-if="kind" class="feed-item" :data-activity="item.id">
    <div class="head">
      <RouterLink
        :to="`/users/${item.actor.id}`"
        class="actor"
        data-link="actor-avatar"
        tabindex="-1"
      >
        <span class="avatar" aria-hidden="true">{{ initial }}</span>
      </RouterLink>
      <RouterLink :to="`/users/${item.actor.id}`" class="nickname" data-link="actor">{{
        item.actor.nickname
      }}</RouterLink>
      <time :datetime="item.createdAt" class="time">{{ when }}</time>
    </div>
    <p class="body">
      <span class="sentence">{{ kind.sentence }}</span>
      <span aria-hidden="true"> · </span>
      <RouterLink :to="kind.to(item.target.id)" class="target" data-link="target">
        {{ item.target.title }}
        <span v-if="item.target.rating != null" class="rating">★{{ item.target.rating }}</span>
      </RouterLink>
    </p>
  </li>
</template>

<style scoped>
.feed-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-4) 0;
  border-bottom: 1px solid var(--color-border);
}

.head {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
}

.actor {
  display: inline-flex;
  text-decoration: none;
}

.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2em;
  height: 2em;
  border-radius: 50%;
  color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 12%, transparent);
  font-weight: 700;
}

.nickname {
  min-width: 0;
  overflow-wrap: anywhere;
  color: inherit;
  font-weight: 600;
}

.time {
  margin-left: auto;
  flex: none;
  color: var(--color-text-muted);
  font-size: 0.9em;
}

.body {
  margin: 0;
  overflow-wrap: anywhere;
}

.sentence {
  color: var(--color-text-muted);
}

.rating {
  margin-left: var(--space-1);
  color: var(--color-warning, #d4a017);
}
</style>
