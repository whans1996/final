# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-27 10:45 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/issue-218-e1-runner

## 현재 기능

**E-1 러너·채점기 후속 4건 #218·#219·#221·#223** (`feature/issue-218-e1-runner`, 이슈 정리 순서 ②, 12/01 EV-1 기준선 전). spec `docs/specs/E-1-eval-runner-grader.md`(DONE) 지표 표·테스트 목록만 갱신

## 진행

- ✅ K-3a PR #234·하네스 PR #236 머지 (#235 닫힘)
- ✅ 커밋 56dad21: #218 C에서 개념 실패 수준 제외(`C.excluded`, 요약 "C 뺀 수준(합)"), #219 `QuizCheck.POSITION` 횟수·코드 인자 제외, #221 `EvalPipeline.swallowing`이 마지막 응답을 앱 파서로 재판정(`FileExplainer.parse` public), #223 `EvalSummary` 중복 제거는 수준 많은 것 우선(grade에 `levels`). 결정로그 1행. verify be 통과(268초)

## 이슈 정리 순서 (11-26 사용자 승인)

① ✅ → K-3a ✅ → ② E-1 러너 🔨(이 브랜치) → HP-4에 ④ #133·#134·#178·#196·#198·#224 흡수 → ③ A 버그 #168(500 먼저)·#200~#203·#155·#156·#150·#121, ⑤ 가드 우회 #188~#191·#193 한 PR. #163→F-27b, #204→K-1, #137→HP-5

## 바로 다음 할 일

1. `/pr-review` (reviewer → PR 본문 `Closes #218 #219 #221 #223`, Roadmap 줄 없음) → push → Codex 루프
2. 머지 후 HP-4 (로드맵 11/28~11/29, #165·#166 + ④ 묶음) `/spec HP-4`

## 막힌 점 / 결정 필요
- 사람: 노션 팀원 초대, F-05b liveTest · 로드맵에 E-2(D·E 기준표·쌍 비교, 12/09 전) 행 추가 필요
## 주의
- 공통: `R__seed_concepts.sql`(생성본)·`SeedConceptsTest`·CODEOWNERS(`/data/seed/`·`/backend/src/test/resources/seed/` A)·`frontend/package.json`·`ci.yml` → PR에 @JoonseobKim
- Bash heredoc·`node -e` 안의 `\`가 줄어든다 → 역슬래시가 든 내용은 Write·Edit 도구로 · 다음 마이그레이션 V15
