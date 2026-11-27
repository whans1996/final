---
name: 가상 스레드 (Java 21)
category: java
difficulty: SENIOR
reference: https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html
---

## summary

가상 스레드는 Java 21에 정식 도입된 가벼운 스레드다. 운영체제 스레드 하나 위에서 수많은 가상 스레드가 번갈아 실행되어, I/O 대기가 많은 서버에서 요청마다 스레드를 써도 자원이 부족해지지 않는다.

## body

## 핵심
- **Java 21부터** 쓸 수 있다. 바이브노트 백엔드는 Java 17이라 쓰지 않는다.
- DB·HTTP 응답을 기다리는 동안 가상 스레드가 운영체제 스레드를 내려놓으므로, 블로킹 코드를 그대로 두고도 동시 처리량이 늘어난다.
- 만들기: `Thread.ofVirtual().start(...)`, `Executors.newVirtualThreadPerTaskExecutor()`.
- Spring Boot 3.2 이상 + Java 21이면 `spring.threads.virtual.enabled=true` 한 줄로 웹 요청 처리와 `@Async`에 가상 스레드를 쓴다.

## 주의
- CPU 계산이 많은 작업은 빨라지지 않는다. 효과는 I/O 대기가 많을 때 난다.
- 가상 스레드는 싸게 만드는 것이 전제라 풀링하지 않는다.
- 동시 요청이 늘어나는 만큼 DB 커넥션 풀이나 외부 API 한도가 먼저 병목이 된다. 동시성 제한은 세마포어 등으로 따로 건다.
- 가상 스레드에서 ThreadLocal에 큰 객체를 담으면 수가 많아 메모리를 크게 쓴다.

## example

```yaml
# application.yml (Spring Boot 3.2+, Java 21)
spring:
  threads:
    virtual:
      enabled: true
```

```java
// 직접 사용
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<String>> futures = urls.stream()
            .map(url -> executor.submit(() -> httpGet(url)))   // 요청마다 가상 스레드
            .toList();
    for (Future<String> f : futures) {
        System.out.println(f.get().length());
    }
}
```
