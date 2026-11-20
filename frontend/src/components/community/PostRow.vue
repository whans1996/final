<script setup>
// SC-14 글 목록 한 행 (F-17 AC13). BE PostSummaryResponse 그대로 받는다. 모바일에서는 제목 줄과 정보 줄 두 줄이 된다.
import { computed } from 'vue'
import { ATTACHMENT_ICONS, authorName, boardChip, formatShortDate } from './boardTypes'

const props = defineProps({
  post: { type: Object, required: true },
})

const ATTACHMENT_LABELS = { ANALYSIS: '분석', CONCEPT: '카드' }

const attachment = computed(() => {
  const type = props.post.attachmentType
  return type ? `${ATTACHMENT_ICONS[type]} ${ATTACHMENT_LABELS[type]}` : ''
})
</script>

<template>
  <RouterLink :to="`/community/${post.id}`" class="row" :data-post-id="post.id">
    <span class="main">
      <span class="chip" data-role="board">{{ boardChip(post.boardType) }}</span>
      <span class="title" data-role="title">{{ post.title }}</span>
      <span v-if="attachment" class="attachment" data-role="attachment">{{ attachment }}</span>
      <span class="comments" data-role="comments">[{{ post.commentCount }}]</span>
    </span>
    <span class="meta">
      <span data-role="author">{{ authorName(post) }}</span>
      <span data-role="date">{{ formatShortDate(post.createdAt) }}</span>
      <span data-role="views" :aria-label="`조회 ${post.viewCount}`"
        >조회 {{ post.viewCount }}</span
      >
    </span>
  </RouterLink>
</template>

<style scoped>
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-3) var(--space-2);
  border-bottom: 1px solid var(--color-border);
  color: var(--color-text);
  text-decoration: none;
}

.row:hover .title {
  text-decoration: underline;
}

.main {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
}

.chip {
  flex: none;
  padding: 0 var(--space-2);
  border-radius: var(--radius);
  background: var(--color-background);
  color: var(--color-text-muted);
  font-size: 0.85em;
}

.title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment,
.comments {
  flex: none;
  color: var(--color-text-muted);
  font-size: 0.85em;
}

.comments {
  color: var(--color-primary);
}

.meta {
  display: flex;
  flex: none;
  gap: var(--space-3);
  color: var(--color-text-muted);
  font-size: 0.85em;
}

@media (max-width: 640px) {
  .row {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-1);
  }

  .main {
    width: 100%;
  }
}
</style>
