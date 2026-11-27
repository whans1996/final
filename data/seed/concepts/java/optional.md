---
name: Optional
category: java
difficulty: JUNIOR
reference: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Optional.html
---

## summary

Optional은 값이 있을 수도 없을 수도 있음을 타입으로 드러내는 컨테이너다. null 확인을 잊어 생기는 NullPointerException을 줄인다.

## body

## 핵심
- 반환 타입으로 쓰는 것이 원래 목적이다. "없을 수 있는 조회 결과"에 어울린다.
- `orElse`, `orElseThrow`, `map`, `ifPresent`로 값을 꺼내거나 변환한다.

## 주의
- `get()`을 바로 부르면 null 확인을 안 한 것과 같다. `orElseThrow()`를 쓴다.
- 필드나 메서드 파라미터 타입으로는 권장되지 않는다.
- `orElse(무거운호출())`은 값이 있어도 호출된다. 지연이 필요하면 `orElseGet`을 쓴다.

## example

```java
public Optional<User> findByEmail(String email) {
    return Optional.ofNullable(userMapper.findByEmail(email));
}
```

```java
User user = findByEmail(email)
        .orElseThrow(() -> new NotFoundException("가입되지 않은 이메일"));
```
