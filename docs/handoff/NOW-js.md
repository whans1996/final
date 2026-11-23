# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-23 14:28 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-13-admin-concepts

---

## 현재 기능

F-13 관리자 카드 관리 — **PR #75 열림** (spec `docs/specs/F-13-admin-concepts.md`, `Closes #51`, @whans1996 멘션). /pr-review 점검 끝: verify all 통과, reviewer 반드시 수정 0·권장 3건 반영(a95c115), contract-checker 실질 불일치 0. **Codex ✅ 0fa0038 (P2 2건 → #76, #77)**. CI: 하네스 테스트가 NOW 갱신 줄 형식(맨 앞 `갱신:`)으로 실패 → 이 커밋에서 고침

## 진행

- ✅ BE AC1~AC12 (V7 `concept_source_admin`, ErrorCode `CONCEPT_002`·`003`, `R__seed_concepts.sql` status 보존)
- ✅ FE AC13 목록(b832207), AC14·AC15 편집(5339550) + contract-checker 지적 2건(저장·삭제 중 404 → 없는 카드 안내, 폼에 없는 details 키는 submit-error)
- ✅ 산출물: SC-20(스크린샷 4장 `screens/SC-20-{list,edit}-{1280,375}.png`), ERD concepts ADMIN, UC50
- ✅ 리뷰 권장 반영(a95c115): 시드 upsert는 DRAFT만 PUBLISHED로 승격(`IF(concepts.status='DRAFT',…)`), `AdminConceptRequestTest` 카테고리 regex 고정, 목록 성공 안내를 history state에서 지움
- 테스트: FE `AdminConceptListView`(13)·`AdminConceptEditView`(14)·`AdminConceptForm`(6), BE `AdminConceptApiTest`(63)·`AdminConceptServiceTest`·`SeedConceptsTest#AC12`

## 바로 다음 할 일

1. `gh pr checks 75` 초록 확인 (하네스 테스트 재실행 결과) → `docs(F-13): 완료 표시 (spec DONE)` 커밋(spec 헤더 `DONE (PR #75)`) → GitHub에서 머지
2. 로컬 정리(선택): 화면 점검용 테스트 회원 `ui-admin@example.test`(ADMIN)·초안 카드 id 171 "[UI점검] 낙관적 락"이 로컬 DB에 있다

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 파일: `ConceptSource`(ADMIN), `ErrorCode` CONCEPT 블록, `ConceptMapper` B 블록, `ConceptService.escapeLike` public, FE `router/routes/concept.js` adminChildren, concept/** 공동 소유
- 목록 URL의 전체 탭은 FE 전용 `?status=ALL`(API에는 status 생략). 편집 저장 후 `history.state.back`의 목록 조건으로 돌아간다
- MyBatis constructor resultMap에서 primitive long은 `javaType="_long"`
