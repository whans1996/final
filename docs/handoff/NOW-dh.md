# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-20 13:35 · 작업자: whans1996@naver.com · PC: dh-home · 브랜치: feature/F-16-recommendation

## 현재 기능

F-16 맞춤 개념 카드 추천 — spec `docs/specs/F-16-recommendation.md` **APPROVED** (whans1996 11-20), 브랜치 `feature/F-16-recommendation` push됨, 구현 전

## 진행

- ✅ F-15 PR #59 머지됨 (develop 0023aeb)
- ✅ /spec F-16 초안: AC10개, `GET /api/recommendations?size=`, V6 `recommendation_reasons.signal_key`, LLM RECOMMEND(동기 1회 배치·실패 시 RULE), SC-04 `/dashboard`(오늘 복습·추천·최근 분석), 로그인 후 `/dashboard`, SC-11 빈 상태 추천 3개
- ✅ 사용자 결정 3건 결정로그 반영 (11-20 F-16 행), spec 승인 커밋

## 바로 다음 할 일

1. `/clear` 후 `/implement F-16` (Opus/high). `node scripts/next-migration.mjs`로 V6 확인 → `V6__recommendation_signal_key.sql`
2. 첫 red: `recommendation/RecommendationScorerTest`(오답 상한 3, levelFit, 동점 순서, signalKey) → `RuleReasonsTest` → `RecommendationApiTest#AC1_…`

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부
- F-12(B) 미착수 → 찜 신호(`wishedCategory`, 찜 카드 제외)는 `concept_wishes`가 빌 때 0. 공통 `ConceptMapper`에 F-16 블록(`findAllPublishedSummaries`, `findWishedByUser`) 추가 예정 → PR에 @JoonseobKim 멘션
- 프로젝트 서브에이전트 미등록 시 general-purpose에 `.claude/agents/*.md`를 따르게 해서 대신함

## 주의

- B는 F-17(커뮤니티) /pr-review 단계. `router/index.js`(공통)를 F-16에서 고친다(`redirectTarget` 기본값, import 한 줄)
- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시)
