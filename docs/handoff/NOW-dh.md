# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-25 19:18 · 작업자: whans1996@naver.com · PC: dh-home · 브랜치: feature/F-26a-index-selection

## 현재 기능

**F-26a 수집·선별 개편** PR #157 — CI ✅ · Codex COMPLETE · spec DONE 표시 완료 → **사람 머지 대기**. spec `docs/specs/F-26a-index-selection.md`

## 진행

- ✅ AC1~AC11 구현, PR 전 리뷰 반영, Codex 1차 P2 수정 `12dead8`, 2차 P2 3건 이관(#156·#158·#159)
- ✅ spec 헤더 `DONE (PR #157)`, 10절 "PR CI 초록"·"상태 DONE" 체크 (docs만이라 Codex 재리뷰 없음)
- 후속 이슈: #155 FE·화면설계서·F-05a "200개 · 10MB" 문구 · #156 readingOrder · #158 cjs 설정 · #159 config 상한 위치

## 바로 다음 할 일

1. `gh pr checks 157` 초록 재확인 → 사람이 GitHub에서 PR #157 머지 (상대가 먼저 develop에 머지했으면 `git merge origin/develop` → push → CI 재확인 후)
2. 머지 뒤 `git switch develop && git pull` → 새 세션에서 #158·#159를 `feature/issue-158-config-collect` 하나로 묶어 처리 (수집 필터에 `*.config.cjs` 추가, config 상한을 후보 단계가 아니라 실제 선별에 적용). #155는 F-28 화면 때 흡수, #156은 F-26b에서
3. 11/27 Figma 세션 → 11/29~ V-1 MVP 감사

## 막힌 점 / 결정 필요

- 사람: GMS 키 수령(12/01 전), 노션 팀원 초대, F-05b liveTest, 제출 zip 이름

## 주의

- PR #157이 공통 파일(ErrorCode·V12·application.yml·WithdrawalApiTest·openapi.json)을 바꿈 → 본문에 @JoonseobKim 멘션함. 다음 마이그레이션은 V13
- 커밋 범위는 F-xx|common|harness만 · Git Bash에서 `/pr-review` 같은 인자는 MSYS 경로 변환됨 (`MSYS_NO_PATHCONV=1`)
- node 스크립트로 Java 편집 시 백슬래시가 깨짐 → Edit 도구 사용 · 이 PC에 python 없음
