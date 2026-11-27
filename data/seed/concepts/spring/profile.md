---
name: 프로파일 (@Profile)
category: spring
difficulty: JUNIOR
reference: https://docs.spring.io/spring-framework/reference/core/beans/environment.html
---

## summary

프로파일은 실행 환경(local, test, prod 등)에 따라 다른 빈과 설정을 활성화하는 기능이다.

## body

## 핵심
- `@Profile("local")`이 붙은 빈은 그 프로파일이 활성일 때만 등록된다.
- 스프링 부트는 `application-{프로파일}.yml`을 함께 읽는다.
- `spring.profiles.active`(실행 인자, 환경변수)로 켠다.

## AI 코드에서 보이는 모습
개발용 Mock 구현과 운영용 실제 구현을 프로파일로 나누는 패턴이 많다.

## 주의
운영 설정 파일에 비밀번호나 API 키를 직접 쓰지 말고 환경변수로 받는다.

## example

```java
@Configuration
public class MailConfig {

    @Bean
    @Profile("local")
    public MailSender fakeMailSender() {
        return new LoggingMailSender();
    }

    @Bean
    @Profile("prod")
    public MailSender smtpMailSender() {
        return new SmtpMailSender();
    }
}
```
