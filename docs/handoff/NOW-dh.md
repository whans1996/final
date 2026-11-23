# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-24 08:54 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-24-challenge-admin

## 현재 기능

**F-24 학습 챌린지 관리** spec `docs/specs/F-24-challenge-admin.md` — **APPROVED**, `/implement` 진행 중. 이번 세션 목표: BE AC1~AC10 → FE AC11~AC13

## 진행

- ✅ BE AC1~AC10 green (84개): `challenge/admin/AdminChallengeController·Service·dto`, `ChallengeMapper`(+XML), `ChallengeStatus`·`ChallengeGoalType`·`ChallengeImageStorage`, `common/config/WebConfig`(`/uploads/challenges/**`만), `ErrorCode` CHALLENGE_404·001, openapi.json·conventions 3절
- 설계: 요청 검증은 서비스에서 `Validator`로 (404 → 400 → 409 순서), 이미지는 `afterCompletion`(커밋 → 이전 파일, 롤백 → 새 파일 삭제). API 테스트는 `@Transactional` 없이 `@AfterEach` 정리
- 🟡 FE 시작: `api/adminChallenges.js`(+spec), `components/challenge/admin/labels.js`·`validation.js` 작성(미커밋)

## 바로 다음 할 일

1. FE: `AdminChallengeStatusTabs`·`AdminChallengeTable`·`AdminChallengeForm`(미리보기 createObjectURL/revoke), `views/admin/challenge/AdminChallengeListView`(삭제 확인 모달, 브라우저 confirm 금지)·`EditView`, `routes/challenge.js` adminChildren(order 20). 테스트는 `views/__tests__/AdminChallenge*.spec.js`(viewTestUtils 재사용), `components/challenge/admin/__tests__/`
2. 그다음 contract-checker, ui-verifier, Chrome으로 SC-21 스크린샷, 산출물(SC-21·UC51)

## 막힌 점 / 결정 필요

- 사람: 노션 팀원 초대, #77 담당(F-08 A·F-13 B 걸침)

## 주의

- 공통 변경(PR에서 @JoonseobKim): `ErrorCode`(CHALLENGE 블록), `common/config/WebConfig`(신규), `UploadResourceTest`(신규)
- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시) · 하네스 동결(11-23): 버그·작업 막는 문제만 고친다
- Bash heredoc에 따옴표가 섞인 긴 Java를 넣으면 파싱 실패 → Write 도구로
