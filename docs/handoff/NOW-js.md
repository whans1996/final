# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-20 21:32 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-04-follow-feed

---

## 현재 기능

F-04 팔로우·활동 피드 — **PR #71 열림, CI 전부 초록, Codex ✅ fdbfcc8 (지적 없음)**, spec DONE 표시 커밋. 남은 것: 사람이 GitHub에서 머지

## 진행

- ✅ AC1~AC5 팔로우 BE (d498220): `follow/FollowController`·`FollowService`(follow는 나·대상 users 행 id 오름차순 FOR UPDATE)·`FollowMapper.xml`. 자기 자신 에러는 `USER_002`(spec의 FOLLOW_001이 conventions 3절과 어긋나 변경, 결정로그)
- ✅ FE AC9~AC11: `api/follows.js`·`feed.js`, `components/follow/FollowSummary`·`FollowUserItem`, `views/UserFollowsView`, `views/community/FeedView`·`components/feed/FeedItem`·`feedTypes.js`, `composables/useMoreList.js`(더 보기+id 거름), `utils/relativeTime.js`, nav 피드 order 7(App·navItems 테스트 갱신)
- ✅ AC6~AC8 피드 BE: `feed/FeedController`(`GET /api/feed`)·`FeedService`·`FeedMapper.xml`(목록·COUNT 공용 `<sql id="visible">`, REVIEW target.id=카드 id, ANALYSIS 제목 없으면 FE용 "제목 없는 분석"은 `FeedItemResponse.from`). `FeedApiTest` 11개
- 주의: 테스트 트랜잭션 안에서 같은 MyBatis SELECT를 두 번 부르면 세션 캐시가 옛 결과를 준다 → `SqlSessionTemplate.clearCache()` (FeedApiTest 찜 재찜 케이스)

## 바로 다음 할 일

1. 사람이 PR #71을 GitHub에서 머지한다 (상대 PR이 먼저 develop에 머지됐으면 `git merge origin/develop` → push → CI 재확인 후)
2. 머지 뒤 `/pickup`: `git switch develop && git pull`, #64가 `close-issues` 워크플로로 닫혔는지 확인 → 다음 B 영역 기능(`/spec`) 또는 내 이슈 #46·#51

## 막힌 점 / 결정 필요

- 없음. 피드 상대 시각 시간대 문제는 #17에 댓글로 넘김

## 주의

- 공통 파일 변경(`ErrorCode` USER_002, `SecurityConfig` FOLLOW 블록, `conventions.md` 3절 USER_002, `feed` 패키지, `routes/user.js`, `UserProfileView`, `App.spec`·`navItems.spec` nav 기대값, F-12 `WishButton.spec` 대기) → PR에 @whans1996 멘션
- 하네스 PR #69(#67·#48·#39) develop 머지됨(8189d3f), 이 브랜치에 이미 포함 → `codex-review.mjs`가 Codex 실패·정체를 STALE로 판정
