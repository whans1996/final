# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-25 17:59 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-26a-index-selection

## 현재 기능

**F-26a 수집·선별 개편** PR #157 Codex ✅ `12dead8` (P2 3건 → #156·#158·#159 이관), CI ✅. 남은 것: spec DONE 표시 커밋 → 사람 머지. spec `docs/specs/F-26a-index-selection.md`

## 진행

- ✅ AC1~AC11 구현 (`a00e0ee`~`68e7dea`), verify all·pre-push 통과
- ✅ PR 전 점검 반영 `05ac577`(Swagger errorCode 설명) · `e7fcd80`(정렬 묶음 1회 판정, 루트 README 우선, `findIndexFiles` README만 content, `AnalysisService.chunks` 100행/400만 자) · `b5cabc6`(spec·UC14·결정로그)
- ✅ Codex 1차 P2 "정규화 경로 중복이 모두 선별됨" → `12dead8` `ZipArchiveReader.supportedEntries`가 처음 것만 담음 (`ZipArchiveReaderTest#AC8_정규화하면_같은_경로인_엔트리는_처음_것만_담는다`), 스레드에 답글
- ✅ Codex 2차 P2 3건(readingOrder 미선별, `*.config.cjs` 미수집, config 상한을 후보 단계에 적용)은 AC·보안 위반이 아니라 이관 → `--resolve` COMPLETE
- 후속 이슈: #155 FE·화면설계서·F-05a "200개 · 10MB" 문구 · #156 readingOrder · #158 cjs 설정 · #159 config 상한 위치

## 바로 다음 할 일

1. `gh pr checks 157`로 CI 초록 확인 → `docs(F-26a): 완료 표시 (spec DONE)`: spec 헤더 상태 `DONE (PR #157)`, 10절 "PR CI 초록"·"상태 DONE" 체크 → push (docs만이라 Codex 재리뷰 없음)
2. 사람이 GitHub에서 PR #157 머지 (상대가 먼저 develop에 머지했으면 `git merge origin/develop` → push → CI 재확인 후)
3. 머지 뒤 `git switch develop && git pull` → 다음 기능. #158·#159는 작아서 `feature/issue-<n>-slug`로 묶어 처리 가능, #155는 F-28 화면 때 흡수

## 막힌 점 / 결정 필요

- 사람: GMS 키 수령(12/01 전), 노션 팀원 초대, F-05b liveTest, 제출 zip 이름

## 주의

- PR #157이 공통 파일(ErrorCode·V12·application.yml·WithdrawalApiTest·openapi.json)을 바꿈 → 본문에 @JoonseobKim 멘션함. 상대가 먼저 머지하면 머지 전 `git merge origin/develop` 후 push. 다음 마이그레이션은 V13
- 커밋 범위는 F-xx|common|harness만 · Git Bash에서 `/pr-review` 같은 인자는 MSYS 경로 변환됨 (`MSYS_NO_PATHCONV=1`)
- node 스크립트로 Java 편집 시 백슬래시가 깨짐 → Edit 도구 사용 · 이 PC에 python 없음
