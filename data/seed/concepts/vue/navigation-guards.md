---
name: 네비게이션 가드 (Vue Router)
category: vue
difficulty: JUNIOR
reference: https://router.vuejs.org/guide/advanced/navigation-guards.html
---

## summary

네비게이션 가드는 라우트 이동 직전에 실행되는 함수다. 로그인하지 않은 사용자를 로그인 화면으로 보내는 등 이동을 허용·취소·변경할 수 있다.

## body

## 핵심
- 전역 가드 `router.beforeEach((to, from) => ...)`에서 `false`를 반환하면 이동을 취소하고, 경로를 반환하면 그곳으로 보낸다.
- 라우트에 `meta: { requiresAuth: true }`를 달아 가드에서 확인하는 방식이 일반적이다.
- 가드 안에서 Pinia 스토어를 쓸 수 있다(앱에 Pinia가 설치된 뒤 호출).

## 주의
프론트엔드 가드는 화면 이동만 막는다. 보안은 반드시 백엔드 API가 인증·인가로 지켜야 한다.

## example

```js
// router/index.js
router.beforeEach((to) => {
  const auth = useAuthStore();
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: "login", query: { redirect: to.fullPath } };
  }
});
```

```js
// 라우트 정의
{ path: "/analyses", component: AnalysisList, meta: { requiresAuth: true } }
```
