---
name: provide/inject
category: vue
difficulty: JUNIOR
reference: https://ko.vuejs.org/guide/components/provide-inject
---

## summary

provide/inject는 상위 컴포넌트가 제공한 값을 중간 컴포넌트를 거치지 않고 하위 컴포넌트가 바로 꺼내 쓰는 방법이다. props를 여러 단계로 계속 넘기는 문제(prop drilling)를 줄인다.

## body

## 핵심
- 상위: `provide(키, 값)`. 하위: `inject(키, 기본값)`.
- ref를 provide하면 하위에서도 반응형으로 갱신된다.
- 키는 문자열보다 `Symbol`을 파일로 export해 쓰면 이름 충돌을 막는다.
- 앱 전체에는 `app.provide`로 제공한다.

## 언제 무엇을
- 부모-자식 한 단계: props/emit.
- 특정 컴포넌트 트리 안에서만 공유(탭 그룹, 폼 컨텍스트): provide/inject.
- 앱 전체에서 공유(로그인 사용자): Pinia 스토어.

## 주의
- 하위에서 값을 마음대로 바꾸면 흐름을 추적하기 어렵다. `readonly`로 제공하고, 바꾸는 함수를 함께 provide한다.
- 어디서 제공된 값인지 코드만 보고 찾기 어렵다. 남용하지 않는다.

## example

```js
// keys.js
export const FormKey = Symbol("form");
```

```vue
<!-- ConceptForm.vue (상위) -->
<script setup>
import { provide, reactive, readonly } from "vue";
import { FormKey } from "./keys";

const errors = reactive({});
function setError(field, message) { errors[field] = message; }
provide(FormKey, { errors: readonly(errors), setError });
</script>
```

```vue
<!-- FormField.vue (몇 단계 아래) -->
<script setup>
import { inject } from "vue";
import { FormKey } from "./keys";

const props = defineProps({ name: String });
const { errors } = inject(FormKey);
</script>

<template>
  <slot />
  <p v-if="errors[props.name]" class="error">{{ errors[props.name] }}</p>
</template>
```
