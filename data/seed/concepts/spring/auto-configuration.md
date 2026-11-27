---
name: 스프링 부트 자동 구성
category: spring
difficulty: JUNIOR
reference: https://docs.spring.io/spring-boot/reference/using/auto-configuration.html
---

## summary

자동 구성(Auto-configuration)은 클래스패스에 있는 라이브러리와 설정값을 보고 필요한 빈을 스프링 부트가 알아서 등록해 주는 기능이다.

## body

## 핵심
- 예: MySQL 드라이버와 `spring.datasource.*` 설정이 있으면 `DataSource` 빈이 자동으로 생긴다.
- 자동 구성은 "내가 같은 빈을 직접 정의하지 않았을 때만" 동작한다(조건부 등록). 직접 정의하면 그쪽이 우선한다.
- starter 의존성(`spring-boot-starter-web` 등)이 필요한 라이브러리와 자동 구성을 함께 가져온다.

## Boot 3에서 바뀐 점
- 자동 구성 클래스는 `@AutoConfiguration`을 붙이고, `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 파일에 한 줄씩 등록한다.
- 예전 `spring.factories`의 `EnableAutoConfiguration` 키 방식은 Boot 3.0에서 제거됐다. 블로그·AI 예제가 이 방식을 쓰면 Boot 2 시절 코드다.
- Boot 2 라이브러리를 Boot 3에 넣으면 자동 구성이 조용히 동작하지 않을 수 있다. starter 버전이 Boot 3를 지원하는지 확인한다.

## 주의
무엇이 자동으로 등록됐는지 모르면 디버깅이 어렵다. `--debug`로 실행하면 조건 평가 보고서를 볼 수 있다.

## example

```yaml
# application.yml 만으로 DataSource와 MyBatis SqlSessionFactory가 자동 구성된다
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/app
    username: app
    password: change-me
mybatis:
  mapper-locations: classpath:mapper/**/*.xml
```
