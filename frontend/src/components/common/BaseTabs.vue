<script setup>
// 공통 탭 (F-29a AC11). Reka UI Tabs를 감싸 역할·aria·←/→ 키를 맡긴다.
// 탭 내용은 item.value 이름의 슬롯으로 넣는다: <BaseTabs v-model="level" :items="..."><template #easy>…</template></BaseTabs>
import { TabsContent, TabsIndicator, TabsList, TabsRoot, TabsTrigger } from 'reka-ui'

defineProps({
  items: { type: Array, required: true }, // [{ value, label }]
  label: { type: String, default: undefined }, // tablist의 aria-label
})

const model = defineModel({ type: String, required: true })
</script>

<template>
  <TabsRoot v-model="model" class="flex flex-col gap-4">
    <TabsList
      :aria-label="label"
      class="relative flex gap-1 overflow-x-auto border-b border-border"
    >
      <TabsTrigger
        v-for="item in items"
        :key="item.value"
        :value="item.value"
        class="-mb-px shrink-0 border-b-2 border-transparent px-3 py-2 text-sm font-semibold text-text-muted transition-colors hover:text-text focus-visible:outline-2 focus-visible:outline-primary data-[state=active]:border-primary data-[state=active]:text-primary"
      >
        {{ item.label }}
      </TabsTrigger>
      <TabsIndicator class="hidden" />
    </TabsList>
    <TabsContent
      v-for="item in items"
      :key="item.value"
      :value="item.value"
      class="focus-visible:outline-2 focus-visible:outline-primary"
    >
      <slot :name="item.value" />
    </TabsContent>
  </TabsRoot>
</template>
