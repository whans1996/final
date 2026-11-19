# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-19 16:50 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-08-concept-extraction

## 현재 기능

F-08 개념 카드 추출·연결 — **PR #50 머지 대기** (CI 초록, Codex ✅ `2a55ced`, spec DONE). 사람이 GitHub에서 머지

## 진행

- ✅ BE AC1~AC7·AC10, FE AC8·AC9, 산출물 02·03·06·08·09·datasets.json
- ✅ /pr-review 점검: verifier 통과(BE 374·FE 182·하네스 57), contract-checker 불일치 0, develop(#45·#47 F-09) 병합(06 충돌 해결, openapi.json 재생성 차이 없음)
- ✅ reviewer 반드시 수정 1건 `c9b4ede` + 권장 3건 `3621925`
- ✅ 스크린샷 `16c61fa` SC-07-concepts-{desktop,mobile} (로컬 계정 `ui-f08@local.test`, 로그인 상태 `.harness/ui-f08.json`, 분석 4번), follow-up #49(fixture가 시드 auto-increment id에 기댐)

## 바로 다음 할 일

1. PR #50 머지 확인(`gh pr view 50 --json state`) → `git switch develop && git pull`, #23은 close-issues 워크플로가 닫는지 확인
2. 다음 기능: 내 영역(A)에서 `/spec` (F-14 퀴즈 등, `docs/기획서.md` 일정 확인) 또는 follow-up #24·#26·#49

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부

## 주의

- B 파일(`ConceptService`·`ConceptController`·`api/concepts.js`·`routes/concept.js`)과 공용 `ConceptCard.vue`는 고치지 않는다. Gradle 테스트를 bootRun과 동시에 돌리지 않는다. V1~V5 수정 금지
- Git Bash heredoc·sed에서 한글·`\n`이 깨질 수 있다 → 한글 수정은 Edit 도구로
