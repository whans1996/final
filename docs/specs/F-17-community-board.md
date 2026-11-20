# F-17 커뮤니티 게시판

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | F-17 |
| 우선순위 | P0 |
| 영역 오너 | B 김준섭 (카드·소셜) |
| 관련 필수 요구사항 | ③ 커뮤니티 게시판 (명세서 "추가 · 커뮤니티: 여러 게시판 CRUD") |
| 의존 spec | F-02 (로그인), F-09·F-10 (카드 검색 `GET /api/concepts`·카드 상세 화면, develop에 머지됨), F-05a (내 분석 목록 `GET /api/analyses`). 활동 기록은 F-00b의 `ActivityService.record`, 탈퇴 회원 차단은 `UserService.getActiveMember`(F-11과 같음) |
| 관련 이슈 | 없음 |
| 승인 | 김준섭 2026-11-20 |

## 1. 목적

AI로 코딩하는 학습자가 **코드 이해 Q&A·학습 후기·자유** 게시판에 글을 쓰고, 다른 사람의 글을 게시판별로 보거나 검색한다.
글에는 **내 분석 결과나 개념 카드를 하나** 첨부할 수 있어서, "이 코드의 이 부분이 이해가 안 된다"는 질문을 맥락과 함께 남긴다 (11-17 결정 "코드 이해 Q&A + 분석·카드 첨부").
작성자는 자기 글을 수정·삭제한다. 글을 쓰면 팔로워 피드(F-04)에 `POST` 활동으로 남는다. 댓글은 F-18에서 다룬다.

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다: `AC1_비로그인으로_전체_게시글을_최신순_조회한다()`
> BE 테스트는 다른 테스트가 넣은 글에 기대지 않는다. 테스트마다 고유한 제목 접두어(예: `f17-ac2-`)로 글을 넣고 `keyword`로 좁혀 확인하거나, 회원·글을 직접 넣고 id로 확인한다.

### 백엔드

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | 세 게시판에 글이 25개 있을 때 | 누구나(비로그인) `GET /api/posts?page=1&size=20`을 요청하면 | 200과 페이지 응답 `{items, page:1, size:20, total:25}`를 받는다. `items`는 게시판 구분 없이 `created_at DESC, id DESC` 순서의 20개이고 항목은 3절 `PostSummaryResponse`다(본문은 담지 않는다). `commentCount`는 그 글의 `comments` 행 수(F-18 전에는 0), `attachmentType`은 `ANALYSIS`·`CONCEPT`·`null` 중 하나다. `page=2`면 나머지 5개, 범위를 넘은 page면 `items: []`·`total: 25`다. size를 주지 않으면 20이다 |
| AC2 | 게시판·제목·본문이 다른 글들이 있을 때 | `boardType=QNA`, `keyword=트랜잭션`, 둘을 함께 주면 | `boardType`은 그 게시판 글만, `keyword`는 **제목 또는 본문**에 부분 일치하는 글만(대소문자 무시) 받고, 둘을 함께 주면 둘 다 만족하는 글만 받는다. `keyword`는 앞뒤 공백을 빼고, 빈 문자열이면 조건이 없는 것으로 본다. `%`·`_`는 글자 그대로 찾는다. `boardType`이 `QNA`·`REVIEW`·`FREE`가 아니거나 `keyword`가 51자 이상이면 400 `COMMON_400`과 `details`의 해당 필드를 받는다 |
| AC3 | 작성자가 탈퇴한(`users.status = 'WITHDRAWN'`) 글이 있을 때 | 목록과 상세를 요청하면 | 그 글도 목록·`total`·상세에 그대로 나오고 `authorId`는 `null`, `authorNickname`은 `"탈퇴한 사용자"`다. 활성 회원의 글은 `authorId`와 현재 닉네임을 담는다 |
| AC4 | 글 P(조회수 7)가 있을 때 | 누구나 `GET /api/posts/{P}`를 두 번 요청하면 | 200과 3절 `PostDetailResponse`를 받고, 응답 `viewCount`가 8, 9로 오른다 (작성자 본인이 열어도 오른다). 없는 id(`999999`)면 404 `POST_404` "게시글을 찾을 수 없습니다."이고 아무 글의 조회수도 바뀌지 않는다 |
| AC5 | P에 분석 A 또는 개념 카드 C가 첨부되어 있을 때 | 상세를 요청하면 | 분석 첨부는 `analysis: {id, viewable, title, projectName}`으로 온다. A가 삭제되지 않았고(`deleted_at IS NULL`) **공개(`is_public`)이거나 요청한 사람이 A의 주인**이면 `viewable: true`와 제목·프로젝트명을, 아니면(비공개를 남이 볼 때, 비로그인, 삭제된 분석) `viewable: false`와 `title`·`projectName`이 `null`이다. 카드 첨부는 `concept: {id, viewable, name, category, difficulty, summary}`로 오고, C가 `PUBLISHED`가 아니면 `viewable: false`이고 나머지가 `null`이다. 첨부가 없으면 둘 다 `null`이다. 목록의 `attachmentType`은 `viewable`과 상관없이 첨부 종류를 알린다 |
| AC6 | 로그인 회원 U가 | `POST /api/posts` `{boardType:"QNA", title:"  @Transactional이 안 먹어요  ", content:"같은 클래스 안에서 ...", analysisId: A}`(A는 U의 DONE 분석)을 보내면 | 201과 `{id}`를 받는다. 저장된 `title`·`content`는 앞뒤 공백을 뺀 값이고 `view_count`는 0이다. 같은 트랜잭션에서 `activities`에 `(U, POST, 글 id)` 한 행이 생긴다. `conceptId`로 `PUBLISHED` 카드를 첨부해도 201이다. A가 비공개여도 첨부할 수 있다 (보이는 범위는 AC5) |
| AC7 | 로그인 회원이 | `boardType`이 없음·잘못된 값, `title`이 없음·공백만·101자, `content`가 없음·공백만·5001자(길이는 앞뒤 공백 제외)이거나 `analysisId`와 `conceptId`를 **둘 다** 보내면 | 400 `COMMON_400`과 `details`에 해당 필드(`boardType`·`title`·`content`·`conceptId`)와 사유를 받는다. 제목 100자·본문 5000자 정확히는 성공한다. 글과 활동은 생기지 않는다 |
| AC8 | 로그인 회원 U가 | `analysisId`로 남의 분석·DONE이 아닌 분석·삭제된 분석·없는 id를 보내면 / `conceptId`로 `DRAFT`·`HIDDEN` 카드나 없는 id를 보내면 | 각각 400 `POST_001` "첨부할 수 없는 분석입니다." / 400 `POST_002` "첨부할 수 없는 개념 카드입니다."를 받고 글은 생기지 않는다. 남의 분석이 있는지 없는지는 응답으로 구분되지 않는다 |
| AC9 | U가 쓴 글 P(조회수 7)가 있을 때 | U가 `PUT /api/posts/{P}`에 `{boardType:"FREE", title:"바뀐 제목", content:"바뀐 본문", conceptId: C}`를 보내면 | 200과 바뀐 `PostDetailResponse`를 받는다. 게시판·제목·본문·첨부를 요청 값으로 **통째로 바꾼다**(분석 첨부를 빼고 카드로 바꾸기, 첨부 없애기 포함). `id`·`createdAt`·`viewCount`(7)는 그대로이고 `updatedAt`이 바뀐다. 활동은 새로 생기지 않는다. 검증·첨부 규칙은 AC7·AC8과 같다 |
| AC10 | U가 쓴 글 P에 댓글이 있을 때 | U가 `DELETE /api/posts/{P}`를 보내면 | 204를 받고 글과 그 댓글이 지워진다(`comments` FK CASCADE). 이후 `GET /api/posts/{P}`는 404 `POST_404`다. `POST` 활동 행은 지우지 않는다 (F-04 피드가 `posts`와 JOIN해 거른다) |
| AC11 | V가 쓴 글 P가 있을 때 | U가 P를 `PUT`·`DELETE`하면 / 없는 id를 `PUT`·`DELETE`하면 | 403 `POST_403` "본인이 쓴 글만 수정·삭제할 수 있습니다." / 404 `POST_404`를 받고 P는 바뀌지 않는다. 403·404 판단은 검증(400)보다 먼저다(남의 글에 잘못된 본문을 보내도 403) |
| AC12 | 토큰 없음·만료·형식 오류이거나, 탈퇴한 회원의 남은 Access 토큰일 때 | 작성·수정·삭제를 요청하면 | 401 `AUTH_401`이고 아무것도 바뀌지 않는다. 목록·상세 조회는 토큰이 깨져 있어도 200이다 (공개 경로). 유효한 토큰으로 상세를 보면 AC5의 "분석 주인" 판단에 그 회원을 쓴다 |

### 프론트엔드

| ID | Given | When | Then |
|---|---|---|---|
| AC13 | 누구나 | SC-14 `/community`를 열면 | 헤더 메뉴에 "커뮤니티"(비로그인에도 보임)가 있고, 게시판 탭(전체 · 코드 이해 Q&A · 학습 후기 · 자유)과 최신순 글 20개가 보인다. 행은 게시판 칩(Q&A·후기·자유), 제목, 첨부 표시(📎 분석 / 🃏 카드), 댓글 수 `[n]`, 작성자 닉네임(`authorId`가 `null`이면 "탈퇴한 사용자"), 작성일(`MM/DD`), 조회수이고 누르면 SC-15로 간다. 탭을 누르면 즉시 1페이지부터 다시 조회한다. 탭·검색어·페이지는 URL 쿼리(`?boardType=&keyword=&page=`)에 남아 새로고침·뒤로 가기에 유지된다. 페이지 이동은 `PageNav`(이전·다음). 0건이면 "게시글이 없습니다"와 (검색어가 있으면) "검색 초기화" 버튼, 실패하면 서버 `message` |
| AC14 | SC-14에서 | 검색어를 입력해 Enter나 "검색"을 누르면 / "글쓰기"를 누르면 | 그 검색어로 1페이지부터 다시 조회한다(입력 중에는 조회하지 않는다). 검색창은 50자에서 더 입력되지 않는다. "글쓰기"는 SC-16 `/community/write`로 가고, 비로그인이면 `/login?redirect=/community/write`로 간다 |
| AC15 | 누구나 | SC-15 `/community/:id`를 열면 | 게시판 칩, 제목, 작성자(`/users/{authorId}` 링크, 탈퇴면 링크 없는 "탈퇴한 사용자"), 작성일(`YYYY-MM-DD HH:mm`), 조회수, 본문이 보인다. 본문은 **글자 그대로**(줄바꿈 유지, HTML이 태그로 해석되지 않음) 보인다. 첨부가 있으면 분석은 `viewable`일 때 제목·프로젝트명과 "분석 보기"(`/analyses/{id}`) 링크, 아니면 "볼 수 없는 분석입니다", 카드는 `viewable`일 때 이름·카테고리·난이도·요약을 담은 상자(누르면 `/concepts/{id}`), 아니면 "볼 수 없는 개념 카드입니다"가 보인다. "← 목록으로"는 SC-14로 간다. 404면 "게시글을 찾을 수 없습니다." + 목록으로 링크, 그 밖의 실패는 서버 `message` |
| AC16 | SC-15에서 | 로그인 회원이 자기 글(`authorId === auth.user.id`)을 열면 / 남의 글이나 비로그인이면 | 자기 글에만 "수정"(SC-16 `/community/:id/edit`)·"삭제"가 보인다. "삭제"는 확인 대화상자에서 확인하면 `deletePost` 후 SC-14로 가고, 취소하면 아무 요청도 보내지 않는다. 삭제가 403·404면 서버 `message`를 보여 준다 |
| AC17 | 로그인 회원이 | SC-16 `/community/write`에서 게시판(라디오, 기본 Q&A)·제목·본문을 입력하고 첨부를 골라 "등록"을 누르면 | `createPost` 후 응답 id의 SC-15로 간다. 제목이 공백뿐이거나 본문이 공백뿐이면 "등록"이 비활성이고, 제목은 100자·본문은 5000자에서 더 입력되지 않으며 본문 아래 "n/5000"이 보인다. 첨부는 "없음 / 내 분석 / 개념 카드" 중 하나만 고른다: 내 분석은 `fetchAnalyses({page:1, size:100})` 중 `status === 'DONE'`인 것을 선택 상자로, 카드는 검색어로 `fetchConcepts({keyword, size:10})` 결과에서 고른다. 고르면 미리보기 칩과 ✕(첨부 해제)가 보인다. 저장 중에는 버튼이 비활성이다. 400이면 `details`의 필드 메시지를 해당 필드 아래에(`conceptId`·`POST_001`·`POST_002`는 첨부 아래에), 그 밖의 실패는 서버 `message`를 보여 준다. 입력이 있는 채로 화면을 벗어나려 하면 "작성 중인 내용이 사라집니다" 확인을 띄우고, 등록 성공 뒤 이동에는 띄우지 않는다 |
| AC18 | SC-16·SC-10에서 | `/community/write?conceptId=C`·`?analysisId=A`로 들어오면 / SC-10에서 "이 카드로 글쓰기"를 누르면 / 자기 글의 `/community/:id/edit`를 열면 / 남의 글의 edit을 열면 | 쿼리의 카드(`fetchConcept`)·분석(`fetchAnalysis`)을 첨부로 미리 채운다(불러오지 못하면 첨부 없이 열고 "첨부할 수 없는 항목이라 빼고 열었습니다"를 보여 준다). SC-10 카드 상세에는 "이 카드로 글쓰기" 버튼이 있고 `/community/write?conceptId={id}`로 간다(비로그인은 로그인 후 돌아온다). edit은 `fetchPost` 값으로 폼을 채우고 버튼이 "저장"이며 `updatePost` 후 SC-15로 간다. 남의 글이면 "본인이 쓴 글만 수정할 수 있습니다."를 보여 주고 SC-15로 보낸다. SC-14~16 모두 375px 폭에서 가로 스크롤이 없다 (NF-02) |

## 3. API 계약

> 구현 후 `docs/api/openapi.json`과 일치해야 한다 (contract-checker, CI가 검사). Swagger Tag: `커뮤니티`. 새 패키지 `com.vibenote.post`: `PostController`(`@RequestMapping("/api/posts")`), `PostService`, `PostMapper`(+`mapper/post/PostMapper.xml`), `post/domain`, `post/dto`.

### `GET /api/posts?boardType=&keyword=&page=1&size=20`

- 권한: 공개. `SecurityConfig.PUBLIC_GET_PATHS`의 POST 블록에 `"/api/posts"`, `"/api/posts/{id:\\d+}"`를 추가한다 (공통 파일 → PR에서 @whans1996 멘션)
- 파라미터: `boardType`(선택, `QNA|REVIEW|FREE`), `keyword`(선택, trim 후 50자 이하, 제목+본문 부분 일치), 공통 `PageQuery`(page 1부터, size 기본 20·최대 100). 정렬은 최신순 고정
- 응답 `200`

```json
{
  "items": [
    {
      "id": 42,
      "boardType": "QNA",
      "title": "@Transactional이 안 먹는 이유가 뭔가요?",
      "attachmentType": "ANALYSIS",
      "commentCount": 3,
      "viewCount": 42,
      "authorId": 7,
      "authorNickname": "코딩초보",
      "createdAt": "2026-11-20T14:02:11+09:00"
    },
    {
      "id": 39,
      "boardType": "FREE",
      "title": "스터디 같이 하실 분",
      "attachmentType": null,
      "commentCount": 0,
      "viewCount": 7,
      "authorId": null,
      "authorNickname": "탈퇴한 사용자",
      "createdAt": "2026-11-18T09:30:00+09:00"
    }
  ],
  "page": 1,
  "size": 20,
  "total": 2
}
```

### `GET /api/posts/{postId}`

- 권한: 공개. 유효한 토큰이 있으면 분석 첨부의 `viewable` 판단에 쓴다 (AC5)
- 부수 효과: `view_count`를 1 올린 뒤 그 값을 응답한다 (`UPDATE posts SET view_count = view_count + 1` 후 조회, 같은 트랜잭션)
- 응답 `200` (`PostDetailResponse`)

```json
{
  "id": 42,
  "boardType": "QNA",
  "title": "@Transactional이 안 먹는 이유가 뭔가요?",
  "content": "같은 클래스 안에서 메서드를 호출하면 트랜잭션이 안 걸린다는데\n왜 그런지 모르겠어요.",
  "authorId": 7,
  "authorNickname": "코딩초보",
  "viewCount": 43,
  "commentCount": 3,
  "analysis": {
    "id": 15,
    "viewable": true,
    "title": "쇼핑몰 과제",
    "projectName": "shop-backend"
  },
  "concept": null,
  "createdAt": "2026-11-20T14:02:11+09:00",
  "updatedAt": "2026-11-20T14:02:11+09:00"
}
```

- 카드 첨부 예시: `"analysis": null, "concept": {"id": 12, "viewable": true, "name": "JWT", "category": "security", "difficulty": "JUNIOR", "summary": "서버가 서명한 토큰으로 로그인 상태를 전달하는 방식"}`
- 볼 수 없는 분석: `"analysis": {"id": 15, "viewable": false, "title": null, "projectName": null}`

### `POST /api/posts`

- 권한: 로그인 필요 (탈퇴 회원의 남은 토큰은 `getActiveMember`로 401)
- 요청 (`PostRequest`, `analysisId`·`conceptId`는 선택이고 둘 중 하나만)

```json
{
  "boardType": "QNA",
  "title": "@Transactional이 안 먹는 이유가 뭔가요?",
  "content": "같은 클래스 안에서 메서드를 호출하면 트랜잭션이 안 걸린다는데\n왜 그런지 모르겠어요.",
  "analysisId": 15,
  "conceptId": null
}
```

- 응답 `201`: `{ "id": 42 }`

### `PUT /api/posts/{postId}`

- 권한: 로그인 필요, 작성자 본인만
- 요청: `POST`와 같은 `PostRequest` (보낸 값으로 통째로 바꾼다. 첨부를 없애려면 둘 다 `null`)
- 응답 `200`: 바뀐 `PostDetailResponse` (조회수는 올리지 않는다)

### `DELETE /api/posts/{postId}`

- 권한: 로그인 필요, 작성자 본인만
- 응답 `204` (본문 없음)

### 에러 (위 엔드포인트 공통)

| 상태 | 코드 | 메시지 | 언제 |
|---|---|---|---|
| 400 | COMMON_400 | (검증) | `boardType`·`title`·`content` 검증 실패, 첨부 둘 다(`details.conceptId`), 목록의 `boardType`·`keyword`·page·size 범위 밖 |
| 400 | POST_001 | 첨부할 수 없는 분석입니다. | 남의 분석·DONE 아님·삭제됨·없는 id (구분하지 않는다) |
| 400 | POST_002 | 첨부할 수 없는 개념 카드입니다. | `PUBLISHED`가 아니거나 없는 카드 |
| 401 | AUTH_401 | (공통) | 작성·수정·삭제에 토큰 없음·만료·탈퇴 회원 |
| 403 | POST_403 | 본인이 쓴 글만 수정·삭제할 수 있습니다. | 남의 글 `PUT`·`DELETE` |
| 404 | POST_404 | 게시글을 찾을 수 없습니다. | 없는 글의 상세·수정·삭제 |

- 판단 순서(`PUT`·`DELETE`): 인증(401) → 글 존재(404) → 작성자(403) → 본문 검증(400) → 첨부(POST_001·002). 본문 검증이 Bean Validation이라 Controller에서 먼저 돌면 AC11을 깨므로, `PUT`의 형식 검증은 Service에서 작성자 확인 뒤에 한다 (구현 때 방법 선택: `@Validated` 그룹 또는 `Validator` 직접 호출)

## 4. DB 변경

- Flyway: 없음. V1의 `posts`(board_type ENUM, title VARCHAR(200), content TEXT, analysis_id·concept_id nullable FK, view_count)와 `comments`(post_id FK `ON DELETE CASCADE`)를 그대로 쓴다. 제목 100자·본문 5000자는 애플리케이션 검증이다
- 사용 테이블: `posts`(읽기·쓰기), `comments`(개수만), `users`(닉네임·status), `analyses`(첨부 검증·표시: `user_id`, `status`, `deleted_at`, `is_public`, `title`, `project_name`), `concepts`(첨부 검증·표시: `status`, `name`, `category`, `difficulty`, `summary`), `activities`(작성 때 `POST` 한 행)
- 검색: `(title LIKE ? ESCAPE '!' OR content LIKE ? ESCAPE '!')`, keyword 이스케이프는 F-09 `ConceptService`와 같은 방식(공용 유틸로 뺄지는 구현 때 판단, 뺀다면 공통 코드라 @멘션). 대소문자 무시는 collation(`utf8mb4_0900_ai_ci`)에 맡긴다. 글 수가 적어 전문 검색 인덱스는 두지 않는다
- 목록의 `commentCount`는 `comments`를 `post_id`로 묶은 개수(LEFT JOIN 서브쿼리). 탈퇴 표시는 `users.status = 'WITHDRAWN'`이면 `authorId`·닉네임을 Service가 바꾼다 (F-11과 같은 방식)
- 활동: 작성 때만 `ActivityService.record(userId, ActivityType.POST, postId)`. 수정 때는 기록하지 않는다. 글을 지워도 활동 행은 지우지 않는다 → **F-04 피드는 `posts`와 JOIN해 지워진 글의 활동을 걸러야 한다** (F-04 spec에 옮긴다)
- 분석이 soft delete되면 `posts.analysis_id`는 남고 `viewable: false`로 보인다. 카드가 실제로 지워지면 FK `ON DELETE SET NULL`로 첨부가 사라진다

## 5. 프론트엔드

| 항목 | 내용 |
|---|---|
| 화면/라우트 | `router/routes/community.js`: `/community` → `views/community/PostListView.vue`(SC-14, `meta.nav: {label:'커뮤니티', order: 6, show:'all'}`), `/community/:id(\\d+)` → `PostDetailView.vue`(SC-15), `/community/write`·`/community/:id(\\d+)/edit` → `PostWriteView.vue`(SC-16, `requiresAuth`) |
| 컴포넌트 | `components/community/`: `BoardTabs.vue`(탭·모바일 가로 스크롤 칩), `PostRow.vue`(목록 행, 모바일 두 줄), `PostAttachment.vue`(상세 첨부 상자, viewable 분기), `AttachmentPicker.vue`(없음/내 분석/개념 카드 선택·미리보기 칩). 공용 `PageNav`·`EmptyState` 재사용. 게시판 이름·칩 라벨은 `components/community/boardTypes.js` 한 곳에 둔다 |
| SC-10 변경 | `ConceptDetailView.vue`에 "이 카드로 글쓰기" 버튼 (`/community/write?conceptId={id}`) |
| store | 없음 (화면 지역 상태). 로그인 회원 id는 `useAuthStore().user.id` |
| API 함수 | `src/api/posts.js`: `fetchPosts({boardType, keyword, page, size})`, `fetchPost(id)`, `createPost(body)`, `updatePost(id, body)`, `deletePost(id)`. 첨부 고르기는 기존 `fetchAnalyses`·`fetchAnalysis`·`fetchConcepts`·`fetchConcept` |
| 상태별 UI | 목록: 불러오는 중 / 0건(+검색 초기화) / 서버 `message`. 상세: 불러오는 중 / 404 안내 + 목록으로 / 볼 수 없는 첨부 안내 / 서버 `message`. 작성: 필드별 검증 / 저장 중 비활성 / 이탈 확인 |
| 본문 표시 | 텍스트 보간(`{{ }}`) + `white-space: pre-wrap`. `v-html`·마크다운 렌더링은 쓰지 않는다 |

## 6. LLM (AI 기능만)

해당 없음.

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1~AC3 | `PostListApiTest#AC1_...`~`#AC3_...` (페이지·정렬·commentCount·attachmentType, boardType·keyword 조합·trim·`%`·`_` 리터럴·400, 탈퇴 작성자) | MockMvc + Testcontainers 통합 |
| AC4, AC5 | `PostDetailApiTest#AC4_...`, `#AC5_...` (공개·비공개×주인·남·비로그인·삭제된 분석, DRAFT 카드) | MockMvc 통합 |
| AC6~AC8 | `PostWriteApiTest#AC6_...`~`#AC8_...` (활동 행, 경계 100·101·5000·5001, 첨부 둘 다, 첨부 실패 4종·2종) | MockMvc 통합 |
| AC9~AC11 | `PostWriteApiTest#AC9_...`~`#AC11_...` (통째 교체·조회수 유지·활동 없음, 댓글 CASCADE·활동 유지, 403이 400보다 먼저) | MockMvc 통합 |
| AC12 | `PostAuthApiTest#AC12_...` (토큰 없음·만료·형식 오류·탈퇴 회원 토큰, 공개 경로에 깨진 토큰) | MockMvc 통합 |
| keyword 이스케이프 | `PostServiceTest#keyword_이스케이프` | 단위 |
| AC13, AC14 | `views/community/__tests__/PostListView.spec.js`, `components/community/__tests__/PostRow.spec.js` | Vitest |
| AC15, AC16 | `views/community/__tests__/PostDetailView.spec.js`, `components/community/__tests__/PostAttachment.spec.js` | Vitest |
| AC17, AC18 | `views/community/__tests__/PostWriteView.spec.js`, `components/community/__tests__/AttachmentPicker.spec.js`, `ConceptDetailView.spec.js`(글쓰기 버튼) | Vitest |
| AC13~AC18 화면 | ui-verifier: SC-14·15·16 데스크톱·375px, 콘솔 에러 없음 | Playwright |
| API 계약 | contract-checker: spec ↔ `openapi.json` ↔ `src/api/posts.js` | 정적 |

## 8. 범위 밖

- 댓글 작성·조회·수정·삭제, 상세 화면의 댓글 영역 (F-18). 목록의 `commentCount`만 이번에 넣는다
- SC-07 분석 결과의 "커뮤니티에 질문하기" 버튼 (A 화면). 이번 spec은 `/community/write?analysisId=`만 받아 두고, 버튼은 구현 PR에서 follow-up 이슈(`area:analysis`, 담당 김동환)로 넘긴다
- 분석 공개/비공개 설정 (F-19). F-19 전에는 모든 분석이 비공개라 남에게는 "볼 수 없는 분석입니다"로 보인다
- 팔로워 피드에 게시글 표시 (F-04, 이번에는 `POST` 활동 기록만)
- 조회수 중복 방지, 인기순 정렬, 좋아요, 관리자의 남의 글 삭제·신고, 첨부 여러 개, 이미지 첨부
- 본문 마크다운·코드 하이라이트 (글자 그대로 표시만)
- 프로필(SC-18)의 "내가 쓴 글" 목록

## 9. 결정 필요

- ~~(구현 착수 전) `PUT`에서 작성자 확인(403)을 형식 검증(400)보다 먼저 하는 방법을 고른다~~ → **결정(11-20)**: `PUT`은 Controller에 `@Valid`를 달지 않고, `PostService.update`가 404·403 확인 뒤 주입받은 `jakarta.validation.Validator`로 직접 검증해 `COMMON_400`(`details` 필드별)로 던진다. `@Validated` 그룹은 Controller 시점에 돌아 순서를 바꾸지 못한다. `POST`는 `@Valid` 그대로. 첨부 둘 다(`details.conceptId`)는 두 경로 모두 Service가 본다

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/F-17-community-board`
- [x] Flyway 마이그레이션 (없음)
- [x] AC별 실패 테스트 작성 (red 확인) — BE AC1~12, FE AC13~18 (뷰 테스트는 기존 관례대로 `views/__tests__/`)
- [x] BE 구현 (green): `PostController`, `PostService`, `PostMapper`(+XML), DTO(`PostRequest`·`PostSearchRequest`·`PostSummaryResponse`·`PostDetailResponse`·`PostAnalysisAttachment`·`PostConceptAttachment`·`PostIdResponse`), `ErrorCode` `POST_001`·`POST_002`·`POST_403`·`POST_404`, `PUBLIC_GET_PATHS`
- [x] FE 구현 (green): `api/posts.js`, `routes/community.js`, 뷰 3개, 컴포넌트 4개, SC-10 글쓰기 버튼
- [x] Swagger 어노테이션 + `docs/api/openapi.json` 갱신
- [x] contract-checker 통과 (불일치 0, 참고 2: openapi `$ref`+`type:null` 형제, edit 진입 시 조회수 +1)
- [x] ui-verifier 통과 (FE 변경 시, 데스크톱·모바일) — 12개 라우트, 콘솔 에러·375px 가로 스크롤 없음
- [ ] reviewer 지적 사항 반영
- [x] 산출물 반영 (follow-up 이슈는 PR 때): `06-화면설계서.md` SC-14·15·16의 "spec에서 정할 것" 정리 + 스크린샷, SC-10에 글쓰기 버튼, `docs/conventions.md` 3절 POST 코드, follow-up 이슈(SC-07 질문하기 버튼)
- [ ] PR CI 초록
- [ ] 상태 DONE (PR 머지 후)
