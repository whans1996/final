# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-20 21:17 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-04-follow-feed

---

## 현재 기능

F-04 팔로우·활동 피드 — spec APPROVED `docs/specs/F-04-follow-feed.md`, `/implement` 진행 중. **AC1~AC11 모두 green**, contract-checker 불일치 0, 산출물(UC05·UC06·SC-17·SC-18) 반영, develop 병합(cf65f33, #70 하네스). **ui-verifier 통과(11-20 21시, 문제 0)** + 스크린샷 8장(SC-17-feed*·SC-18-follow*) 화면설계서에 삽입. 남은 것: `/pr-review`

## 진행

- ✅ AC1~AC5 팔로우 BE (d498220): `follow/FollowController`·`FollowService`(follow는 나·대상 users 행 id 오름차순 FOR UPDATE)·`FollowMapper.xml`. 자기 자신 에러는 `USER_002`(spec의 FOLLOW_001이 conventions 3절과 어긋나 변경, 결정로그)
- ✅ FE AC9~AC11: `api/follows.js`·`feed.js`, `components/follow/FollowSummary`·`FollowUserItem`, `views/UserFollowsView`, `views/community/FeedView`·`components/feed/FeedItem`·`feedTypes.js`, `composables/useMoreList.js`(더 보기+id 거름), `utils/relativeTime.js`, nav 피드 order 7(App·navItems 테스트 갱신)
- ✅ AC6~AC8 피드 BE: `feed/FeedController`(`GET /api/feed`)·`FeedService`·`FeedMapper.xml`(목록·COUNT 공용 `<sql id="visible">`, REVIEW target.id=카드 id, ANALYSIS 제목 없으면 FE용 "제목 없는 분석"은 `FeedItemResponse.from`). `FeedApiTest` 11개
- 주의: 테스트 트랜잭션 안에서 같은 MyBatis SELECT를 두 번 부르면 세션 캐시가 옛 결과를 준다 → `SqlSessionTemplate.clearCache()` (FeedApiTest 찜 재찜 케이스)

## 바로 다음 할 일

1. `/pr-review` — verifier·reviewer·contract-checker → PR 본문에 `Closes #64`, 공통 파일 @whans1996 멘션, 스크린샷 `node scripts/pr-screens.mjs SC-17 SC-18`
2. spec 체크리스트 남은 것: reviewer 반영, 산출물(REST API 명세 확인), PR CI 초록

## 막힌 점 / 결정 필요

- 없음. ui-verifier가 못 본 것: 언팔로우 hover, 비로그인 클릭 redirect, "새 활동이 없습니다", 더 보기 (단위 테스트로만 확인). BE는 21:15에 띄운 새 코드(bootRun local)

## 주의

- 공통 파일 변경(`ErrorCode` USER_002, `SecurityConfig` FOLLOW 블록, `UserService.findByIdForUpdate`, `conventions.md` 3절 USER_002, `feed` 패키지, `routes/user.js`, `UserProfileView`, `App.spec`·`navItems.spec` nav 기대값) → PR에 @whans1996 멘션
- 하네스 PR #69(#67·#48·#39) develop 머지됨(8189d3f), 이 브랜치에 이미 포함 → `codex-review.mjs`가 Codex 실패·정체를 STALE로 판정
