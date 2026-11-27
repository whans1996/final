---
name: v-model과 defineModel
category: vue
difficulty: BEGINNER
reference: https://ko.vuejs.org/guide/components/v-model
---

## summary

v-model은 입력 값과 상태를 양방향으로 묶는다. Vue 3.4부터 자식 컴포넌트는 defineModel() 한 줄로 부모의 v-model을 받을 수 있다.

## body

## 핵심
- 기본 요소: `<input v-model="keyword">`는 값 표시(`:value`)와 입력 이벤트(`@input`)를 한 번에 처리한다.
- 수식어: `v-model.trim`(앞뒤 공백 제거), `v-model.number`(숫자 변환), `v-model.lazy`(change 시점에 반영).
- 컴포넌트: 자식에서 `const model = defineModel()`을 선언하면, 부모의 `v-model` 값을 읽고 `model.value = ...`로 바꿀 수 있다.
- 여러 값을 묶으려면 이름을 붙인다: 부모 `v-model:title`, 자식 `defineModel("title")`.

## 최신 문법
3.4 이전에는 자식이 `modelValue` prop과 `update:modelValue` 이벤트를 직접 선언해야 했다. AI 코드에서 이 방식이 보이면 예전 문법이다(지금도 동작한다).

## 주의
- 같은 상태를 여러 컴포넌트가 v-model로 고치면 흐름을 추적하기 어렵다. 공유 상태는 스토어로 옮긴다.

## example

```vue
<!-- SearchInput.vue -->
<script setup>
const keyword = defineModel({ type: String, default: "" });
</script>

<template>
  <input v-model.trim="keyword" placeholder="개념 검색" />
</template>
```

```vue
<!-- 부모 -->
<script setup>
import { ref } from "vue";
const keyword = ref("");
</script>

<template>
  <SearchInput v-model="keyword" />
  <p>검색어: {{ keyword }}</p>
</template>
```
