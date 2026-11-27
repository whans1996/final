---
name: 빈 스코프
category: spring
difficulty: JUNIOR
reference: https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html
---

## summary

빈 스코프는 빈 인스턴스가 얼마나 오래, 몇 개 존재하는지를 정한다. 기본은 싱글톤이고, 필요하면 prototype·request·session 스코프를 쓴다.

## body

## 핵심
- singleton: 컨테이너당 하나. 대부분의 서비스·매퍼가 여기에 해당한다.
- prototype: 요청할 때마다 새로 만든다. 컨테이너는 만든 뒤 소멸을 관리하지 않는다.
- request·session: 웹 요청·HTTP 세션마다 하나. 요청 추적 ID처럼 요청 단위 정보를 담을 때 쓴다.

## 실무에서
- 대부분은 싱글톤 + 상태 없는(stateless) 빈으로 충분하다. 다른 스코프가 필요해 보이면 먼저 "메서드 파라미터로 넘기면 되지 않나"를 생각한다.
- 싱글톤 빈이 더 짧은 스코프(request·prototype) 빈을 쓰려면 `ObjectProvider`로 꺼내거나 스코프 프록시(`proxyMode`)를 쓴다.

## 주의
- 싱글톤 빈이 prototype 빈을 생성자로 주입받으면 처음 한 번만 만들어진다.
- 싱글톤 빈에 변경 가능한 필드를 두면 요청끼리 값이 섞인다(동시성 문제).

## example

```java
@Component
@RequestScope                        // 요청마다 새 인스턴스, 싱글톤에는 프록시가 주입된다
public class RequestContext {
    private final String traceId = UUID.randomUUID().toString();
    public String traceId() { return traceId; }
}
```

```java
@Service
public class AnalysisService {
    private final RequestContext requestContext;

    public AnalysisService(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    public void request() {
        log.info("[{}] 분석 요청", requestContext.traceId());
    }
}
```
