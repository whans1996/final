---
name: SSE (서버 전송 이벤트)
category: web
difficulty: JUNIOR
reference: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html
---

## summary

SSE(Server-Sent Events)는 서버가 하나의 HTTP 연결을 열어 둔 채 클라이언트에게 이벤트를 계속 밀어주는 방식이다. 진행률, 알림처럼 서버 → 클라이언트 단방향 실시간 갱신에 쓴다.

## body

## 핵심
- 스프링 MVC는 `SseEmitter`를 반환해 SSE를 보낸다. 응답 타입은 `text/event-stream`.
- 브라우저는 `new EventSource(url)`로 받고, 연결이 끊기면 자동으로 다시 연결한다.
- 양방향 통신이 필요하면 WebSocket, 몇 초 지연이 괜찮으면 폴링(주기적 조회)이 더 단순하다.

## 비교
- 폴링: 구현이 가장 쉽다. 요청이 많고 지연이 있다.
- SSE: HTTP 그대로, 서버 → 클라이언트 단방향. 연결을 오래 유지한다.
- WebSocket: 양방향. 별도 프로토콜과 인프라 설정이 필요하다.

## 주의
- 연결마다 서버 자원을 잡는다. 타임아웃을 정하고, 완료·오류 시 `complete()`로 닫는다.
- 브라우저 `EventSource`는 커스텀 헤더(Authorization)를 넣을 수 없다. 쿠키 인증을 쓰거나 다른 방식을 고른다.
- 프록시(Nginx)가 응답을 버퍼링하면 이벤트가 늦게 도착한다.

## example

```java
@GetMapping(value = "/api/analyses/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter subscribe(@PathVariable long id) {
    SseEmitter emitter = new SseEmitter(Duration.ofMinutes(5).toMillis());
    progressPublisher.register(id, emitter);
    emitter.onCompletion(() -> progressPublisher.remove(id, emitter));
    emitter.onTimeout(emitter::complete);
    return emitter;
}
```

```java
// 진행률이 바뀔 때
emitter.send(SseEmitter.event().name("progress").data(Map.of("status", "EXPLAINING", "progress", 60)));
```

```js
// 브라우저
const source = new EventSource("/api/analyses/42/events");
source.addEventListener("progress", (e) => {
  const { status, progress } = JSON.parse(e.data);
});
```
