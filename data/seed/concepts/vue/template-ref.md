---
name: 템플릿 ref (useTemplateRef)
category: vue
difficulty: JUNIOR
reference: https://ko.vuejs.org/guide/essentials/template-refs
---

## summary

템플릿 ref는 렌더링된 DOM 요소나 자식 컴포넌트 인스턴스에 직접 접근하는 방법이다. 포커스 주기, 스크롤, 차트 라이브러리 연결처럼 DOM을 직접 다뤄야 할 때 쓴다.

## body

## 핵심
- 템플릿에 `ref="이름"`을 붙이고, 스크립트에서 `useTemplateRef("이름")`(Vue 3.5+)으로 가져온다.
- DOM은 마운트 뒤에 생기므로 `onMounted` 이후에 `.value`를 쓴다.
- 자식 컴포넌트는 `<script setup>`이 기본으로 닫혀 있어, 자식이 `defineExpose`로 공개한 것만 접근할 수 있다.

## 최신 문법
3.5 이전에는 `const input = ref(null)`처럼 템플릿의 ref 이름과 **같은 이름의 변수**를 선언하는 방식이었다. 지금도 동작하지만 이름이 우연히 같아서 연결되는 구조라 헷갈리기 쉽다.

## 주의
- 상태로 해결할 수 있는 일(보이기·숨기기, 값 바꾸기)을 DOM 직접 조작으로 하지 않는다. 반응성과 어긋난다.
- `v-if`로 사라진 요소의 ref는 `null`이다.

## example

```vue
<script setup>
import { useTemplateRef, onMounted } from "vue";

const searchInput = useTemplateRef("search");

onMounted(() => {
  searchInput.value.focus();   // 화면이 열리면 검색창에 포커스
});
</script>

<template>
  <input ref="search" type="search" placeholder="개념 검색" />
</template>
```
