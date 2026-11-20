# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-20 09:24 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-11-concept-review

## 현재 기능

F-11 카드 리뷰 (`docs/specs/F-11-concept-review.md` **APPROVED 11-20**). F-10은 PR #52로 머지 완료

## 진행

- ✅ spec 초안: BE AC1~AC12(목록·작성·`/me` 조회·수정·삭제, 평점 재계산, 409·404·401), FE AC13~AC18(SC-10 리뷰 절)
- ✅ spec 승인 (김준섭 2026-11-20)
- ✅ 결정(사용자 답): 수정·삭제 포함(`/reviews/me`), 내용 필수 1~200자, "더 보기" 10개씩 → 결정로그 추가
- 마이그레이션 없음. 새 에러 코드 `CONCEPT_001`(내 리뷰 없음 404)·`CONCEPT_409`(중복)

## 바로 다음 할 일

1. `/clear` 후 새 세션(Opus/high)에서 `/implement F-11`. 첫 테스트: `backend/src/test/.../concept/review/ConceptReviewApiTest#AC1_...` (12개 리뷰 페이지 2쪽·범위 밖·0건)
2. AC12에서 `PUBLIC_GET_PATHS`의 `/api/concepts/{id:\\d+}/reviews`가 `/reviews/me`를 공개로 열지 않는지 확인

## 막힌 점 / 결정 필요

- 없음

## 주의

- concept 패키지·`components/concept`·`src/api/concepts.js`·`SecurityConfig`·`ErrorCode`는 CODEOWNERS상 공통 → PR 본문에 @whans1996 멘션
- F-04 피드는 지워진 리뷰의 REVIEW 활동을 `concept_reviews` JOIN으로 걸러야 함 (F-04 spec에 반영)
- develop(김동환 PR #53 F-08 mock fixture 머지분)을 `fbcb1c9`에서 합침
