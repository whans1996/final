---
name: 라우트와 동적 매칭 (Vue Router)
category: vue
difficulty: BEGINNER
reference: https://router.vuejs.org/guide/essentials/dynamic-matching.html
---

## summary

Vue Router는 URL 경로와 화면 컴포넌트를 연결한다. /users/:id처럼 경로 일부를 파라미터로 받아 같은 화면에서 다른 데이터를 보여 주고, 화면 코드를 필요할 때 불러오도록(lazy loading) 나눈다.

## body

## 핵심
- 라우트 정의: `{ path: "/concepts/:id", name: "concept-detail", component: ... }`.
- 컴포넌트에서 `useRoute().params.id`로 값을 읽는다. **값은 문자열**이다.
- 이동: `<RouterLink :to="{ name: 'concept-detail', params: { id } }">` 또는 `router.push(...)`. 이름으로 이동하면 경로가 바뀌어도 코드를 고치지 않는다.
- 지연 로딩: `component: () => import("@/views/ConceptDetailView.vue")`. 첫 화면 로딩이 가벼워진다.
- 없는 경로: `{ path: "/:pathMatch(.*)*", component: NotFoundView }`.

## 주의
- `/concepts/1`에서 `/concepts/2`로 이동하면 **같은 컴포넌트가 재사용**되어 `onMounted`가 다시 실행되지 않는다. `watch(() => route.params.id, load)`로 다시 불러온다.
- 숫자로 쓸 때는 `Number(route.params.id)`로 바꾸고, 잘못된 값이면 404 화면으로 보낸다.

## example

```js
// router/index.js
const routes = [
  { path: "/", name: "home", component: HomeView },
  {
    path: "/concepts/:id",
    name: "concept-detail",
    component: () => import("@/views/ConceptDetailView.vue"),   // 지연 로딩
  },
  { path: "/:pathMatch(.*)*", name: "not-found", component: NotFoundView },
];
```

```vue
<!-- ConceptDetailView.vue -->
<script setup>
import { ref, watch } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();
const concept = ref(null);

watch(
  () => route.params.id,
  async (id) => { concept.value = await fetchConcept(Number(id)); },
  { immediate: true },
);
</script>
```
