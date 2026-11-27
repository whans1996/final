---
name: resultMap과 연관 매핑
category: mybatis
difficulty: JUNIOR
reference: https://mybatis.org/mybatis-3/ko/sqlmap-xml.html#Result_Maps
---

## summary

resultMap은 조회 결과 컬럼을 객체 필드에 어떻게 넣을지 직접 정의하는 MyBatis 설정이다. 조인 결과를 객체 안의 객체(association)나 목록(collection)으로 묶을 때 쓴다.

## body

## 핵심
- `<id>`는 행을 구분하는 키다. collection 매핑에서 같은 부모 행을 하나로 묶는 기준이 된다.
- `<association>`: 1:1 (게시글 → 작성자), `<collection>`: 1:N (게시글 → 댓글 목록).
- 단순 이름 차이는 `map-underscore-to-camel-case`나 SQL 별칭으로 해결하고, 중첩 구조일 때 resultMap을 쓴다.

## 주의
- 조인한 테이블 컬럼 이름이 겹치면(`id`) 별칭으로 구분해야 값이 섞이지 않는다.
- `<id>`를 빠뜨리면 collection이 부모 행마다 따로 만들어질 수 있다.

## example

```xml
<resultMap id="postWithComments" type="PostDetail">
  <id property="id" column="post_id"/>
  <result property="title" column="title"/>
  <association property="author" javaType="Author">
    <id property="id" column="author_id"/>
    <result property="nickname" column="author_nickname"/>
  </association>
  <collection property="comments" ofType="Comment">
    <id property="id" column="comment_id"/>
    <result property="content" column="comment_content"/>
  </collection>
</resultMap>
```

```xml
<select id="findDetail" resultMap="postWithComments">
  SELECT p.id AS post_id, p.title,
         u.id AS author_id, u.nickname AS author_nickname,
         c.id AS comment_id, c.content AS comment_content
  FROM posts p
  JOIN users u ON u.id = p.user_id
  LEFT JOIN comments c ON c.post_id = p.id
  WHERE p.id = #{id}
</select>
```
