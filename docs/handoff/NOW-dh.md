# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-20 10:05 · 작업자: whans1996@naver.com · PC: dh-home · 브랜치: feature/F-15-review-plan

## 현재 기능

F-15 복습 계획 — spec `docs/specs/F-15-review-plan.md` **APPROVED (whans1996 11-20)**, 구현 전

## 진행

- ✅ F-14 PR #56 머지(11-20 09:38), develop CI 초록
- ✅ F-15 spec 초안: AC1~AC12 (GET 기간·밀린 복습, POST MANUAL, PATCH 완료·되돌림, DELETE, `addWish` for F-12, SC-11, SC-10 버튼, 헤더 nav·SC-08 링크). 마이그레이션 없음
- ✅ 사용자 결정 4건 → 결정로그 11-20 행 (찜은 F-12가 addWish 호출, 직접 추가는 SC-10 버튼, 밀린 복습 맨 위, 대시보드는 F-16)

## 바로 다음 할 일

1. `/clear` 후 새 세션에서 `/implement F-15` (Opus/high). 브랜치 `feature/F-15-review-plan` (spec 승인 커밋 push됨)
2. 순서: ErrorCode `REVIEW_PLAN_404/409` → `reviewplan/ReviewPlanApiTest#AC1_기간을_주지_않으면_오늘부터_7일을_돌려준다`부터 red → Mapper `findInRange`·`findOverdue` → Service `list` → Controller

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부
- F-12(B)에 `addWish` 호출을 알려야 함 → spec 승인 push 후 Mattermost로 공유

## 주의

- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시)
- 공통 파일 `views/concept/ConceptDetailView.vue`를 B가 F-11에서 고치는 중 → 요약 아래 한 줄만 추가, PR @JoonseobKim 멘션
- Gradle 테스트를 bootRun과 동시에 돌리지 않는다
