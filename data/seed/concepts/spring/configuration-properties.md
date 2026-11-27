---
name: 외부 설정과 @ConfigurationProperties
category: spring
difficulty: JUNIOR
reference: https://docs.spring.io/spring-boot/reference/features/external-config.html
---

## summary

스프링 부트는 application.yml, 환경변수, 실행 인자에서 설정값을 읽는다. @ConfigurationProperties는 관련 설정을 타입이 있는 객체 하나로 묶어 준다.

## body

## 핵심
- 우선순위가 있다. 실행 인자와 환경변수가 설정 파일보다 우선하므로, 운영 값은 환경변수로 덮어쓴다.
- `@Value("${...}")`는 값 하나, `@ConfigurationProperties(prefix = "...")`는 묶음에 적합하다.
- record로 선언하면 불변 설정 객체가 된다.

## 주의
비밀 값(API 키, DB 비밀번호)은 설정 파일에 쓰지 말고 환경변수로 받는다. 저장소에 커밋되면 되돌리기 어렵다.

## example

```java
@ConfigurationProperties(prefix = "app.upload")
public record UploadProperties(String dir, long maxBytes) { }
```

```java
@SpringBootApplication
@ConfigurationPropertiesScan
public class App { }
```

```yaml
# application.yml
app:
  upload:
    dir: ./uploads
    max-bytes: 2097152
```
