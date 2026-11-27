---
name: RestClient (외부 API 호출)
category: spring
difficulty: JUNIOR
reference: https://docs.spring.io/spring-framework/reference/integration/rest-clients.html
---

## summary

RestClient는 Spring 6.1(Boot 3.2)에 추가된 동기 HTTP 클라이언트다. 메서드를 이어 붙이는 방식으로 외부 API를 호출하고 응답을 객체로 받는다. 인터페이스만 선언하는 HTTP 인터페이스(@HttpExchange)와 함께 쓸 수 있다.

## body

## 핵심
- `restClient.get().uri(...).retrieve().body(타입)` 순서로 쓴다.
- 4xx·5xx 응답은 기본적으로 예외(`RestClientResponseException`)가 된다. `onStatus`로 상태별 처리를 바꾼다.
- Boot는 `RestClient.Builder` 빈을 제공한다. 주입받아 `baseUrl`, 기본 헤더를 정한 뒤 `build()`한다.
- HTTP 인터페이스: `@GetExchange("/users/{id}")`처럼 메서드만 선언하면 구현이 자동으로 만들어진다.

## 최신 문법
- `RestTemplate`은 유지 보수 모드다. 새 코드는 RestClient를 쓴다. `WebClient`는 리액티브(WebFlux) 스택용이다.

## 주의
- **타임아웃을 꼭 설정한다.** 외부 서버가 응답하지 않으면 요청 스레드가 계속 묶인다.
- 외부 호출을 `@Transactional` 안에서 하면 응답을 기다리는 동안 DB 커넥션을 잡는다. 호출과 저장을 나눈다.
- API 키는 환경변수로 받고 로그에 찍지 않는다.

## example

```java
@Configuration
public class GithubClientConfig {

    @Bean
    public RestClient githubRestClient(RestClient.Builder builder) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return builder
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .requestFactory(factory)
                .build();
    }
}
```

```java
Repo repo = githubRestClient.get()
        .uri("/repos/{owner}/{name}", owner, name)
        .retrieve()
        .onStatus(status -> status.value() == 404, (req, res) -> {
            throw new BusinessException(ErrorCode.REPO_NOT_FOUND);
        })
        .body(Repo.class);
```

```java
// HTTP 인터페이스
public interface GithubApi {
    @GetExchange("/repos/{owner}/{name}")
    Repo getRepo(@PathVariable String owner, @PathVariable String name);
}
```

```java
GithubApi api = HttpServiceProxyFactory
        .builderFor(RestClientAdapter.create(githubRestClient))
        .build()
        .createClient(GithubApi.class);
```
