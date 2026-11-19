# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-dh.md`)은 읽기만 한다.

갱신: 2026-11-19 14:39 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/F-09-concept-list

## 현재 기능

F-09 개념 카드 목록·검색 — `docs/specs/F-09-concept-list.md` **APPROVED** (김준섭 11-19) · 브랜치 `feature/F-09-concept-list` (spec 승인 커밋만 있음, 구현 전)

## 진행

- ✅ PR #34(Codex 계정 미연결 ERROR 판정) 머지, js-home 착수 체크리스트 완료 (11-19)
- 🟡 js-ssafy 착수 체크리스트: pcs.json 등록됨, 교육장 PC에서 `/pickup` 확인만 남음
- ✅ F-09 spec 승인: AC 11개, `GET /api/concepts` 공개(PUBLISHED만), 인기순=wish_count→avg_rating→id ASC(기본), 최신순, 키워드=name+summary LIKE(`!` 이스케이프), DB 변경 없음. 목록 DTO `ConceptSummaryResponse` 계약을 spec 3절에서 확정
- ✅ 화면 설계서 SC-09 절 초안(와이어프레임, 스크린샷은 구현 PR에서) — 이 브랜치에 커밋

## 바로 다음 할 일

1. 김동환에게 Mattermost로 공유: 공통 기반 PR의 `ConceptSummaryResponse`를 spec 3절 필드(id·name·category·difficulty·summary·wishCount·avgRating)대로 만들어 달라고
2. 공통 기반 PR이 develop에 머지되면 `git merge origin/develop` 후 `/implement F-09` (첫 테스트 `ConceptListApiTest#AC1_...`)
3. 그동안 W2 산출물 `06-화면설계서.md`의 나머지 B 화면 초안: SC-10 카드 상세, SC-14~16 커뮤니티, SC-17 피드, SC-20 관리자 카드 (11/24~28)

## 막힌 점 / 결정 필요

- F-09 구현은 A의 concept 공통 기반 PR(결정로그 11-19) 머지 뒤 시작. 아직 시작 전

## 주의

- 공통 코드(SecurityConfig `PUBLIC_GET_PATHS`, 라우터, nav, 공용 컴포넌트)를 바꾸면 PR 본문에서 @whans1996 멘션
- 마이그레이션 만들기 전 `node scripts/next-migration.mjs`. 통합 테스트는 Docker Desktop 필요
