---
name: 의존성 주입 (DI)
category: spring
difficulty: BEGINNER
reference: https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html
---

## summary

의존성 주입은 객체가 필요한 다른 객체를 스스로 만들지 않고 외부(스프링 컨테이너)로부터 받는 방식이다. 생성자 주입이 권장된다.

## body

## 핵심
- 생성자 주입: 필드를 `final`로 둘 수 있고, 필요한 의존성이 빠지면 객체가 만들어지지 않는다.
- 생성자가 하나면 `@Autowired`를 생략해도 된다.
- 테스트에서 가짜 객체를 생성자로 넣기 쉽다.

## AI 코드에서 보이는 모습
Lombok `@RequiredArgsConstructor`와 `private final` 필드 조합이 자주 보인다. 이것도 생성자 주입이다.

## 주의
필드에 `@Autowired`를 붙이는 필드 주입은 테스트와 불변성에 불리하다.

## example

```java
@Service
public class OrderService {
    private final OrderMapper orderMapper;
    private final PaymentClient paymentClient;

    public OrderService(OrderMapper orderMapper, PaymentClient paymentClient) {
        this.orderMapper = orderMapper;
        this.paymentClient = paymentClient;
    }
}
```
