---
name: Mapper 인터페이스와 XML 매퍼
category: mybatis
difficulty: BEGINNER
reference: https://mybatis.org/mybatis-3/ko/getting-started.html
---

## summary

MyBatis는 자바 인터페이스(Mapper)의 메서드와 XML에 적은 SQL을 연결한다. 메서드를 호출하면 연결된 SQL이 실행되고 결과가 객체로 매핑된다.

## body

## 핵심
- XML의 `namespace`는 Mapper 인터페이스의 전체 이름, 각 SQL의 `id`는 메서드 이름과 같아야 한다.
- 파라미터는 `#{이름}`으로 받는다. 파라미터가 여러 개면 `@Param`으로 이름을 붙인다.
- `resultType`은 결과를 담을 클래스다. `map-underscore-to-camel-case`를 켜면 `created_at` 컬럼이 `createdAt` 필드로 들어간다.

## 주의
namespace나 id 오타는 실행 시점에 `Invalid bound statement` 오류로 드러난다.

## example

```java
@Mapper
public interface PostMapper {
    Post findById(long id);
    List<Post> findByUser(@Param("userId") long userId, @Param("size") int size);
}
```

```xml
<!-- resources/mapper/post/PostMapper.xml -->
<mapper namespace="com.example.post.PostMapper">
  <select id="findById" resultType="com.example.post.domain.Post">
    SELECT id, user_id, title, created_at FROM posts WHERE id = #{id}
  </select>
</mapper>
```
