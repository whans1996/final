# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신: 2026-11-23 14:06 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-13-admin-concepts

---

## 현재 기능

F-13 관리자 카드 관리 — `/implement` 진행 중 (spec `docs/specs/F-13-admin-concepts.md`, AC1~AC15). **AC1~AC15 전부 green** (BE·FE). 마무리 검증·산출물·PR 남음

## 진행

- ✅ V7 `concept_source_admin` + `ConceptSource.ADMIN`, ErrorCode `CONCEPT_002`·`CONCEPT_003`, conventions 3절
- ✅ AC1~AC3 조회(e7f003e), AC4~AC11 POST·PUT·DELETE(`AdminConceptRequest` trim·문자열 검증, `findByIdForUpdate` 잠금, 이름 사전 확인 + DuplicateKeyException→CONCEPT_002), AC12 `R__seed_concepts.sql` upsert에서 status 제거(`SeedConceptsTest#AC12_...`)
- ✅ FE AC13: `api/adminConcepts.js`(6함수), 라우트 3개(`concept.js` adminChildren), `AdminConceptListView`(쿼리 정본, 전체 탭 `status=ALL`·조회 시 status 생략, 탭별 빈 문구, history state `notice`), `components/concept/admin/` `AdminConceptStatusTabs`·`AdminConceptTable`(640px 이하 CSS 카드형 행)·`labels.js`. FE AC14·AC15: `AdminConceptForm`(v-model 객체, 필드 에러, SEED 이름 readonly, MarkdownBody 미리보기·640px 이하 편집/미리보기 탭), `AdminConceptEditView`(상태별 버튼: 새 카드 초안·공개 / DRAFT 초안·공개·숨김·삭제 / PUBLISHED 저장·숨김 / HIDDEN 저장·공개, 저장 후 history.state.back의 목록 조건으로 복귀 + notice)
- 테스트: FE `AdminConceptListView.spec.js`(12)·`AdminConceptEditView.spec.js`(12)·`AdminConceptForm.spec.js`(6), BE `concept/admin/AdminConceptApiTest`(63건, @BeforeEach에서 트랜잭션 안 `DELETE FROM concepts` 후 롤백), `AdminConceptServiceTest`(AC6 UNIQUE 변환)

## 바로 다음 할 일

1. `contract-checker`(spec 3절 ↔ openapi.json ↔ `src/api/adminConcepts.js`)
2. BE(local) + FE dev 띄우고 `ui-verifier` `/admin/concepts`·`/admin/concepts/new`·`/admin/concepts/{id}/edit` × 1280·375 (ADMIN 계정 필요), 스크린샷을 `docs/산출물/screens/`에
3. 산출물: `06-화면설계서.md` SC-20, `03-ERD.md` concepts source ADMIN, `02-유스케이스.md` UC50 → `/pr-review` (#51은 AC5로 흡수 → PR에 `Closes #51`)

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 파일 변경: `ConceptSource`(ADMIN), `ErrorCode` CONCEPT 블록(002·003), `ConceptMapper` B 블록 끝 "F-13", `ConceptService.escapeLike` public, concept/** CODEOWNERS 공동 소유 → PR에 @whans1996 멘션
- `R__seed_concepts.sql` 체크섬이 바뀌어 다음 기동 때 한 번 재적용된다 (status는 유지)
- MyBatis constructor resultMap에서 primitive long은 `javaType="_long"` (`long`은 Long 별칭)
