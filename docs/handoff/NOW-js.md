# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신: 2026-11-20 09:33 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-11-concept-review

## 현재 기능

F-11 카드 리뷰 (`docs/specs/F-11-concept-review.md` APPROVED 11-20). `/implement` 진행 중

## 진행

- ✅ BE AC1~AC12 green, 커밋 `feat(F-11): BE 카드 리뷰 API` (`concept/review/` Controller·Service·Mapper(+XML)·DTO 4종, `ConceptReviewApiTest` 18개 + `ConceptReviewServiceTest`)
- ✅ ErrorCode `CONCEPT_001`·`CONCEPT_409`, `PUBLIC_GET_PATHS` 리뷰 목록, conventions 3절, openapi.json
- ⏳ FE AC13~AC18

## 바로 다음 할 일

1. FE: `src/api/concepts.js`에 `fetchReviews`·`fetchMyReview`(204→null)·`createReview`·`updateReview`·`deleteReview` + `api/__tests__/concepts.spec.js`
2. `components/concept/StarRating.vue`·`ReviewForm.vue`·`ConceptReviews.vue` + `__tests__/ConceptReviews.spec.js`·`ReviewForm.spec.js`
3. `views/concept/ConceptDetailView.vue` 헤더 "· 리뷰 N" + 리뷰 절, `views/__tests__/ConceptDetailView.spec.js` AC18

## 막힌 점 / 결정 필요

- 없음

## 주의

- concept 패키지·`components/concept`·`src/api/concepts.js`·`SecurityConfig`·`ErrorCode`는 CODEOWNERS상 공통 → PR 본문에 @whans1996 멘션
- 개념 카드 Tag 설명을 `ConceptReviewController.TAG_DESCRIPTION` 상수로 두 Controller가 공유 (설명 "…·리뷰"로 바뀜)
- 통합 테스트는 트랜잭션 하나라 같은 MyBatis 쿼리를 JDBC insert 뒤 다시 부르면 1차 캐시가 답한다 (AC7 테스트 주석)
- F-04 피드는 지워진 리뷰의 REVIEW 활동을 `concept_reviews` JOIN으로 걸러야 함
