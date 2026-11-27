---
name: storeToRefs
category: vue
difficulty: JUNIOR
reference: https://pinia.vuejs.org/core-concepts/#Destructuring-from-a-Store
---

## summary

Pinia 스토어를 구조 분해하면 반응성이 끊긴다. storeToRefs로 state와 getter를 ref로 꺼내면 반응성을 유지한 채 구조 분해할 수 있다.

## body

## 핵심
- `const { user } = useAuthStore()`는 그 순간의 값만 복사한다. 이후 변경이 화면에 반영되지 않는다.
- `const { user } = storeToRefs(store)`는 ref를 돌려주므로 반응성이 유지된다.
- action(함수)은 `storeToRefs`로 꺼내지 않고 스토어에서 직접 구조 분해한다.

## 주의
"로그인했는데 헤더가 안 바뀐다" 같은 버그의 흔한 원인이다.

## example

```vue
<script setup>
import { storeToRefs } from "pinia";
import { useAuthStore } from "@/stores/auth";

const store = useAuthStore();
const { user, isLoggedIn } = storeToRefs(store); // 반응성 유지
const { logout } = store;                         // 액션은 그대로
</script>

<template>
  <span v-if="isLoggedIn">{{ user.nickname }}</span>
  <button v-if="isLoggedIn" @click="logout">로그아웃</button>
</template>
```
