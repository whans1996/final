---
name: props와 emit
category: vue
difficulty: BEGINNER
reference: https://ko.vuejs.org/guide/components/props
---

## summary

부모 컴포넌트는 props로 자식에게 데이터를 내려주고, 자식은 emit으로 이벤트를 올려 부모에게 알린다. 데이터는 아래로, 이벤트는 위로 흐른다.

## body

## 핵심
- `defineProps`로 받을 값과 타입을 선언한다.
- `defineEmits`로 올릴 이벤트 이름을 선언하고, `emit("이름", 값)`으로 보낸다.
- 부모는 `<Child :item="x" @remove="onRemove" />`처럼 연결한다.

## 최신 문법 (Vue 3.5)
- `const { title, size = "md" } = defineProps({ ... })`처럼 props를 구조 분해해도 반응성이 유지되고, 기본값을 자바스크립트 문법으로 줄 수 있다.
- 단, 구조 분해한 값을 `watch`하려면 `watch(() => title, ...)`처럼 함수로 감싼다.
- 부모와 값을 양방향으로 묶는 경우(입력 컴포넌트)는 props + emit 대신 `defineModel`(3.4+)이 더 간단하다.

## 주의
- 자식이 props를 직접 바꾸면 안 된다(읽기 전용). 바꾸고 싶으면 이벤트를 올려 부모가 바꾸게 한다.
- 여러 단계 아래로 계속 전달해야 하면 provide/inject나 Pinia 스토어를 고려한다.

## example

```vue
<!-- ConceptItem.vue -->
<script setup>
const props = defineProps({ concept: { type: Object, required: true } });
const emit = defineEmits(["wish"]);
</script>

<template>
  <li>
    {{ props.concept.name }}
    <button @click="emit('wish', props.concept.id)">찜</button>
  </li>
</template>
```

```vue
<!-- 부모 -->
<ConceptItem v-for="c in concepts" :key="c.id" :concept="c" @wish="toggleWish" />
```
