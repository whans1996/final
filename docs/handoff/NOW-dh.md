# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-20 11:28 · 작업자: whans1996@naver.com · PC: dh-home · 브랜치: feature/F-15-review-plan

## 현재 기능

F-15 복습 계획 — spec `docs/specs/F-15-review-plan.md` APPROVED, **AC1~AC12 구현·테스트 green**, 마무리 검증 전

## 진행

- ✅ BE AC1~AC8: `ReviewPlanController`(GET·POST·PATCH·DELETE `/api/review-plans`), `ReviewPlanService.list·addManual·changeStatus·delete·addWish`, Mapper 6개 쿼리, DTO 4개, ErrorCode `REVIEW_PLAN_404/409`. `ReviewPlanApiTest` 20 · `ReviewPlanServiceTest` 5 · Concurrency 1 green. openapi.json 재생성
- ✅ FE AC9~AC12: `api/reviewPlans.js`, `routes/reviewplan.js`(nav 복습 30), `views/reviewplan/ReviewPlanView.vue`, `components/reviewplan/ReviewPlanItem·ReviewPlanAddButton.vue`, SC-10 버튼 한 줄, SC-08 링크. 관련 vitest 8파일 67개 green
- ✅ CODEOWNERS 4줄, conventions 3절 REVIEW_PLAN 코드
- 참고: AC8은 `addWrongAnswers` 리팩터링 때 `addWish`를 먼저 써서 red 없이 통과, FE 테스트는 구현과 함께 작성(red 미확인)

## 바로 다음 할 일

1. (완료) 커밋 6dd1841(BE)·b31a233(FE) push됨. FE 커밋 때 App.spec 헤더 메뉴 기대값에 복습 추가
2. `contract-checker`로 spec 3절 ↔ openapi.json ↔ `src/api/reviewPlans.js` 비교
3. BE(local)·FE dev 띄우고 `ui-verifier`: `/review-plans` 데스크톱·375px, `/concepts/{id}` 복습에 추가 버튼
4. 산출물 글은 반영함(화면 설계서 SC-11 절·SC-10·SC-08, UC31, ERD). ui-verifier 스크린샷을 `docs/산출물/screens/SC-11-*.png`로 넣고 SC-11 절에 표 추가 → `/pr-review`

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부
- F-12(B) 연동은 이슈 #57 → B의 `/spec F-12`가 `addWish` 호출. F-15 PR 머지가 선행
- 이 PC에서 BE 테스트가 매우 느림(Spring 컨텍스트 기동 447초 사례). Gradle과 vitest를 동시에 돌리지 않는다

## 주의

- 공통 파일 변경 → PR에 @JoonseobKim 멘션: `views/concept/ConceptDetailView.vue`(요약 아래 버튼 한 줄·import), `router/index.js`(import 한 줄), `router/__tests__/routes.spec.js`, `utils/__tests__/navItems.spec.js`, `ErrorCode.java`(REVIEW_PLAN 블록), `openapi.json`, `.github/CODEOWNERS`
- B PR #58(F-11)이 `ConceptDetailView.vue`를 고치는 중 → 머지 순서에 따라 충돌 확인
- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시)
