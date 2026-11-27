---
name: @Configuration과 @Bean
category: spring
difficulty: JUNIOR
reference: https://docs.spring.io/spring-framework/reference/core/beans/java.html
---

## summary

@Configuration 클래스 안의 @Bean 메서드는 반환한 객체를 빈으로 등록한다. 직접 만든 클래스가 아니라 라이브러리 객체를 빈으로 쓰거나 생성 과정을 코드로 제어할 때 쓴다.

## body

## 핵심
- 컴포넌트 스캔은 내가 만든 클래스에, `@Bean`은 외부 라이브러리 객체(예: `PasswordEncoder`, `RestClient`)에 주로 쓴다.
- 메서드 이름이 기본 빈 이름이 된다.
- `@Configuration` 클래스에서 다른 `@Bean` 메서드를 호출해도 같은 싱글톤 빈이 반환된다.

## 주의
같은 타입 빈이 둘 이상이면 주입할 때 모호해진다. `@Primary`나 `@Qualifier`로 고른다.

## example

```java
@Configuration
public class SecurityBeans {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```
