<script setup>
// 파일 경로를 `/` 뒤에서만 줄바꿈하도록 표시한다 (좁은 목록에서 `TodoC|ontroller.java`처럼 끊기지 않게).
import { computed } from 'vue'

const props = defineProps({
  path: { type: String, required: true },
})

const parts = computed(() => props.path.split('/'))
</script>

<template>
  <span class="file-path"
    ><template v-for="(part, index) in parts" :key="index"
      >{{ part }}<template v-if="index < parts.length - 1">/<wbr /></template></template
  ></span>
</template>

<style scoped>
.file-path {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  overflow-wrap: break-word;
  word-break: normal;
}
</style>
