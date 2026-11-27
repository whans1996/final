---
name: 컴포저블
category: vue
difficulty: JUNIOR
reference: https://ko.vuejs.org/guide/reusability/composables
---

## summary

컴포저블은 상태와 로직을 묶어 여러 컴포넌트에서 재사용하는 함수다. 이름은 보통 use로 시작한다(useFetch, useAuth).

## body

## 핵심
- 안에서 `ref`, `computed`, `watch`, 생명주기 훅을 써서 상태 로직을 캡슐화하고 필요한 값을 반환한다.
- 호출할 때마다 새 상태가 만들어진다. 전역으로 공유할 상태는 Pinia 스토어가 더 적합하다.
- 반환할 때 ref를 그대로 돌려주면 구조 분해해도 반응성이 유지된다.

## 주의
- `<script setup>` 안이나 setup 시점에 동기적으로 호출해야 생명주기 훅이 올바르게 연결된다.

## example

```js
// composables/useFetch.js
import { ref } from "vue";
```

```js
export function useFetch(loader) {
  const data = ref(null);
  const error = ref(null);
  const loading = ref(false);

  async function run(...args) {
    loading.value = true;
    error.value = null;
    try {
      data.value = await loader(...args);
    } catch (e) {
      error.value = e;
    } finally {
      loading.value = false;
    }
  }

  return { data, error, loading, run };
}
```
