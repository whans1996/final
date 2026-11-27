---
name: 페이징 쿼리 (LIMIT·OFFSET과 COUNT)
category: mybatis
difficulty: JUNIOR
reference: https://mybatis.org/mybatis-3/ko/sqlmap-xml.html
---

## summary

목록 API는 한 번에 일부만 돌려준다. MySQL에서는 LIMIT·OFFSET으로 한 페이지를 가져오고, 같은 조건의 COUNT(*)로 전체 개수를 구해 페이지 정보를 만든다.

## body

## 핵심
- `OFFSET = (page - 1) * size`. 페이지 번호 계산은 Java(DTO)에서 하고 SQL에는 값만 넘긴다.
- 목록 쿼리와 COUNT 쿼리는 **같은 WHERE 조건**을 써야 한다. `<sql>` 조각으로 공유하면 어긋나지 않는다.
- 정렬(`ORDER BY`)이 없으면 페이지마다 순서가 달라져 중복·누락이 생긴다. 고유 키(id)를 마지막 정렬 기준으로 넣는다.
- size에 상한(예: 100)을 둔다.

## 주의
- OFFSET이 크면(뒤쪽 페이지) DB가 앞의 행을 모두 읽고 버리므로 느려진다. 무한 스크롤처럼 끝없이 넘기는 화면은 "마지막으로 본 id 다음부터"(커서 방식)를 쓴다.
- 정렬 컬럼을 사용자 입력으로 받을 때 `${}`를 쓰지 말고 `<choose>`나 enum 화이트리스트로 고른다.

## example

```xml
<sql id="searchWhere">
  <where>
    status = 'PUBLISHED'
    <if test="category != null">AND category = #{category}</if>
  </where>
</sql>
```

```xml
<select id="findPage" resultType="Concept">
  SELECT id, name, category, difficulty FROM concepts
  <include refid="searchWhere"/>
  ORDER BY created_at DESC, id DESC
  LIMIT #{size} OFFSET #{offset}
</select>
```

```xml
<select id="count" resultType="long">
  SELECT COUNT(*) FROM concepts
  <include refid="searchWhere"/>
</select>
```

```java
// Service
long total = conceptMapper.count(query);
List<Concept> items = conceptMapper.findPage(query);   // query.offset() = (page - 1) * size
return new PageResponse<>(items, total, query.page(), query.size());
```
