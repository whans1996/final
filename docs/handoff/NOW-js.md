# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신: 2026-11-19 14:58 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-09-concept-list

## 현재 기능

F-10 개념 카드 상세 — `docs/specs/F-10-concept-detail.md` **APPROVED** (김준섭 11-19). spec은 F-09 브랜치 `feature/F-09-concept-list`에 함께 커밋해 F-09 PR로 올린다 (F-10 구현은 F-09 머지 뒤 `feature/F-10-concept-detail`)

## 진행

- ✅ F-09 spec 승인(AC 11개), 화면 설계서 B 화면 초안 push. F-09 구현은 F-00b 머지 대기
- ✅ F-10 spec 승인: AC 14개(BE 8·FE 6), `GET /api/concepts/{id}` 공개 + 로그인 시 `appearances`(내 DONE 분석, 최근 10개), `related` 최대 4개, DB 변경 없음
- ✅ 결정(결정로그 11-19): 관련 카드 = 함께 등장 횟수 우선 + 같은 카테고리 인기순 채움 / 본문 Markdown = 자체 최소 렌더러(`v-html` 금지)
- 🟡 js-ssafy 착수 체크리스트: 교육장 PC에서 `/pickup` 확인만 남음

## 바로 다음 할 일

1. F-00b가 develop에 머지되면 `git merge origin/develop` → `/implement F-09` (Opus/high, 첫 테스트 `ConceptListApiTest#AC1_...`)
2. F-09 PR 머지 후 develop에서 `feature/F-10-concept-detail` → `/implement F-10` (첫 테스트 `ConceptDetailApiTest#AC1_...`)

## 막힌 점 / 결정 필요

- F-09·F-10 구현은 F-00b 머지 뒤. W2(11/24)까지 머지되지 않으면 Mattermost로 일정을 정한다
- F-10 리뷰 관련 미정(1인 1리뷰, 길이, 페이지 크기)은 F-11 spec으로 넘김

## 주의

- 공통 코드(SecurityConfig `PUBLIC_GET_PATHS`, 라우터, nav, 공용 컴포넌트)를 바꾸면 PR 본문에서 @whans1996 멘션
- 마이그레이션 만들기 전 `node scripts/next-migration.mjs`. 통합 테스트는 Docker Desktop 필요
