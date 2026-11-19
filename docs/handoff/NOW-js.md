# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-19 14:44 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-09-concept-list

## 현재 기능

F-09 개념 카드 목록·검색 — `docs/specs/F-09-concept-list.md` **APPROVED** (김준섭 11-19) · 브랜치 `feature/F-09-concept-list` (spec·화면 설계서 커밋만 있고 구현 전)

## 진행

- 🟡 js-ssafy 착수 체크리스트: pcs.json 등록됨, 교육장 PC에서 `/pickup` 확인만 남음
- ✅ F-09 spec 승인: AC 11개, `GET /api/concepts` 공개(PUBLISHED만), DB 변경 없음
- ✅ `ConceptSummaryResponse` 필드 일치 확인: F-00b spec AC4(origin/feature/F-00b-common-foundation)와 F-09 spec 3절 필드가 같음 → 김동환에게 따로 공유할 필요 없음
- ✅ 화면 설계서 B 화면 와이어프레임 초안: SC-09, SC-10 카드 상세, SC-14~16 커뮤니티, SC-17 피드, SC-20 관리자 카드 (커밋 881f314, push 전). 각 절의 "spec에서 정할 것"에 미정 사항을 적어 둠 (관련 카드 기준, 1인 1리뷰, 첨부 범위, SC-07 진입 버튼은 A와 협의 등)

## 바로 다음 할 일

1. 이 브랜치 push (`git push`). 화면 설계서 커밋은 F-09 구현 PR에 함께 올린다
2. F-00b(A 공통 기반)가 develop에 머지되면 `git merge origin/develop` → `/implement F-09` (Opus/high, 첫 테스트 `ConceptListApiTest#AC1_...`)
3. 그 전에 시간이 남으면 `/spec F-10` (카드 상세): SC-10의 "spec에서 정할 것" 3가지(관련 카드 기준, 리뷰 정책, 페이지 크기)를 결정 필요로 가져간다

## 막힌 점 / 결정 필요

- F-09 구현은 F-00b 머지 뒤 시작한다. 11-19 14:40 기준 F-00b 브랜치에는 spec 승인 커밋만 있음. W2(11/24)까지 머지되지 않으면 Mattermost로 일정을 정한다

## 주의

- 공통 코드(SecurityConfig `PUBLIC_GET_PATHS`, 라우터, nav, 공용 컴포넌트)를 바꾸면 PR 본문에서 @whans1996 멘션
- 마이그레이션 만들기 전 `node scripts/next-migration.mjs`. 통합 테스트는 Docker Desktop 필요
