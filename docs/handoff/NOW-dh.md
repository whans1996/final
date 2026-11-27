# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-27 09:58 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/K-3a-seed-check

## 현재 기능

**K-3a 시드 카드 원본 형식 + 검수 ①형식 ②예제 실행** (`docs/specs/K-3a-seed-check.md`, APPROVED 11-27, `Roadmap: K-3a`, 11/26~11/28)

## 진행

- ✅ AC1~AC6·AC9·AC10 커밋·push (e716b40). 85장 develop과 동일, 수정 1건(vue/navigation-guards 라우트 조각 → `const routes = [...]`)
- ✅ AC7·AC8·AC11 커밋 (5e8d439·c549729). /pr-review: verifier 지적(seed-check --be UP-TO-DATE, baed368), reviewer 반드시 수정 2(javac 흐름 분석 생략·@Override 완화 범위, efb6957), 권장 전부(지어낸 API 오류·SQL 격리·DB 한도·ConceptCategories 필터, f4e6293, 결정로그) 반영. 85장 오류 0·경고 437. PR 설명 `.harness/pr-K-3a.md`

## 이슈 정리 순서 (11-26 사용자 승인)

① ✅ → K-3a 시드 검수(~11/28) → ② E-1 러너 #218·#219·#221·#223 (12/01 EV-1 전) → HP-4에 ④ #133·#134·#178·#196·#198·#224 흡수 → ③ A 버그 #168(500 먼저)·#200~#203·#155·#156·#150·#121, ⑤ 가드 우회 #188~#191·#193 한 PR. #163→F-27b, #204→K-1, #137→HP-5

## 바로 다음 할 일

1. push → `gh pr create --base develop --title "feat(K-3a): 시드 카드 원본 형식과 검수 ①형식 ②예제 실행" --body-file .harness/pr-K-3a.md` → CI·Codex 루프(`node scripts/codex-review.mjs <PR> --request --wait --status`)
2. COMPLETE·CI 초록이면 `docs(K-3a): 완료 표시 (spec DONE)` → 머지는 GitHub에서 직접
3. 11/29부터 HP-4 (#165·#166 + ④ 묶음)

## 막힌 점 / 결정 필요
- 사람: 노션 팀원 초대, F-05b liveTest · 로드맵에 E-2(D·E 기준표·쌍 비교, 12/09 전) 행 추가 필요

## 주의
- 공통: `R__seed_concepts.sql`(생성본)·`SeedConceptsTest`·CODEOWNERS(`/data/seed/`·`/backend/src/test/resources/seed/` A)·`frontend/package.json`·`ci.yml` → PR에 @JoonseobKim
- Bash heredoc·`node -e` 안의 `\`가 줄어든다 → 역슬래시가 든 내용은 Write·Edit 도구로 · 다음 마이그레이션 V15
