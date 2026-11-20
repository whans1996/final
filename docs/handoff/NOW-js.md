# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신: 2026-11-20 22:25 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-13-admin-concepts

---

## 현재 기능

F-13 관리자 카드 관리 — `/implement` 진행 중 (spec `docs/specs/F-13-admin-concepts.md`, AC1~AC15). **BE AC1~AC12 완료**, FE AC13~AC15 남음

## 진행

- ✅ V7 `concept_source_admin` + `ConceptSource.ADMIN`, ErrorCode `CONCEPT_002`·`CONCEPT_003`, conventions 3절
- ✅ AC1~AC3 조회(e7f003e), AC4~AC11 POST·PUT·DELETE(`AdminConceptRequest` trim·문자열 검증, `findByIdForUpdate` 잠금, 이름 사전 확인 + DuplicateKeyException→CONCEPT_002), AC12 `R__seed_concepts.sql` upsert에서 status 제거(`SeedConceptsTest#AC12_...`)
- 테스트: `concept/admin/AdminConceptApiTest`(63건, @BeforeEach에서 트랜잭션 안 `DELETE FROM concepts` 후 롤백), `AdminConceptServiceTest`(AC6 UNIQUE 변환)

## 바로 다음 할 일

1. FE AC13: `src/api/adminConcepts.js`(6함수) → `views/__tests__/AdminConceptListView.spec.js` red → `router/routes/concept.js` adminChildren에 `admin-concepts`·`admin-concept-new`·`admin-concept-edit`(meta.adminNav label '개념 카드', order 10) + `views/admin/concept/AdminConceptListView.vue`, `components/concept/admin/AdminConceptTable.vue`·`AdminConceptStatusTabs.vue`
2. FE AC14·AC15: `AdminConceptEditView.vue` + `AdminConceptForm.vue`(+ `__tests__/AdminConceptForm.spec.js`, `AdminConceptEditView.spec.js`)
3. contract-checker → ui-verifier(1280·375) → 산출물(SC-20·ERD concepts ADMIN·UC50) → `/pr-review`

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 파일 변경: `ConceptSource`(ADMIN), `ErrorCode` CONCEPT 블록(002·003), `ConceptMapper` B 블록 끝 "F-13", `ConceptService.escapeLike` public, concept/** CODEOWNERS 공동 소유 → PR에 @whans1996 멘션
- `R__seed_concepts.sql` 체크섬이 바뀌어 다음 기동 때 한 번 재적용된다 (status는 유지)
- MyBatis constructor resultMap에서 primitive long은 `javaType="_long"` (`long`은 Long 별칭)
