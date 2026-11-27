---
name: CompletableFuture (비동기 조합)
category: java
difficulty: SENIOR
reference: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/CompletableFuture.html
---

## summary

CompletableFuture는 나중에 끝나는 작업의 결과를 나타내고, thenApply·thenCombine·allOf로 여러 비동기 작업을 이어 붙이거나 함께 기다린다.

## body

## 핵심
- `supplyAsync(작업, executor)`: 다른 스레드에서 작업을 시작한다.
- `thenApply`(결과 변환), `thenCompose`(이어지는 비동기 작업), `thenCombine`(두 결과 합치기), `allOf`(모두 기다리기).
- `exceptionally`·`handle`로 실패를 처리한다.
- 스프링 `@Async` 메서드는 `CompletableFuture<T>`를 반환할 수 있다.

## 주의
- executor를 지정하지 않으면 공용 ForkJoinPool을 쓴다. 블로킹 I/O(HTTP, DB)는 전용 스레드 풀을 넘긴다.
- `join()`·`get()`을 바로 부르면 결국 기다리게 되어 비동기 이점이 없다. 모아서 마지막에 기다린다.
- 비동기 스레드에서는 요청 스레드의 `SecurityContext`와 트랜잭션이 이어지지 않는다.

## example

```java
ExecutorService io = Executors.newFixedThreadPool(8);
```

```java
CompletableFuture<Profile> profile =
        CompletableFuture.supplyAsync(() -> profileClient.get(userId), io);
CompletableFuture<Stats> stats =
        CompletableFuture.supplyAsync(() -> statsClient.get(userId), io);
```

```java
Dashboard dashboard = profile
        .thenCombine(stats, Dashboard::new)
        .exceptionally(ex -> Dashboard.empty())
        .join();   // 두 호출을 동시에 보내고 마지막에 한 번 기다린다
```
