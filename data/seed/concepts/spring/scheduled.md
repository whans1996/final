---
name: @Scheduled 스케줄링
category: spring
difficulty: JUNIOR
reference: https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-scheduled
---

## summary

@Scheduled를 붙인 메서드는 정해진 주기나 cron 표현식에 따라 자동으로 실행된다. 만료 데이터 정리, 집계, 알림 같은 배치 작업에 쓴다.

## body

## 핵심
- 설정 클래스에 `@EnableScheduling`을 붙인다.
- `fixedRate`: 시작 시점 기준 주기. `fixedDelay`: 이전 실행이 끝난 뒤 기다리는 시간. `cron`: 달력 기준.
- 스프링 cron은 **6자리**(초 분 시 일 월 요일)다. 리눅스 crontab(5자리)과 다르다.
- `zone = "Asia/Seoul"`로 시간대를 명시한다.

## 주의
- Boot 기본 스케줄러 스레드는 1개다. 작업 하나가 오래 걸리면 다른 작업이 밀린다. `spring.task.scheduling.pool.size`로 늘린다.
- 서버를 여러 대 띄우면 **모든 서버에서 실행된다.** 한 번만 실행해야 하면 DB 락 같은 분산 락이 필요하다.
- 예외가 나도 다음 주기에 다시 실행되지만 로그를 남기지 않으면 실패를 모른다.

## example

```java
@Configuration
@EnableScheduling
public class SchedulingConfig { }
```

```java
@Component
public class CleanupJob {

    // 매일 새벽 4시 (초 분 시 일 월 요일)
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void deleteExpiredTokens() {
        int deleted = refreshTokenMapper.deleteExpired(LocalDateTime.now());
        log.info("만료 토큰 {}건 삭제", deleted);
    }

    @Scheduled(fixedDelay = 60_000)   // 끝나고 1분 뒤 다시
    public void retryFailedNotifications() { ... }
}
```
