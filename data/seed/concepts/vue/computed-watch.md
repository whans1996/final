---
name: computed와 watch
category: vue
difficulty: JUNIOR
reference: https://ko.vuejs.org/guide/essentials/computed
---

## summary

computed는 다른 상태로부터 계산되는 값을 캐시해 두는 반응형 값이고, watch는 상태가 바뀔 때 API 호출 같은 부수 효과를 실행한다.

## body

## 핵심
- computed: 의존하는 값이 바뀔 때만 다시 계산한다. 템플릿의 복잡한 식을 옮기기 좋다. 계산만 하고 상태를 바꾸지 않는다.
- watch: 특정 값을 지켜보다가 바뀌면 콜백을 실행한다. 검색어 변경 → 목록 재조회 같은 경우.
- `watch(..., { immediate: true })`로 처음에도 한 번 실행할 수 있다.

## 주의
- computed 안에서 API를 부르거나 다른 상태를 바꾸지 않는다.
- 계산으로 충분한 것을 watch + 별도 상태로 만들면 동기화 버그가 생긴다.

## example

```vue
<script setup>
import { ref, computed, watch } from "vue";

const keyword = ref("");
const concepts = ref([]);

const count = computed(() => concepts.value.length);

watch(keyword, async (value) => {
  concepts.value = await fetchConcepts({ keyword: value });
});
</script>
```
