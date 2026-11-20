# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신: 2026-11-20 09:09 · 작업자: whans1996@naver.com · PC: dh-home · 브랜치: feature/F-14-code-quiz

## 현재 기능

F-14 코드 이해 퀴즈 (`docs/specs/F-14-code-quiz.md`) — AC1~AC11 green, develop 병합 완료. 남은 것: `/pr-review`

## 진행

- ✅ AC1~AC11 (`bd9a7e9`·`df139ec`·`e97ace4`), 375px 탭 nowrap `0fa4933`, develop 병합+openapi `fed7933`, quiz fixture 이름 연결 `27dab65`(spec AC11·결정로그, 사용자 승인)
- ✅ 375px 선택 탭 `scrollIntoView`(`AnalysisDetailView.vue` `tabsEl`+watch), `06-화면설계서.md` SC-07 퀴즈 탭·SC-08 스크린샷 표, 6장 재촬영(devtools 숨김). ui 점검: 콘솔·요청 오류 0, 문항 5개 카드 연결·1개 null (로컬 분석 8·9)

## 바로 다음 할 일

1. `/clear` 후 새 세션(Opus/high)에서 `/pr-review` — verifier·reviewer·contract-checker → PR(develop 대상, @JoonseobKim 멘션, spec과 다른 점 3개 본문에) → Codex 루프
2. PR 본문 스크린샷: `node scripts/pr-screens.mjs SC-07 SC-08`

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부

## 주의

- 공통 파일 변경: `application.yml`(quiz 블록), `docs/conventions.md`(QUIZ 행), `ErrorCode`(QUIZ 2개), `AnalysisService.checkOwned` → PR에 @JoonseobKim 멘션
- spec과 다른 점(PR 본문에): 복습 INSERT는 `NOT EXISTS + ODKU`(CLIENT_FOUND_ROWS). FE는 red 단계 생략. quiz fixture 이름 연결(11-20 결정)
- 이 세션엔 프로젝트 에이전트(ui-verifier 등)가 등록 안 됨 → general-purpose에 `.claude/agents/*.md`를 읽게 해서 대신함. `.harness/auth.json`·`ui-49.json` refresh 토큰이 회전돼 다음 ui-check 때 재로그인 필요할 수 있음
- Gradle 테스트를 bootRun과 동시에 돌리지 않는다
