---
name: 컴포넌트 스캔과 스테레오타입
category: spring
difficulty: BEGINNER
reference: https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html
---

## summary

컴포넌트 스캔은 지정한 패키지 아래에서 @Component 계열 어노테이션이 붙은 클래스를 찾아 자동으로 빈으로 등록하는 기능이다.

## body

## 핵심
- `@Component`와 그 특수화인 `@Service`, `@Repository`, `@Controller`(`@RestController`)를 스테레오타입 어노테이션이라 한다.
- 스프링 부트는 `@SpringBootApplication`이 붙은 클래스의 패키지와 하위 패키지를 스캔한다.
- 역할별 어노테이션은 의도를 드러내고, `@Repository`는 DB 예외 변환 같은 추가 기능도 받는다.

## 주의
메인 클래스보다 바깥 패키지에 둔 클래스는 스캔되지 않아 "빈을 찾을 수 없음" 오류가 난다.

## example

```java
package com.example.app;

@SpringBootApplication   // com.example.app 이하를 스캔
public class App { }
```

```java
package com.example.app.user;

@Service                 // 자동으로 빈 등록
public class UserService { }
```
