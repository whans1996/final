---
name: #{}와 ${}의 차이 (SQL 인젝션)
category: mybatis
difficulty: JUNIOR
reference: https://mybatis.org/mybatis-3/ko/sqlmap-xml.html
---

## summary

MyBatis에서 #{}는 PreparedStatement의 파라미터(?)로 안전하게 바인딩되고, ${}는 값을 SQL 문자열에 그대로 끼워 넣는다. 사용자 입력을 ${}로 넣으면 SQL 인젝션이 생긴다.

## body

## 핵심
- `#{keyword}` → `WHERE title LIKE ?` 로 바뀌고 값은 따로 전달된다. 입력에 따옴표가 있어도 SQL 구조가 바뀌지 않는다.
- `${column}` → 값이 SQL 텍스트 자체가 된다.
- `${}`가 필요한 경우는 정렬 컬럼명처럼 파라미터로 바인딩할 수 없는 자리뿐이다.

## AI 코드에서 보이는 모습
정렬·검색 기능을 만들 때 AI가 `ORDER BY ${sort}`나 `LIKE '%${keyword}%'`를 쓰는 경우가 있다. 리뷰에서 꼭 확인할 지점이다.

## 주의
`${}`를 써야 한다면 enum이나 화이트리스트로 검증된 값만 넘긴다. LIKE 검색은 `CONCAT('%', #{keyword}, '%')`로 쓴다.

## example

```xml
<!-- 위험: keyword에 따옴표와 OR 1=1을 넣으면 전체 조회 -->
<select id="searchUnsafe" resultType="Post">
  SELECT * FROM posts WHERE title LIKE '%${keyword}%'
</select>
```

```xml
<!-- 안전 -->
<select id="search" resultType="Post">
  SELECT * FROM posts
  WHERE title LIKE CONCAT('%', #{keyword}, '%')
  ORDER BY
  <choose>
    <when test="sort == 'POPULAR'">like_count DESC</when>
    <otherwise>created_at DESC</otherwise>
  </choose>
</select>
```
