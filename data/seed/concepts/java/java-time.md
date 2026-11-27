---
name: 날짜와 시간 API (java.time)
category: java
difficulty: BEGINNER
reference: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/package-summary.html
---

## summary

java.time(Java 8+)은 LocalDate·LocalDateTime·Instant·Duration 같은 불변 타입으로 날짜와 시간을 다룬다. 예전 Date·Calendar의 문제(가변, 0부터 시작하는 월)를 해결했다.

## body

## 핵심
- `LocalDate`(날짜), `LocalDateTime`(날짜+시간, 시간대 없음), `ZonedDateTime`(시간대 포함), `Instant`(UTC 기준 시점).
- `Duration`(초·나노초 간격), `Period`(년·월·일 간격).
- 모든 타입이 불변이다. `plusDays(1)`은 새 객체를 돌려준다.
- 스프링·Jackson·MyBatis 모두 java.time을 바로 지원한다. MySQL `DATETIME` ↔ `LocalDateTime`, `DATE` ↔ `LocalDate`.

## 주의
- `LocalDateTime.now()`는 서버 시간대를 따른다. 서버·DB·JVM 시간대(예: Asia/Seoul)를 맞춰 두지 않으면 9시간 차이 버그가 난다.
- 테스트에서 현재 시각에 의존하면 결과가 날마다 달라진다. `Clock`을 주입받아 고정한다.
- `plusDays` 결과를 변수에 다시 담지 않으면 아무 일도 일어나지 않는다.

## example

```java
LocalDate today = LocalDate.now(clock);
LocalDate nextReview = today.plusDays(3);              // 새 객체
```

```java
boolean overdue = plan.dueDate().isBefore(today);
long days = ChronoUnit.DAYS.between(start, today);
```

```java
Duration latency = Duration.between(startedAt, Instant.now());
log.info("LLM 응답 {}ms", latency.toMillis());
```

```java
String label = createdAt.format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"));
```
