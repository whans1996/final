# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-20 17:12 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-04-follow-feed

---

## 현재 기능

F-04 팔로우·활동 피드 — spec APPROVED `docs/specs/F-04-follow-feed.md`, `/implement` 진행 중. **BE AC1~AC8 완료**, 다음 FE AC9~AC11

## 진행

- ✅ AC1~AC5 팔로우 BE (d498220): `follow/FollowController`·`FollowService`(follow는 나·대상 users 행 id 오름차순 FOR UPDATE)·`FollowMapper.xml`. 자기 자신 에러는 `USER_002`(spec의 FOLLOW_001이 conventions 3절과 어긋나 변경, 결정로그)
- ✅ AC6~AC8 피드 BE: `feed/FeedController`(`GET /api/feed`)·`FeedService`·`FeedMapper.xml`(목록·COUNT 공용 `<sql id="visible">`, REVIEW target.id=카드 id, ANALYSIS 제목 없으면 FE용 "제목 없는 분석"은 `FeedItemResponse.from`). `FeedApiTest` 11개
- 주의: 테스트 트랜잭션 안에서 같은 MyBatis SELECT를 두 번 부르면 세션 캐시가 옛 결과를 준다 → `SqlSessionTemplate.clearCache()` (FeedApiTest 찜 재찜 케이스)

## 바로 다음 할 일

1. FE API: `frontend/src/api/follows.js`(`fetchFollowStatus`·`follow`·`unfollow`·`fetchFollowers`·`fetchFollowings`), `src/api/feed.js`(`fetchFeed`) + `src/api/__tests__` (기존 `posts.js` 패턴)
2. `utils/relativeTime.js` `formatRelativeTime(iso, now)` + `utils/__tests__/relativeTime.spec.js` (AC11 규칙)
3. AC9 `components/follow/FollowSummary.vue` → `UserProfileView.vue`, AC10 `views/UserFollowsView.vue`(`routes/user.js` `/users/:id(\\d+)/follows`), AC11 `views/community/FeedView.vue`·`components/feed/FeedItem.vue`(`routes/community.js` nav 피드 order 7)

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 파일 변경(`ErrorCode` USER_002, `SecurityConfig` FOLLOW 블록, `UserService.findByIdForUpdate`, `conventions.md` 3절 USER_002, `feed` 패키지, `routes/user.js`, `UserProfileView`) → PR에 @whans1996 멘션
- `codex-review.mjs`가 Codex 실행 실패를 RUNNING으로 기다린다 (#48, 상대 PR #69가 수정 중)
