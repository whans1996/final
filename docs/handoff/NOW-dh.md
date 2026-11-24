# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-24 10:19 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-25-challenge-participation

## 현재 기능

**F-25 챌린지 참여·달성도** spec `docs/specs/F-25-challenge-participation.md` — APPROVED, `/implement` 진행 중

## 진행

- ✅ BE AC1~AC11 green (`verify be` 1009 통과), openapi.json 갱신, 커밋 `feat(F-25): BE ...`
  - 테스트: `challenge/ChallengeApiTest`·`ChallengeParticipationApiTest`·`ChallengeProgressTest`·`UserChallengeApiTest` (+`ChallengeTestSupport`)
  - 구현: `ChallengeService`, `ChallengeProgressService.refresh/recount`, `ChallengeParticipantMapper`, 컨트롤러 2개. 집계 호출은 `ReviewPlanService.changeStatus/delete`·`QuizService.submit`·`AnalysisService.complete`
- #101(Actions 결제 막힘): 사용자 결정 11-24 "로컬 구현 계속, PR·머지는 복구 뒤"

## 바로 다음 할 일

1. FE AC16: `frontend/src/utils/seoulDate.js`(`todayInSeoul(now)`, `daysBetween(a,b)`) + `utils/__tests__/seoulDate.spec.js` → `components/challenge/admin/validation.js`의 `todayIso` 교체(#99)
2. FE: `src/api/challenges.js`(+spec) → `components/challenge/admin/labels.js`를 `components/challenge/labels.js`로 이동 → `ChallengeCard`·`ChallengeProgressBar`·`ChallengeJoinPanel`·`MyChallengeList` → `views/challenge/ChallengeListView`·`ChallengeDetailView`, `routes/challenge.js`(nav order 9) → 대시보드·프로필 절 (AC12~15)

## 막힌 점 / 결정 필요

- 사람: 노션 팀원 초대, #77 담당(F-08 A·F-13 B 걸침), #101 Actions 결제 확인

## 주의

- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시) · 하네스 동결(11-23): 버그·작업 막는 문제만 고친다
- 공통 파일 변경(ErrorCode, SecurityConfig, feed.ActivityService·ActivityMapper) → PR "공통 코드 변경"에 적고 @JoonseobKim 멘션
- openapi 경로 변수가 `/api/users/{userId}/challenges`(spec은 `{id}`). contract-checker 때 확인
