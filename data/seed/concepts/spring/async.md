---
name: @Async 비동기 실행
category: spring
difficulty: JUNIOR
reference: https://docs.spring.io/spring-framework/reference/integration/scheduling.html
---

## summary

@Async를 붙인 메서드는 호출한 스레드가 아니라 별도 스레드 풀에서 실행된다. 오래 걸리는 작업(분석, 메일 발송)을 요청 처리와 분리해 응답을 먼저 돌려줄 때 쓴다.

## body

## 핵심
- 설정 클래스에 `@EnableAsync`를 붙인다.
- 반환 타입은 `void` 또는 `CompletableFuture<T>`.
- Boot는 `ThreadPoolTaskExecutor`를 자동 구성한다. `spring.task.execution.pool.*`로 크기를 조정한다.
- "요청 접수 → 202 응답 → 백그라운드 처리 → 상태를 DB에 기록 → 클라이언트가 조회" 패턴과 잘 맞는다.

## 주의
- 프록시 방식이라 **같은 클래스 안에서 호출하면 비동기로 실행되지 않는다.** 다른 빈에서 호출한다.
- `void` 메서드의 예외는 호출자에게 전달되지 않는다. 메서드 안에서 잡아 실패 상태를 기록하거나 `AsyncUncaughtExceptionHandler`를 둔다.
- 호출한 스레드의 트랜잭션과 `SecurityContext`는 이어지지 않는다. 필요한 값(사용자 ID 등)은 파라미터로 넘긴다.
- 커밋 전에 비동기 작업이 시작되면 아직 저장되지 않은 데이터를 읽을 수 있다. 커밋 후에 시작하려면 이벤트(`@TransactionalEventListener`)와 함께 쓴다.

## example

```java
@Configuration
@EnableAsync
public class AsyncConfig { }
```

```java
@Service
public class AnalysisWorker {

    @Async
    public void run(long analysisId) {
        try {
            pipeline.execute(analysisId);
            statusService.markDone(analysisId);
        } catch (Exception e) {
            log.error("분석 실패 id={}", analysisId, e);
            statusService.markFailed(analysisId, e.getMessage());
        }
    }
}
```

```java
// Controller → Service가 저장 후 다른 빈의 @Async 메서드 호출 → 즉시 202 응답
```
