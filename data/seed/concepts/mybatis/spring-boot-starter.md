---
name: MyBatis 스프링 부트 설정
category: mybatis
difficulty: BEGINNER
reference: https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/
---

## summary

mybatis-spring-boot-starter는 DataSource를 보고 SqlSessionFactory를 자동으로 만들고, @Mapper 인터페이스를 빈으로 등록해 준다. 설정은 application.yml의 mybatis.* 항목으로 한다.

## body

## 핵심
- `mybatis.mapper-locations`: XML 매퍼 파일 위치.
- `mybatis.configuration.map-underscore-to-camel-case: true`: `user_id` → `userId` 자동 매핑.
- `mybatis.type-aliases-package`: resultType에 패키지 없이 클래스 이름만 쓸 수 있게 한다.
- `@Mapper`를 붙이거나 `@MapperScan`으로 패키지를 지정해 Mapper를 등록한다.

## 버전 맞추기
- Spring Boot 3(Java 17+)에는 `mybatis-spring-boot-starter` **3.x**를 쓴다. 2.x는 Boot 2용이라 Boot 3에서 자동 구성이 동작하지 않는다.
- AI가 오래된 예제의 버전(2.x)을 그대로 넣는 경우가 많다. 의존성 버전을 먼저 확인한다.

## 주의
XML 위치 설정이 틀리면 앱은 뜨지만 호출 시점에 `Invalid bound statement (not found)` 오류가 난다.

## example

```yaml
# application.yml
mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.example.domain
  configuration:
    map-underscore-to-camel-case: true
    default-fetch-size: 100
```
