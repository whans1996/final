---
name: record
category: java
difficulty: JUNIOR
reference: https://docs.oracle.com/en/java/javase/17/language/records.html
---

## summary

record는 값을 담기만 하는 불변 클래스를 한 줄로 선언하는 문법(Java 16+)이다. 생성자, getter, equals·hashCode·toString을 자동으로 만든다.

## body

## 핵심
- `record Point(int x, int y) {}`만으로 필드, 생성자, 접근자 `x()`, `y()`가 생긴다. 접근자 이름에 `get`이 붙지 않는다.
- 필드는 final이라 만든 뒤 바꿀 수 없다.
- 요청·응답 DTO처럼 데이터를 나르는 객체에 잘 맞는다.

## 실무 패턴
- 요청 DTO: 컴포넌트에 Bean Validation 어노테이션(`@NotBlank` 등)을 단다. Jackson이 record를 바로 역직렬화한다.
- 응답 DTO: DB 행 객체에서 필요한 필드만 골라 담는 정적 팩토리 `from()`을 둔다.
- 값 객체·메서드 반환 묶음(예: 결과 + 개수)에도 쓴다.

## 주의
- setter가 없으므로 값을 바꾸려면 새 객체를 만든다.
- 필드가 final이어도 안에 담긴 `List`는 바뀔 수 있다. 완전히 불변으로 만들려면 compact 생성자에서 `List.copyOf`로 복사한다.
- Lombok `@Data`·`@Getter` DTO와 섞여 있으면 접근자 이름(`name()`과 `getName()`)이 달라 매퍼·템플릿에서 헷갈린다.

## example

```java
public record SignupRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(max = 20) String nickname) { }
```

```java
public record UserResponse(long id, String nickname, List<String> roles) {
    public UserResponse {
        roles = List.copyOf(roles);              // 방어적 복사로 완전 불변
    }

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getNickname(), user.getRoles());
    }
}
```

```java
UserResponse res = UserResponse.from(user);
res.nickname();   // get 없이 필드 이름으로 읽는다
```
