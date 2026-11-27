---
name: async/await와 Promise
category: web
difficulty: BEGINNER
reference: https://developer.mozilla.org/ko/docs/Web/JavaScript/Reference/Statements/async_function
---

## summary

Promise는 나중에 끝나는 작업(서버 요청 등)의 결과를 나타내는 객체이고, async/await는 그 결과를 동기 코드처럼 순서대로 기다리게 해 주는 문법이다.

## body

## 핵심
- `async` 함수는 항상 Promise를 반환한다.
- `await`는 Promise가 끝날 때까지 그 함수의 실행을 멈추고 결과값을 꺼낸다.
- 실패(reject)는 `try/catch`로 잡는다.

## AI 코드에서 보이는 모습
Vue 컴포넌트와 Pinia 액션에서 API를 호출할 때 거의 항상 쓰인다.

## 주의
- `await`를 빼먹으면 값 대신 Promise 객체를 쓰게 된다.
- 서로 독립된 요청을 하나씩 await하면 느리다. `Promise.all`로 동시에 보낸다.

## example

```js
async function loadDashboard(userId) {
  try {
    const [profile, stats] = await Promise.all([
      api.get("/api/users/" + userId),
      api.get("/api/users/" + userId + "/stats"),
    ]);
    return { profile: profile.data, stats: stats.data };
  } catch (error) {
    console.error("대시보드 로딩 실패", error);
    throw error;
  }
}
```
