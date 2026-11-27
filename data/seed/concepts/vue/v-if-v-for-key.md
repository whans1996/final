---
name: v-if·v-for와 key
category: vue
difficulty: BEGINNER
reference: https://ko.vuejs.org/guide/essentials/list
---

## summary

v-if는 조건에 따라 요소를 만들거나 없애고, v-for는 배열을 반복해 요소를 그린다. v-for에는 항목을 구분하는 고유한 :key를 붙여야 Vue가 바뀐 부분만 정확히 갱신한다.

## body

## 핵심
- `v-if` / `v-else-if` / `v-else`: 조건이 거짓이면 DOM에서 제거된다.
- `v-show`: CSS `display`만 바꾼다. 자주 토글하는 요소에 유리하다.
- `v-for="item in items" :key="item.id"`: key는 DB id처럼 바뀌지 않는 고유 값을 쓴다.

## 주의
- **배열 인덱스를 key로 쓰지 않는다.** 항목을 추가·삭제·정렬하면 인덱스가 바뀌어 입력 값이나 상태가 엉뚱한 항목에 붙는다.
- **같은 요소에 v-if와 v-for를 함께 쓰지 않는다.** Vue 3에서는 v-if가 먼저 평가되어 반복 변수를 쓸 수 없다(Vue 2와 반대). 걸러진 목록은 computed로 만들거나 `<template v-for>`로 감싼다.
- 목록이 비었을 때의 화면(빈 상태)을 `v-if`로 따로 보여 준다.

## example

```vue
<script setup>
import { computed } from "vue";
const props = defineProps({ concepts: Array });
const published = computed(() => props.concepts.filter((c) => c.status === "PUBLISHED"));
</script>

<template>
  <p v-if="published.length === 0">아직 공개된 카드가 없습니다.</p>
  <ul v-else>
    <li v-for="concept in published" :key="concept.id">
      {{ concept.name }}
      <span v-show="concept.wished">★</span>
    </li>
  </ul>
</template>
```
