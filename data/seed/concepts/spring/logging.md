---
name: 로깅 (SLF4J·Logback)
category: spring
difficulty: BEGINNER
reference: https://docs.spring.io/spring-boot/reference/features/logging.html
---

## summary

스프링 부트는 SLF4J 인터페이스와 Logback 구현으로 로그를 남긴다. 로그 레벨(ERROR·WARN·INFO·DEBUG·TRACE)과 패키지별 설정으로 필요한 만큼만 출력한다.

## body

## 핵심
- `LoggerFactory.getLogger(클래스)`로 로거를 만든다. Lombok `@Slf4j`도 같은 일을 한다.
- 문자열 더하기 대신 `{}` 자리 표시자를 쓴다. 레벨이 꺼져 있으면 문자열을 만들지 않는다.
- 예외는 마지막 인자로 넘기면 스택 트레이스가 함께 찍힌다.
- `logging.level.패키지=debug`로 패키지별 레벨을 정한다.

## 최신 문법
- Boot 3.4부터 `logging.structured.format.console=ecs`(또는 logstash)로 JSON 구조화 로그를 설정 한 줄로 낸다. 로그 수집 도구에서 필드로 검색할 수 있다.

## 주의
- `System.out.println`은 레벨·시간·스레드 정보가 없고 끌 수 없다. 서버 코드에서는 로거를 쓴다.
- 비밀번호, 토큰, API 키, 개인정보를 로그에 남기지 않는다.
- 반복문 안 INFO 로그는 로그 양을 폭발시킨다.

## example

```java
@Service
public class LoginService {
    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    public void login(String email) {
        log.info("로그인 시도 email={}", maskEmail(email));
        try {
            ...
        } catch (Exception e) {
            log.error("로그인 처리 실패 email={}", maskEmail(email), e);  // 예외는 마지막 인자
            throw e;
        }
    }
}
```

```yaml
# application.yml
logging:
  level:
    root: info
    com.vibenote: debug
    org.springframework.security: warn
```
