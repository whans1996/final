# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-20 14:39 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-12-concept-wish

---

## 현재 기능

F-12 카드 찜 — spec `docs/specs/F-12-concept-wish.md` APPROVED, `/implement` 진행 중 (#57 흡수)

## 진행

- ✅ BE AC1~AC11 green (BE 566건 통과): `concept/wish/ConceptWishController·Service·Mapper(+XML)·dto/ConceptWishResponse`, `ConceptDetailResponse.wished`, `ActivityService.recordOnce`·`ActivityMapper.exists`, `PUBLIC_GET_PATHS`에 `/api/users/{id:\\d+}/wishes`, openapi.json 갱신
- 테스트: `ConceptWishApiTest`(AC1~AC11, AC7도 여기), `ConceptWishConcurrencyTest`(AC2 같은 회원·AC6 다른 회원 동시), `ConceptWishServiceTest`(DuplicateKey), `feed/ActivityServiceTest`
- spec과 다른 점: 쓰기 순서를 F-11처럼 `lockConcept` → `getPublished` → `getActiveMember`로 (잠금 전 스냅샷 문제)
- ⏳ FE 테스트 작성됨(미커밋, red): `WishButton.spec.js`, `UserWishes.spec.js`, `ConceptDetailView.spec.js`·`UserProfileView.spec.js`·`api/__tests__/concepts.spec.js` 추가분

## 바로 다음 할 일

1. FE 구현: `src/api/concepts.js`에 `wishConcept`·`unwishConcept`·`fetchUserWishes`, `components/concept/WishButton.vue`(data-action=toggle-wish, data-message=wish-review-plan/wish-error, class on), `UserWishes.vue`(data-section=wishes, data-action=more-wishes/browse-concepts), `ConceptDetailView`·`UserProfileView` 연결 → `node scripts/verify.mjs fe`
2. contract-checker, ui-verifier(375·1280), 산출물(06 SC-10·SC-18, 02 UC23) → `/pr-review`

## 막힌 점 / 결정 필요

- 없음

## 주의

- 공통 파일 변경: `concept` 패키지 전체(CODEOWNERS 공통)·`SecurityConfig`·`feed/ActivityService`·`src/api/concepts.js` → PR에서 @whans1996 멘션
- 동환 F-16이 `router/index.js`·헤더 nav를 바꾼다 → `App.spec.js`·`navItems.spec.js` 충돌 가능. F-16은 `concept_wishes`를 읽는다
- `codex-review.mjs`가 Codex 실행 실패를 RUNNING으로 기다린다 (#48)
