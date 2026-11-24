# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-24 13:43 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/issue-77-concept-link-fk

---

## 현재 기능

**#77 개념 추출 중 지워진 초안 카드로 분석 FAILED** (`feature/issue-77-concept-link-fk`, develop 0d7d2e6에서 분기). 이슈 브랜치, 이슈 "제안"이 수용 기준

## 진행

- ✅ ERD PR #116 머지 (11-24)
- ✅ #77 수정 bc87e3b: `AnalysisConceptMapper.insertAll`을 `INSERT ... SELECT ... JOIN concepts`로 바꿔 남은 카드만 넣고, `ConceptLinkService.save`가 빠진 개수를 warn 로그. 테스트 `ConceptLinkServiceTest.issue77_*` 2개, `AnalysisExtractingPipelineTest.issue77_*` 1개 (red→green), verify be 통과
- A 영역 파일(analysis/concept, 테스트 2개) 수정 → PR에서 @whans1996 멘션

## 바로 다음 할 일

1. `/pr-review` (PR 설명 `.harness/pr-issue-77.md`, `Closes #77`, 공통 코드 변경에 A 파일 5개) → CI·Codex COMPLETE → 사람이 머지
2. 그다음 #44 하네스 동기화 다듬기 (`feature/issue-44-sync-tests`): `scripts/test/sync.test.mjs` 경계 테스트 4개, session-start "## 자동 동기화" 헤딩 Git 블록 뒤로, `autoSyncEnabled`를 `git config --type=bool`로
3. #77·#44 뒤에는 멈춘다 (사용자 지시 11-24). Phase 6은 나중에

## 막힌 점 / 결정 필요

- 없음

## 주의

- 최종 ERD 이미지(`diagrams/erd.png`, Workbench Reverse Engineer)는 W5에 붙인다
