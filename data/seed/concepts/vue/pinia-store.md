---
name: Pinia 스토어
category: vue
difficulty: BEGINNER
reference: https://pinia.vuejs.org/core-concepts/
---

## summary

Pinia는 Vue의 공식 상태 관리 라이브러리다. 로그인 사용자처럼 여러 화면이 함께 쓰는 상태를 스토어 하나에 모아 두고 어디서나 읽고 바꾼다.

## body

## 핵심
- `defineStore("이름", ...)`로 정의한다. state(상태), getters(계산 값), actions(상태를 바꾸는 함수, API 호출)로 구성된다.
- setup 문법을 쓰면 `ref`가 state, `computed`가 getter, 함수가 action이 된다.
- 컴포넌트에서 `useXxxStore()`로 꺼내 쓴다.

## 주의
- 한 화면에서만 쓰는 상태까지 스토어에 넣으면 흐름이 복잡해진다.
- 새로고침하면 스토어 상태는 사라진다. 유지가 필요한 값(토큰 등)은 저장 위치와 보안을 따로 결정한다.

## example

```js
// stores/auth.js
import { defineStore } from "pinia";
import { ref, computed } from "vue";
```

```js
export const useAuthStore = defineStore("auth", () => {
  const user = ref(null);
  const isLoggedIn = computed(() => user.value !== null);

  async function login(email, password) {
    const res = await api.post("/api/auth/login", { email, password });
    user.value = res.data.user;
  }

  function logout() {
    user.value = null;
  }

  return { user, isLoggedIn, login, logout };
});
```
