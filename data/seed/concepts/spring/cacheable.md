---
name: 캐시 추상화 (@Cacheable)
category: spring
difficulty: JUNIOR
reference: https://docs.spring.io/spring-framework/reference/integration/cache.html
---

## summary

@Cacheable을 붙인 메서드는 같은 인자로 다시 호출되면 메서드를 실행하지 않고 저장해 둔 결과를 돌려준다. 자주 읽고 드물게 바뀌는 데이터의 조회 부담을 줄인다.

## body

## 핵심
- `@EnableCaching`으로 켠다. Boot는 캐시 라이브러리가 없으면 메모리 맵(`ConcurrentMapCache`)을 쓴다.
- `@Cacheable(cacheNames = "concepts", key = "#id")`: 결과를 저장하고 재사용한다.
- `@CacheEvict`: 데이터가 바뀔 때 캐시를 지운다. `@CachePut`: 항상 실행하고 결과로 캐시를 갱신한다.
- 운영에서는 만료 시간(TTL)을 지원하는 Caffeine이나 Redis를 붙인다.

## 주의
- 기본 메모리 캐시는 만료가 없어 계속 쌓인다.
- 수정·삭제 경로에서 `@CacheEvict`를 빠뜨리면 오래된 데이터가 계속 보인다.
- 프록시 방식이라 같은 클래스 내부 호출에는 캐시가 적용되지 않는다.
- 사용자별로 다른 결과(권한, 찜 여부)를 키 없이 캐시하면 다른 사람의 데이터가 보인다.

## example

```java
@Configuration
@EnableCaching
public class CacheConfig { }
```

```java
@Service
public class ConceptService {

    @Cacheable(cacheNames = "concept", key = "#id")
    public ConceptResponse get(long id) {
        return ConceptResponse.from(conceptMapper.findById(id));
    }

    @CacheEvict(cacheNames = "concept", key = "#id")
    @Transactional
    public void update(long id, ConceptUpdateRequest req) {
        conceptMapper.update(id, req);
    }
}
```
