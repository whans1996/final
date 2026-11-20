# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-20 09:27 · 작업자: whans1996@naver.com · PC: dh-home · 브랜치: feature/F-14-code-quiz

## 현재 기능

F-14 코드 이해 퀴즈 (`docs/specs/F-14-code-quiz.md`) — `/pr-review` 진행 중. 점검 끝, PR 생성 전(사용자 확인 대기)

## 진행

- ✅ AC1~AC11, develop 병합, 스크린샷 6장(SC-07 퀴즈 탭·SC-08)
- ✅ /pr-review 점검: verifier 통과(BE 464·FE 239), contract-checker 불일치 0, reviewer 반드시 수정 1(NOW 머리말 `d70494d`)
- ✅ reviewer '무시 가능'까지 전부 확인: 동시 제출 데드락 **재현**(다른 회원끼리도) → `INSERT IGNORE`로 수정 `5f784a5`(`ReviewPlanConcurrencyTest`), 다시 풀기 스크롤 `f7a8178`, 권장 3건 `59b10b5`, JSON 본문 크기 제한 없음 → #55(area:common)
- PR 설명 초안 `.harness/pr-F-14.md`

## 바로 다음 할 일

1. 사용자 확인 후 `git push -u origin feature/F-14-code-quiz` → `gh pr create --base develop --title "feat(F-14): 코드 이해 퀴즈" --body-file .harness/pr-F-14.md`
2. push 후 `node scripts/pr-screens.mjs SC-07 SC-08` 출력으로 PR 설명 `## 스크린샷` 갱신(`gh pr edit --body-file`), `gh pr checks`
3. Codex 루프: `node scripts/codex-review.mjs <PR> --request --wait --status` (background)

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부

## 주의

- 공통 파일 변경: `application.yml`(quiz 블록), `ErrorCode`(QUIZ 2개), `openapi.json`, `docs/conventions.md`(QUIZ 행) → PR에 @JoonseobKim 멘션
- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시)
- 이 세션엔 프로젝트 에이전트가 등록 안 됨 → general-purpose에 `.claude/agents/*.md`를 읽게 해서 대신함
