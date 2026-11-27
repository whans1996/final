---
name: IoC 컨테이너와 빈
category: spring
difficulty: BEGINNER
reference: https://docs.spring.io/spring-framework/reference/core/beans/introduction.html
---

## summary

스프링은 객체를 직접 new 하지 않고 컨테이너가 만들어 관리한다. 컨테이너가 관리하는 객체를 빈(Bean)이라 부르고, 객체 생성의 제어권이 개발자에서 프레임워크로 넘어간 것을 제어의 역전(IoC)이라 한다.

## body

## 핵심
- `ApplicationContext`가 빈을 만들고, 의존 관계를 연결하고, 생명주기를 관리한다.
- 기본적으로 빈은 애플리케이션에 하나만 만들어지는 싱글톤이다.

## AI 코드에서 보이는 모습
`@Service`, `@Repository`, `@Controller`, `@Configuration`의 `@Bean` 메서드가 모두 빈을 등록하는 방법이다.

## 주의
싱글톤 빈은 여러 요청이 동시에 쓴다. 빈 필드에 요청별 상태(현재 사용자 등)를 저장하면 요청끼리 섞인다.

## example

```java
@Service
public class GreetingService {
    public String greet(String name) {
        return "안녕하세요, " + name;
    }
}
```

```java
// 직접 new 하지 않고 컨테이너에서 받는다
@RestController
public class GreetingController {
    private final GreetingService greetingService;

    public GreetingController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }
}
```
