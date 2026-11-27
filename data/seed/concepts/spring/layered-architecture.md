---
name: 계층형 구조 (Controller·Service·Mapper)
category: spring
difficulty: BEGINNER
reference: https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html#beans-stereotype-annotations
---

## summary

웹 애플리케이션을 요청 처리(Controller), 비즈니스 로직(Service), 데이터 접근(Mapper·Repository) 계층으로 나누는 구조다. 각 계층은 아래 계층만 호출한다.

## body

## 핵심
- Controller: 요청을 받고 검증한 뒤 Service를 호출하고 응답을 돌려준다. 로직을 두지 않는다.
- Service: 규칙과 트랜잭션을 담당한다.
- Mapper: SQL을 실행한다.

## AI 코드에서 보이는 모습
AI가 만든 코드는 Controller에서 바로 Mapper를 부르거나, Service에 HTTP 관련 코드가 섞이는 경우가 있다. 계층을 건너뛰면 트랜잭션과 테스트 경계가 흐려진다.

## 주의
DB 행 객체를 그대로 응답으로 내보내면 비밀번호 같은 필드가 노출될 수 있다. 응답 DTO로 바꿔서 보낸다.

## example

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable long id) {
        return userService.getUser(id);          // Controller -> Service
    }
}
```

```java
@Service
public class UserService {
    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public UserResponse getUser(long id) {
        User user = userMapper.findById(id);     // Service -> Mapper
        return UserResponse.from(user);
    }
}
```
