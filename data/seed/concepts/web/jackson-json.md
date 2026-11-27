---
name: JSON 직렬화 (Jackson)
category: web
difficulty: JUNIOR
reference: https://docs.spring.io/spring-boot/reference/features/json.html
---

## summary

스프링 부트는 Jackson 라이브러리로 자바 객체와 JSON을 서로 변환한다. 요청 본문은 객체로(역직렬화), 반환값은 JSON으로(직렬화) 바뀐다.

## body

## 핵심
- 기본적으로 getter(record는 접근자) 이름이 JSON 필드 이름이 된다.
- 날짜(`LocalDateTime`)는 ISO-8601 문자열로 나간다.
- `@JsonProperty`, `@JsonIgnore`로 이름과 포함 여부를 조정한다.

## 주의
- 엔티티를 그대로 반환하면 원치 않는 필드(비밀번호 해시 등)까지 직렬화된다. 응답 DTO를 쓴다.
- 서로를 참조하는 객체는 무한 재귀로 직렬화가 실패할 수 있다.

## example

```java
public record UserResponse(
        long id,
        String nickname,
        @JsonProperty("joinedAt") LocalDateTime createdAt) { }
```

```java
// 응답: {"id":1,"nickname":"vibecoder","joinedAt":"2026-11-18T10:00:00"}
```
