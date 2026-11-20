# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-20 17:02 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-04-follow-feed

---

## 현재 기능

F-04 팔로우·활동 피드 — spec APPROVED `docs/specs/F-04-follow-feed.md`, `/implement` 진행 중. 이번 세션 목표: BE AC1~AC8

## 진행

- ✅ AC1~AC5 팔로우 BE green (`follow/FollowApiTest` 8, `FollowQueryApiTest` 8): `FollowController`·`FollowService`(follow는 나·대상 users 행을 id 오름차순 FOR UPDATE, unfollow는 일반 조회)·`FollowMapper.xml`(INSERT IGNORE, 수·목록은 ACTIVE 회원 JOIN), `ErrorCode.USER_002`(자기 자신, 결정로그 11-20: conventions상 팔로우=USER 도메인), `SecurityConfig` FOLLOW 블록, `UserService.findByIdForUpdate` 추가
- ⏳ AC6~AC8 피드 BE

## 바로 다음 할 일

1. `feed/FeedApiTest#AC6_팔로우한_사람의_활동만_최신순으로_보인다` red → `feed/FeedController`(`GET /api/feed`, Tag `피드`)·`FeedService.getFeed(me, page)`·`FeedMapper.xml`(spec 4절 쿼리: follows JOIN + 종류별 LEFT JOIN + OR 조건, REVIEW는 activities.target_id=리뷰 id → target.id=카드 id)
2. AC7 종류별 숨김(분석 비공개·삭제·DONE 아님 / 리뷰 삭제·카드 비공개 / 찜 해제·카드 비공개 / 글 삭제 / 챌린지 삭제), AC8 빈 피드
3. 그다음 FE AC9~AC11 (spec 5절)

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 파일 변경(`ErrorCode` USER_002, `SecurityConfig` FOLLOW 블록, `UserService.findByIdForUpdate`, `feed` 패키지, `routes/user.js`, `UserProfileView`) → PR에 @whans1996 멘션
- `codex-review.mjs`가 Codex 실행 실패를 RUNNING으로 기다린다 (#48, 상대 PR #69가 수정 중)
