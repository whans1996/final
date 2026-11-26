# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-27 08:51 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/K-3a-seed-check

## 현재 기능

**K-3a 시드 카드 원본 형식 + 검수 ①형식 ②예제 실행** (`docs/specs/K-3a-seed-check.md`, **APPROVED 11-27**, `Roadmap: K-3a`, 11/26~11/28)

## 진행

- ✅ L-1a #227·HP-3 #230·이슈 정리 ① #233 머지 확인 (로컬 `feature/L-1a-rerun-quiz-regen`은 머지된 낡은 브랜치)
- ✅ spec 초안 AC1~AC11. 결정(11-27 결정로그): 카드별 Markdown + `seed-build.mjs` 생성기, 문법 + 맥락 무관 오류(심볼·모듈·테이블 없음은 경고), 오류만 CI 실패

## 이슈 정리 순서 (11-26 사용자 승인)

① ✅ → K-3a 시드 검수(~11/28) → ② E-1 러너 #218·#219·#221·#223 (12/01 EV-1 전) → HP-4에 ④ #133·#134·#178·#196·#198·#224 흡수 → ③ A 버그 #168(500 먼저)·#200~#203·#155·#156·#150·#121, ⑤ 가드 우회 #188~#191·#193 한 PR. #163→F-27b, #204→K-1, #137→HP-5

## 바로 다음 할 일

1. ✅ spec 승인 커밋·push
2. `/clear` 후 `/implement K-3a` (Opus/high): 순서 = `scripts/seed-build.mjs`·`scripts/lib/seed/`(파서·①형식) 테스트 먼저(`scripts/test/seed-build.test.mjs`·`seed-format.test.mjs`) → 85장 변환(일회 스크립트는 scratchpad, AC2 비교 결과를 PR 본문에) → BE `SeedExampleChecker`·`SeedExampleTest`(com.vibenote.common) → FE 검사·`seed-check.mjs` 보고서 → ci.yml 경로 필터 `data/seed/**`·`data/datasets.json`
3. 11/29부터 HP-4 (#165·#166 + ④ 묶음)

## 막힌 점 / 결정 필요
- 사람: 노션 팀원 초대, F-05b liveTest
- 로드맵에 E-2(D·E 기준표·쌍 비교, 12/09 전) 행 추가 필요

## 주의
- 이 PC Docker 응답 없음(11-27 세션 시작) → BE·Testcontainers 테스트 전에 `docker info`
- 공통: `R__seed_concepts.sql`(생성본이 됨)·`frontend/package.json`(typescript 명시)·`ci.yml`·`SeedConceptsTest` → PR에 @JoonseobKim, CODEOWNERS에 `data/seed/` A 추가
- Bash heredoc·`node -e` 안의 `\`가 줄어든다 → 역슬래시가 든 치환은 Edit 도구로 · 다음 마이그레이션 V15
