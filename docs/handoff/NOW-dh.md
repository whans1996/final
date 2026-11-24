# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-24 10:02 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-25-challenge-participation

## 현재 기능

**F-25 챌린지 참여·달성도** spec `docs/specs/F-25-challenge-participation.md` — **APPROVED (김동환 11-24)**, 관련 이슈 #99 흡수

## 진행

- ✅ F-24 PR #97 머지(11-24 09:47)
- ✅ F-25 spec 초안: AC16개(BE 11, FE 5). 사용자 결정 4개(시작일부터 집계, 퀴즈는 (퀴즈, 날짜)당 1회, 미달성·종료 전만 취소하고 JOIN 활동 삭제, 참여 목록 공개)를 결정로그에 한 줄로 적음. Flyway 없음
- ✅ spec 승인 커밋 `docs(F-25): spec 승인`, develop(#100 F-20c 시작일 상한) 합쳐 push

## 바로 다음 할 일

1. `/clear` → `/implement F-25` (Opus/high). BE부터: `ChallengeProgressService.refresh` + `ChallengeParticipantMapper.xml` 집계 쿼리 3개 → `ChallengeProgressTest` AC5~AC8
2. 로컬 DB에 `[시연]` 챌린지 3개가 있다 (화면 점검용, 지워도 됨)

## 막힌 점 / 결정 필요

- 사람: 노션 팀원 초대, #77 담당(F-08 A·F-13 B 걸침)

## 주의

- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시) · 하네스 동결(11-23): 버그·작업 막는 문제만 고친다
- Bash heredoc·python 치환은 백슬래시와 `갱신:` 같은 반복 문자열에 약하다(라우트 `:id(\\d+)`·NOW 머리말 깨짐 실제 발생) → 문서·코드는 Edit/Write 도구로
- F-25는 공통 `feed.ActivityService`에 `delete`를 더한다 → PR 때 `owners.mjs --diff`로 @멘션 확인
