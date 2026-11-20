# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-20 16:55 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-04-follow-feed

---

## 현재 기능

F-04 팔로우·활동 피드 — spec **APPROVED** (김준섭 11-20) `docs/specs/F-04-follow-feed.md`, 브랜치 `feature/F-04-follow-feed`

## 진행

- ✅ F-18 PR #68 머지 (11-20 16:45)
- ✅ F-04 spec 초안: AC1~11 (팔로우 멱등 200·공개 상태/목록 GET·피드 필터링(#64 흡수)·FE 3화면), 결정로그 1행 추가
- 결정 4건 확정: 멱등 200 / 피드 페이지 번호 20 + 더 보기 / 팔로우 목록 별도 화면 `/users/:id/follows` / 피드는 팔로우한 사람만

## 바로 다음 할 일

1. `/clear` 후 새 세션 `/implement F-04` (Opus·high). 마이그레이션 없음
2. BE부터: `ErrorCode` FOLLOW 블록 `FOLLOW_001` → `follow/FollowApiTest#AC1_다른_회원을_팔로우하면_200과_팔로우_상태를_반환한다` red → `FollowController`·`FollowService.follow`·`FollowMapper.xml`(INSERT IGNORE, 대상 users FOR UPDATE)

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 파일 변경 예정(`ErrorCode` FOLLOW 블록, `SecurityConfig` FOLLOW 블록, `feed` 패키지, `routes/user.js`, `UserProfileView`) → PR에 @whans1996 멘션
- `codex-review.mjs`가 Codex 실행 실패를 RUNNING으로 기다린다 (#48, 상대 PR #69가 수정 중)
