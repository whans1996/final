# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-20 13:52 · 작업자: whans1996@naver.com · PC: dh-home · 브랜치: feature/F-16-recommendation

## 현재 기능

F-16 맞춤 개념 카드 추천 — spec `docs/specs/F-16-recommendation.md` APPROVED, `/implement` 진행 중. 이번 세션 목표: BE AC1~AC7 → FE AC8~AC10

## 진행

- ✅ BE AC1~AC7 green (RecommendationApiTest 10, Scorer·RuleReasons·Parser 단위, MockLlmClient linkRecommendIds), verify be 통과, openapi.json 갱신 → 커밋
- ⏳ FE AC8~AC10: 테스트 작성됨(api/recommendations, RecommendationCard·List, DashboardView spec), 컴포넌트 구현 전

## 바로 다음 할 일

1. FE 구현: `src/api/recommendations.js`, `components/recommendation/RecommendationCard.vue`·`RecommendationList.vue`(props title·size·hideOnEmptyOrError), `views/dashboard/DashboardView.vue`, `router/routes/dashboard.js`(nav order 1) + `router/index.js` import·`redirectTarget` 기본 `/dashboard`
2. 기존 테스트 갱신: authGuard.spec(기본 `/dashboard`), LoginView.spec "redirect 없으면", navItems.spec(대시보드 맨 앞), routes.spec; ReviewPlanView 빈 상태 아래 추천 3개(AC10, 처음 빈 상태일 때 한 번만 마운트)

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부
- 공통 `ConceptMapper`에 F-16 블록(`findAllPublishedSummaries`, `findWishedByUser`) 추가 → PR에 @JoonseobKim 멘션
- backend/CLAUDE.md MyBatis 절은 "추천 집계는 삭제 분석을 거르지 않는다"인데 F-16 spec(승인)은 거른다 → spec을 따르고 CLAUDE.md 문구를 고친다

## 주의

- `router/index.js`(공통)를 F-16에서 고친다(`redirectTarget` 기본값, import 한 줄) → PR @JoonseobKim
- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시)
