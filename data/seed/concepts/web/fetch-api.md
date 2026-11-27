---
name: Fetch API
category: web
difficulty: BEGINNER
reference: https://developer.mozilla.org/ko/docs/Web/API/Fetch_API/Using_Fetch
---

## summary

Fetch API는 브라우저에서 HTTP 요청을 보내는 표준 함수 fetch()다. Promise를 반환하므로 async/await와 함께 쓴다. axios 같은 라이브러리도 내부 개념은 같다.

## body

## 핵심
- `fetch(url, { method, headers, body })` → `Response`. 본문은 `await res.json()`으로 꺼낸다.
- JSON을 보낼 때는 `Content-Type: application/json`과 `JSON.stringify(body)`.
- 쿠키를 함께 보내려면 `credentials: "include"`(다른 출처) 또는 `"same-origin"`.
- `AbortController`로 요청을 취소하거나 타임아웃을 건다.

## 주의
- **fetch는 404·500에서도 예외를 던지지 않는다.** 네트워크 실패일 때만 reject된다. `res.ok`를 직접 확인한다. axios는 2xx가 아니면 예외를 던지므로 둘의 동작이 다르다.
- 응답 본문은 한 번만 읽을 수 있다.
- 204 응답에 `res.json()`을 부르면 오류가 난다.

## example

```js
async function createPost(post) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 10_000);
  try {
    const res = await fetch("/api/posts", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(post),
      credentials: "same-origin",
      signal: controller.signal,
    });
    if (!res.ok) {
      const error = await res.json();          // {code, message}
      throw new Error(error.message);
    }
    return await res.json();
  } finally {
    clearTimeout(timer);
  }
}
```
