---
name: @Transactional 선언적 트랜잭션
category: spring
difficulty: JUNIOR
reference: https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative.html
---

## summary

@Transactional을 메서드나 클래스에 붙이면 스프링이 메서드 시작 시 트랜잭션을 열고, 정상 종료 시 커밋, 예외 시 롤백한다.

## body

## 핵심
- 여러 SQL이 "모두 성공하거나 모두 실패"해야 할 때 쓴다. 예: 주문 저장 + 재고 차감.
- 보통 Service 계층 메서드에 붙인다.
- 조회만 하는 메서드는 `@Transactional(readOnly = true)`로 의도를 드러낸다.

## 주의
- 기본 설정은 unchecked 예외(`RuntimeException`)에서만 롤백한다. checked 예외는 커밋된다.
- 프록시 방식이라 같은 클래스 내부 호출에는 적용되지 않는다.
- 트랜잭션 안에서 외부 API처럼 오래 걸리는 호출을 하면 그동안 DB 커넥션을 잡고 있게 된다.

## example

```java
@Service
public class OrderService {

    @Transactional
    public void placeOrder(OrderRequest req) {
        orderMapper.insert(req);
        stockMapper.decrease(req.productId(), req.quantity());
        // 여기서 예외가 나면 insert도 롤백된다
    }
}
```
