---
name: 생성 키 받기 (useGeneratedKeys)
category: mybatis
difficulty: BEGINNER
reference: https://mybatis.org/mybatis-3/ko/sqlmap-xml.html
---

## summary

AUTO_INCREMENT로 만든 새 행의 id는 INSERT 후에야 정해진다. MyBatis의 useGeneratedKeys와 keyProperty를 쓰면 INSERT 직후 파라미터 객체에 그 id가 채워진다.

## body

## 핵심
- `<insert useGeneratedKeys="true" keyProperty="id">`: DB가 만든 키를 파라미터 객체의 `id` 필드에 넣는다.
- Mapper 메서드의 반환값(int)은 **영향받은 행 수**이지 id가 아니다. id는 파라미터 객체에서 꺼낸다.
- 키를 받을 객체에는 값을 넣을 수 있는 필드(setter나 가변 필드)가 필요하다.

## 주의
- record는 불변이라 keyProperty로 값을 채울 수 없다. INSERT용 파라미터는 가변 클래스로 두거나 `SELECT LAST_INSERT_ID()`를 따로 조회한다.
- 반환값을 id로 착각해 쓰는 실수가 AI 코드에서 흔하다(항상 1이 나온다).
- 여러 행 INSERT에서도 키를 받을 수 있지만 DB·드라이버에 따라 동작이 다르므로 확인한다.

## example

```java
public class PostInsert {            // 키를 받으려면 가변 객체
    private Long id;
    private final long userId;
    private final String title;
    // 생성자, getter, setId 생략
}
```

```xml
<insert id="insert" useGeneratedKeys="true" keyProperty="id">
  INSERT INTO posts (user_id, title) VALUES (#{userId}, #{title})
</insert>
```

```java
PostInsert param = new PostInsert(userId, req.title());
int rows = postMapper.insert(param);   // rows = 1 (행 수)
long newId = param.getId();            // 생성된 id
```
