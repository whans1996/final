---
name: N+1 쿼리 문제
category: mybatis
difficulty: SENIOR
reference: https://mybatis.org/mybatis-3/ko/sqlmap-xml.html
---

## summary

N+1 문제는 목록 1번 조회 후 각 항목의 연관 데이터를 항목마다 따로 조회해 쿼리가 N+1번 실행되는 성능 문제다. 데이터가 적을 땐 안 보이다가 늘어나면 급격히 느려진다.

## body

## 핵심
- MyBatis에서는 `<association select="...">`·`<collection select="...">`(중첩 select)나, 반복문 안에서 Mapper를 호출할 때 생긴다.
- 해결: 조인 한 번 + resultMap 중첩 매핑, 또는 ID 목록을 모아 `IN (...)`으로 한 번에 조회한 뒤 메모리에서 합친다.
- 지연 로딩은 쿼리 시점을 미룰 뿐 횟수를 줄이지 않는다.

## AI 코드에서 보이는 모습
"게시글 목록 + 작성자 닉네임"을 만들 때 for문 안에서 `userMapper.findById()`를 부르는 코드가 흔하다.

## 주의
조인으로 1:N을 가져오면 행이 부모 × 자식 수만큼 늘어나고 페이지네이션(LIMIT)이 자식 행 기준으로 잘린다. 목록 페이지에서는 IN 조회 방식이 안전하다.

## example

```java
// 문제: 게시글 20개면 쿼리 21번
List<Post> posts = postMapper.findPage(page);
for (Post post : posts) {
    post.setAuthor(userMapper.findById(post.getUserId()));
}
```

```java
// 개선: 쿼리 2번
List<Post> posts = postMapper.findPage(page);
List<Long> userIds = posts.stream().map(Post::getUserId).distinct().toList();
Map<Long, User> users = userMapper.findByIds(userIds).stream()
        .collect(Collectors.toMap(User::getId, u -> u));
posts.forEach(p -> p.setAuthor(users.get(p.getUserId())));
```
