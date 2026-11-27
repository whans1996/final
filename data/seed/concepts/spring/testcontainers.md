---
name: Testcontainers 통합 테스트
category: spring
difficulty: JUNIOR
reference: https://docs.spring.io/spring-boot/reference/testing/testcontainers.html
---

## summary

Testcontainers는 테스트를 실행할 때 Docker로 실제 DB(MySQL 등)를 띄워 준다. H2 같은 대체 DB가 아니라 운영과 같은 DB로 SQL을 검증한다.

## body

## 핵심
- Docker가 실행 중이어야 한다.
- Boot 3.1부터 `@ServiceConnection`을 붙이면 컨테이너 주소·계정이 데이터소스 설정에 자동으로 연결된다. 예전에는 `@DynamicPropertySource`로 직접 넣었다.
- 컨테이너를 `static`으로 두면 테스트 클래스 안에서 한 번만 띄운다. 여러 클래스가 공유하는 싱글톤 컨테이너로 만들면 더 빠르다.
- Flyway 마이그레이션도 테스트 시작 시 그대로 적용된다.

## 주의
- H2로 테스트하면 MySQL 전용 문법(ENUM, JSON, ON DUPLICATE KEY)에서 운영과 다르게 동작할 수 있다.
- 컨테이너 이미지는 운영 DB 버전과 맞춘다(`mysql:8.4`).
- 처음 실행할 때 이미지를 내려받아 오래 걸린다. CI에서는 캐시를 고려한다.

## example

```java
@SpringBootTest
@Testcontainers
class ConceptMapperTest {

    @Container
    @ServiceConnection                       // Boot 3.1+: 데이터소스 자동 연결
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired ConceptMapper conceptMapper;

    @Test
    void 공개된_카드만_검색한다() {
        List<Concept> result = conceptMapper.search("트랜잭션", null);
        assertThat(result).allMatch(c -> c.status() == Status.PUBLISHED);
    }
}
```
