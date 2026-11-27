---
name: 인터페이스와 다형성
category: java
difficulty: BEGINNER
reference: https://docs.oracle.com/javase/tutorial/java/IandI/createinterface.html
---

## summary

인터페이스는 "무엇을 할 수 있는가"만 약속하고, 실제 동작은 구현 클래스가 정한다. 같은 타입으로 여러 구현을 바꿔 끼울 수 있는 성질이 다형성이다.

## body

## 핵심
- 인터페이스는 메서드 시그니처(약속)만 정의하고, 클래스가 `implements`로 그 약속을 지킨다.
- 호출하는 쪽은 인터페이스 타입만 알면 되므로 구현을 바꿔도 코드를 고치지 않는다.
- Java 8부터 `default` 메서드로 구현을 가진 메서드를, `static` 메서드로 도우미를 둘 수 있다. 인터페이스에 메서드를 추가해도 기존 구현 클래스가 깨지지 않는다.

## AI 코드에서 보이는 모습
- `UserService` 인터페이스와 `UserServiceImpl` 구현을 나누는 패턴이 흔하다. 구현이 하나뿐이면 나누지 않는 팀도 많다. 테스트용 가짜 구현이나 교체 가능성(Mock·실제 LLM 클라이언트처럼)이 있을 때 의미가 생긴다.
- 스프링은 같은 인터페이스의 구현 빈을 `List<Notifier>`로 모두 주입받을 수 있다. 전략 패턴을 if 분기 없이 만들 때 쓴다.

## 주의
인터페이스를 만들었다고 다형성이 생기는 것은 아니다. 호출하는 쪽이 구현 클래스 타입을 직접 쓰면 이점이 사라진다.

## example

```java
public interface Notifier {
    void send(String to, String message);

    default boolean supports(String channel) {   // Java 8+ 기본 구현
        return false;
    }
}
```

```java
@Component
public class EmailNotifier implements Notifier {
    public void send(String to, String message) { /* 메일 발송 */ }

    @Override
    public boolean supports(String channel) { return "EMAIL".equals(channel); }
}
```

```java
@Service
public class NotificationService {
    private final List<Notifier> notifiers;      // 구현 빈이 모두 주입된다

    public NotificationService(List<Notifier> notifiers) {
        this.notifiers = notifiers;
    }

    public void notify(String channel, String to, String message) {
        notifiers.stream()
                .filter(n -> n.supports(channel))
                .findFirst()
                .orElseThrow()
                .send(to, message);
    }
}
```
