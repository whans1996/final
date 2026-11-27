---
name: 어노테이션 매퍼 (@Select·@Insert)
category: mybatis
difficulty: BEGINNER
reference: https://mybatis.org/mybatis-3/ko/java-api.html
---

## summary

MyBatis는 XML 대신 Mapper 인터페이스 메서드에 @Select·@Insert·@Update·@Delete로 SQL을 직접 적을 수도 있다. 짧은 쿼리는 파일 이동 없이 한눈에 보인다.

## body

## 핵심
- `@Select("SELECT ... WHERE id = #{id}")`처럼 쓴다. 파라미터 규칙(`#{}`)은 XML과 같다.
- `@Options(useGeneratedKeys = true, keyProperty = "id")`로 생성 키를 받는다.
- 텍스트 블록(`"""`)을 쓰면 여러 줄 SQL도 읽기 쉽다.
- 같은 Mapper 안에서 어노테이션 메서드와 XML 메서드를 섞어 쓸 수 있다(같은 메서드를 양쪽에 정의하면 오류).

## 언제 XML을 쓰나
- 동적 SQL(`<if>`, `<foreach>`), 긴 조인, resultMap 중첩 매핑은 XML이 낫다. 어노테이션 안의 `<script>` 동적 SQL은 읽기 어렵다.
- 팀 규칙을 정해 두는 것이 중요하다. 예: "한 줄 쿼리만 어노테이션, 나머지는 XML".

## 주의
문자열 안 SQL은 IDE 검사가 약해 오타가 실행 시점에 드러난다. 매퍼 테스트로 확인한다.

## example

```java
@Mapper
public interface FollowMapper {

    @Select("SELECT COUNT(*) FROM follows WHERE following_id = #{userId}")
    long countFollowers(long userId);

    @Insert("INSERT INTO follows (follower_id, following_id) VALUES (#{followerId}, #{followingId})")
    int insert(@Param("followerId") long followerId, @Param("followingId") long followingId);

    @Select("""
            SELECT u.id, u.nickname
            FROM follows f JOIN users u ON u.id = f.follower_id
            WHERE f.following_id = #{userId}
            ORDER BY f.created_at DESC
            """)
    List<FollowerRow> findFollowers(long userId);
}
```
