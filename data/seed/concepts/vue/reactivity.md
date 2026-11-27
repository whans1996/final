---
name: 반응성 (ref·reactive)
category: vue
difficulty: BEGINNER
reference: https://ko.vuejs.org/guide/essentials/reactivity-fundamentals
---

## summary

Vue의 반응성은 상태가 바뀌면 그 상태를 쓰는 화면이 자동으로 다시 그려지는 성질이다. Composition API에서는 ref와 reactive로 반응형 상태를 만든다.

## body

## 핵심
- `ref(값)`: 어떤 타입이든 감싼다. 스크립트에서는 `.value`로 읽고 쓰고, 템플릿에서는 `.value` 없이 쓴다.
- `reactive(객체)`: 객체 자체를 반응형으로 만든다. 객체·배열에만 쓸 수 있다.
- 공식 문서는 기본 선택으로 `ref`를 권한다.

## Options API와 비교
- Vue 2 시절 방식인 Options API는 `data()`, `methods`, `computed` 객체 속성으로 컴포넌트를 나눠 쓰고 `this.count`로 접근한다.
- Vue 3의 `<script setup>` + Composition API는 `ref`·함수를 그냥 선언하고, 관련 로직을 한곳에 모으거나 컴포저블로 빼기 쉽다.
- AI가 두 방식을 한 컴포넌트에 섞어 내는 경우가 있다. 한 프로젝트 안에서는 한 방식으로 통일한다.

## 주의
- `reactive` 객체를 구조 분해하거나 통째로 새 객체를 대입하면 반응성이 끊긴다.
- 스크립트에서 `.value`를 빼먹는 실수가 흔하다.

## example

```vue
<script setup>
import { ref } from "vue";

const count = ref(0);
const todos = ref([]);

function add(text) {
  todos.value.push({ id: Date.now(), text });
  count.value++;
}
</script>

<template>
  <p>할 일 {{ count }}개</p>
  <button @click="add('새 할 일')">추가</button>
</template>
```
