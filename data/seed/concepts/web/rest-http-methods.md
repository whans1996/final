---
name: REST API와 HTTP 메서드
category: web
difficulty: BEGINNER
reference: https://developer.mozilla.org/ko/docs/Web/HTTP/Reference/Methods
---

## summary

REST API는 자원을 URL로 표현하고, 그 자원에 대한 행동을 HTTP 메서드(GET·POST·PUT·PATCH·DELETE)로 나타내는 설계 방식이다.

## body

## 핵심
- URL은 명사(자원), 행동은 메서드로 표현한다. `POST /api/posts`(생성), `GET /api/posts/3`(조회), `DELETE /api/posts/3`(삭제).
- GET은 데이터를 바꾸지 않아야 한다.
- PUT은 전체 교체, PATCH는 일부 수정에 쓴다.
- 같은 요청을 여러 번 보내도 결과가 같은 성질(멱등성): GET·PUT·DELETE는 멱등, POST는 아니다.

## 주의
`GET /api/deletePost?id=3`처럼 동사를 URL에 넣는 설계는 캐시·크롤러가 데이터를 지우는 사고로 이어질 수 있다.

## example

```text
GET    /api/posts?page=1        목록
GET    /api/posts/42            상세
POST   /api/posts               생성 (201 + Location)
PATCH  /api/posts/42            일부 수정
DELETE /api/posts/42            삭제 (204)
POST   /api/posts/42/likes      하위 자원 생성 (좋아요)
```
