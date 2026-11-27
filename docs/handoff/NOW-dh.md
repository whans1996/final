# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-27 09:07 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/K-3a-seed-check

## 현재 기능

**K-3a 시드 카드 원본 형식 + 검수 ①형식 ②예제 실행** (`docs/specs/K-3a-seed-check.md`, APPROVED 11-27, `Roadmap: K-3a`, 11/26~11/28)

## 진행

- ✅ AC1·AC3~AC6 파서·생성기·①형식 · ✅ AC2 85장 변환(develop과 85장 전부 동일) · ✅ AC9·AC10 ②FE·`seed-check.mjs` 보고서
- 85장 수정: vue/navigation-guards 라우트 조각이 JS 문법 오류 → `const routes = [...]` (PR 표에). 일회 스크립트는 scratchpad k3a/, develop 원본 사본 `.harness/k3a-develop-seed.sql`

## 이슈 정리 순서 (11-26 사용자 승인)

① ✅ → K-3a 시드 검수(~11/28) → ② E-1 러너 #218·#219·#221·#223 (12/01 EV-1 전) → HP-4에 ④ #133·#134·#178·#196·#198·#224 흡수 → ③ A 버그 #168(500 먼저)·#200~#203·#155·#156·#150·#121, ⑤ 가드 우회 #188~#191·#193 한 PR. #163→F-27b, #204→K-1, #137→HP-5

## 바로 다음 할 일

1. AC7·AC8 BE: `backend/src/test/java/com/vibenote/common/SeedExampleChecker.java`(javac 3단계 감싸기·진단 분류, sql은 IntegrationTestBase.MYSQL root로 카드마다 빈 스키마, xml·yaml) + `SeedExampleCheckerTest`·`SeedExampleTest`(결과 `backend/build/seed-check/result.json`, env SEED_SRC·SEED_CARD) → `node scripts/seed-check.mjs --be`로 85장 오류 0
2. AC11: ci.yml 경로 필터(`data/seed/**`·`data/datasets.json` → be·fe), harness 잡 `seed-build.mjs --check`, verify.mjs fe에 seed-check·seed-examples 테스트, related.mjs areasOf, frontend/package.json typescript 6.0.3
3. 문서: backend/CLAUDE.md 시드 규칙(역슬래시 금지 → 카드 파일·생성기), 산출물 09 DS-05, spec 체크리스트 → /pr-review
4. 11/29부터 HP-4 (#165·#166 + ④ 묶음)

## 막힌 점 / 결정 필요
- 사람: 노션 팀원 초대, F-05b liveTest · 로드맵에 E-2(D·E 기준표·쌍 비교, 12/09 전) 행 추가 필요

## 주의
- 공통: `R__seed_concepts.sql`(생성본)·`SeedConceptsTest`·CODEOWNERS(`/data/seed/`·`/backend/src/test/resources/seed/` A)·`frontend/package.json`·`ci.yml` → PR에 @JoonseobKim
- Bash heredoc·`node -e` 안의 `\`가 줄어든다 → 역슬래시가 든 내용은 Write·Edit 도구로 · 다음 마이그레이션 V15
