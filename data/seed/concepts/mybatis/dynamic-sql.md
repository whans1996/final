---
name: 동적 SQL
category: mybatis
difficulty: JUNIOR
reference: https://mybatis.org/mybatis-3/ko/dynamic-sql.html
---

## summary

동적 SQL은 조건에 따라 SQL 일부를 넣거나 빼는 MyBatis 기능이다. <if>, <where>, <choose>, <foreach>, <set> 태그를 쓴다.

## body

## 핵심
- `<where>`는 안쪽 조건이 하나라도 있으면 WHERE를 붙이고, 맨 앞의 AND·OR를 정리한다.
- `<foreach>`는 `IN (...)` 목록이나 여러 행 INSERT에 쓴다.
- `<set>`은 수정할 필드만 골라 UPDATE할 때 끝의 쉼표를 정리한다.

## 주의
- `<foreach>`에 빈 목록이 들어오면 `IN ()`이 되어 SQL 오류가 난다. 호출 전에 빈 목록을 처리한다.
- 조건이 모두 빠지면 전체 조회·전체 수정이 될 수 있다. 특히 UPDATE·DELETE에서 조심한다.

## example

```xml
<select id="search" resultType="Concept">
  SELECT id, name, category, difficulty FROM concepts
  <where>
    status = 'PUBLISHED'
    <if test="keyword != null and keyword != ''">
      AND name LIKE CONCAT('%', #{keyword}, '%')
    </if>
    <if test="category != null">AND category = #{category}</if>
    <if test="ids != null and ids.size() > 0">
      AND id IN
      <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
    </if>
  </where>
</select>
```
