---
name: 애플리케이션 이벤트 (@TransactionalEventListener)
category: spring
difficulty: SENIOR
reference: https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html
---

## summary

스프링 이벤트는 "어떤 일이 일어났다"를 발행하고 관심 있는 리스너가 받아 처리하게 해 서비스 간 직접 호출을 줄인다. @TransactionalEventListener는 트랜잭션이 커밋된 뒤에만 리스너를 실행한다.

## body

## 핵심
- `ApplicationEventPublisher.publishEvent(이벤트)`로 발행한다. 이벤트는 보통 record로 만든다.
- `@EventListener`: 발행 즉시 같은 스레드·같은 트랜잭션 안에서 실행된다.
- `@TransactionalEventListener`: 기본 단계가 `AFTER_COMMIT`이다. 롤백되면 실행되지 않으므로 "저장이 확정된 뒤 알림·비동기 작업 시작"에 맞다.
- `@Async`와 함께 쓰면 커밋 후 별도 스레드에서 처리한다.

## AI 코드에서 보이는 모습
가입 후 환영 메일, 글 작성 후 활동 피드 기록, 분석 접수 후 파이프라인 시작처럼 "주 작업 + 부가 작업"을 나눌 때 쓴다.

## 주의
- 트랜잭션 없이 발행하면 `@TransactionalEventListener`는 실행되지 않는다(`fallbackExecution = true`로 바꿀 수 있다).
- `AFTER_COMMIT` 리스너 안에서 DB에 쓰려면 이미 끝난 트랜잭션에 참여할 수 없으므로 `REQUIRES_NEW` 트랜잭션을 연다.
- 이벤트 흐름이 많아지면 코드만 보고 실행 순서를 추적하기 어렵다. 꼭 필요한 곳에만 쓴다.

## example

```java
public record AnalysisRequested(long analysisId) { }
```

```java
@Service
public class AnalysisService {

    @Transactional
    public long request(AnalysisRequest req, long userId) {
        Analysis analysis = Analysis.of(req, userId);
        analysisMapper.insert(analysis);  // useGeneratedKeys로 id가 채워진다
        events.publishEvent(new AnalysisRequested(analysis.getId()));
        return analysis.getId();          // 커밋되면 리스너가 실행된다
    }
}
```

```java
@Component
public class AnalysisStarter {

    @Async
    @TransactionalEventListener      // 기본 AFTER_COMMIT
    public void on(AnalysisRequested event) {
        worker.run(event.analysisId());
    }
}
```
