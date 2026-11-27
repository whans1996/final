---
name: AOP (관점 지향 프로그래밍)
category: spring
difficulty: SENIOR
reference: https://docs.spring.io/spring-framework/reference/core/aop.html
---

## summary

AOP는 로깅·트랜잭션·권한 확인처럼 여러 곳에 반복되는 부가 기능을 핵심 로직과 분리해 한곳에서 적용하는 기법이다. 스프링은 프록시 객체로 이를 구현한다.

## body

## 핵심
- Aspect(부가 기능 모듈), Advice(언제 무엇을 할지: `@Before`, `@Around` 등), Pointcut(어디에 적용할지)으로 구성된다.
- 스프링 AOP는 빈을 프록시로 감싸 메서드 호출 앞뒤에 끼어든다. `@Transactional`도 이 방식이다.

## 주의
- 같은 클래스 안에서 자기 메서드를 호출(self-invocation)하면 프록시를 거치지 않아 AOP가 적용되지 않는다.
- private 메서드에는 적용되지 않는다.
- 너무 넓은 Pointcut은 어디서 무엇이 실행되는지 추적하기 어렵게 만든다.

## example

```java
@Aspect
@Component
public class TimingAspect {

    @Around("execution(* com.example..service.*.*(..))")
    public Object measure(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();
        } finally {
            long ms = System.currentTimeMillis() - start;
            System.out.println(pjp.getSignature() + " " + ms + "ms");
        }
    }
}
```
