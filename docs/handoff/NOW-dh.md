# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-19 20:26 · 작업자: whans1996@naver.com · PC: dh-home · 브랜치: feature/issue-49-mock-concept-names

## 현재 기능

follow-up #49 mock concept fixture가 시드 카드 id에 기대는 문제 — **PR #53 열림** (develop 대상, `Closes #49`). **Codex ✅ `28641e7`** (지적 없음), CI Backend 검증 실행 중

## 진행

- ✅ `MockLlmClient.linkConceptNames` + `concept.json` 시드 이름 5개 (`c69697e`), SC-07 개념 탭 스크린샷 (`c4d22d1`)
- ✅ /pr-review: verifier 통과, reviewer 반드시 수정 0·권장 3 모두 반영 (`28641e7`): 템플릿 전체 프롬프트로 AC10 검사, `MockLlmClient(Function)` 주입 생성자로 `complete()` CONCEPT 분기 테스트, 이름에 ` | ` 포함 테스트, spec AC10·8절 문구 + 결정로그 1줄
- PR 설명 `.harness/pr-issue-49.md`

## 바로 다음 할 일

1. `gh pr checks 53`으로 Backend 검증 초록 확인 (실패하면 `gh run view --log-failed`)
2. CI 초록 + Codex ✅ → 사람이 GitHub에서 머지 (spec 완료 표시는 없음: F-08은 이미 DONE)
3. 머지 뒤 다음 기능: F-14 코드 이해 퀴즈 `/spec` (Opus/high)

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부

## 주의

- B 파일(`ConceptService`·`ConceptController`·`api/concepts.js`·`routes/concept.js`)과 공용 `ConceptCard.vue`, 시드 `R__seed_concepts.sql`은 고치지 않는다. Gradle 테스트를 bootRun과 동시에 돌리지 않는다. V1~V5 수정 금지
- Git Bash heredoc·sed에서 한글·`\n`이 깨질 수 있다 → 한글 수정은 Edit 도구로
