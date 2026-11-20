# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신: 2026-11-20 22:05 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-13-admin-concepts

---

## 현재 기능

F-13 관리자 카드 관리 — `/implement` 진행 중 (spec `docs/specs/F-13-admin-concepts.md`, AC1~AC15). 이번 세션 목표: BE AC1~AC12

## 진행

- ✅ V7 `concept_source_admin` + `ConceptSource.ADMIN`, ErrorCode `CONCEPT_002`·`CONCEPT_003`
- ✅ AC1~AC3 green: `concept/admin/AdminConceptController`·`AdminConceptService`·dto 4종, `ConceptMapper` B 블록 끝 "F-13" 메서드(findForAdmin·countForAdmin·countByStatus·findByIdForUpdate·existsByName·insertByAdmin·updateByAdmin·deleteDraft·countLinkedAnalyses), `ConceptService.escapeLike` public
- 테스트 `AdminConceptApiTest`는 @BeforeEach에서 트랜잭션 안 `DELETE FROM concepts`(롤백) 후 데이터를 넣는다

## 바로 다음 할 일

1. AC4·AC5: `AdminConceptApiTest`에 POST 테스트(trim·빈 값 NULL·source ADMIN·HIDDEN 거부·필드별 details·#51 URL 3종) → `dto/AdminConceptRequest`(문자열 검증, compact 생성자 trim) + `AdminConceptService.create`
2. AC6~AC10 PUT·DELETE(`findByIdForUpdate`, DuplicateKeyException→CONCEPT_002), AC11 권한, AC12 `R__seed_concepts.sql`에서 `status = seed.status` 제거 + `SeedConceptsTest#AC12_...`
3. 그다음 FE AC13~AC15

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 파일 변경: `ConceptSource`(ADMIN), `ErrorCode` CONCEPT 블록(002·003), `ConceptMapper` B 블록, concept/** 전체가 CODEOWNERS 공동 소유 → PR에 @whans1996 멘션. `conventions.md` 3절 CONCEPT 행 추가 예정
- `R__seed_concepts.sql` 수정 시 체크섬이 바뀌어 다음 기동 때 한 번 재적용된다
- MyBatis constructor resultMap에서 primitive long은 `javaType="_long"` (`long`은 Long 별칭)
