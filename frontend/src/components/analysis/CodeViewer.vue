<script setup>
// 줄 번호가 붙은 코드 상자 (F-06 6절). 하이라이트 라이브러리는 쓰지 않는다.
// 긴 줄은 이 상자 안에서만 가로 스크롤한다 (375px에서 페이지 가로 스크롤 없음, NF-02).
// highlight({ start, end })가 파일 줄 수 안이면 그 줄들을 형광펜 색으로 칠하고 첫 줄을 화면 가운데로 스크롤한다.
// 범위가 줄 수를 넘으면 강조하지 않는다 (I-1a AC15).
// scrollReady가 false면 스크롤을 미룬다: 위쪽 레이아웃(모바일에서 코드 위에 쌓이는 파일 목록)이 아직 그려지지 않았으면
// 지금 스크롤해도 나중에 코드가 아래로 밀려나기 때문이다. true가 되는 순간 스크롤한다.
import { computed, nextTick, ref, watch } from 'vue'

const props = defineProps({
  content: { type: String, default: '' },
  label: { type: String, default: '코드' },
  highlight: { type: Object, default: null },
  scrollReady: { type: Boolean, default: true },
})

const root = ref(null)

const lines = computed(() => {
  const all = props.content.split('\n')
  // 파일 끝 줄바꿈 때문에 생기는 마지막 빈 줄은 보여 주지 않는다
  if (all.length > 1 && all[all.length - 1] === '') all.pop()
  return all
})

const range = computed(() => {
  const h = props.highlight
  if (!h || !Number.isInteger(h.start) || !Number.isInteger(h.end)) return null
  return h.start >= 1 && h.start <= h.end && h.end <= lines.value.length ? h : null
})

function highlighted(lineNo) {
  return range.value !== null && lineNo >= range.value.start && lineNo <= range.value.end
}

watch(
  [range, lines, () => props.scrollReady],
  async () => {
    if (!range.value || !props.scrollReady) return
    await nextTick()
    root.value
      ?.querySelector(`[data-line="${range.value.start}"]`)
      ?.scrollIntoView?.({ block: 'center' })
  },
  { immediate: true, flush: 'post' },
)
</script>

<template>
  <div
    ref="root"
    class="max-w-full overflow-x-auto rounded-md border border-border bg-code-bg text-code-text"
    role="region"
    :aria-label="label"
    tabindex="0"
  >
    <pre class="m-0 py-3 font-mono text-sm leading-relaxed break-normal whitespace-pre"><code><span
      v-for="(text, index) in lines"
      :key="index"
      class="block pr-4"
      :class="{ 'bg-highlight': highlighted(index + 1) }"
      :data-line="index + 1"
      :data-highlighted="highlighted(index + 1) ? 'true' : undefined"
    ><span
      class="mr-3 inline-block min-w-[3.5em] border-r border-border pr-2 text-right text-text-muted select-none"
      aria-hidden="true"
    >{{ index + 1 }}</span>{{ text || ' ' }}</span></code></pre>
  </div>
</template>
