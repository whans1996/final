# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-26 21:37 · 작업자: whans1996@naver.com · PC: dh-home · 브랜치: feature/HP-3-session-display

## 현재 기능

**HP-3 하네스 PR ③ 세션 시작·표시** (H-5~7, HX-6·7, 범위는 `docs/기획서.md` 7.7절, 하네스라 spec 없음, `Roadmap: HP-3`). L-1a PR #227은 머지됨

## 진행

- ✅ H-5 시간 예산 `scripts/lib/budget.mjs` (11초, 모자라면 fetch·자동 병합·gh 대기·내 브랜치 조회 건너뜀, `autoSync({ timeLeft })`, peer-watch도). 예산 0초로도 NOW 주입 확인
- ✅ H-6 `sync.mjs` `dependencyActions`·`mergeNotes`: package*.json·Gradle 설정 병합 시 ⚠️ npm ci·bootRun 재시작
- ✅ H-7 `peerDigest`가 develop 첫 부모(PR 병합 커밋, GitHub 계정) 작성자로 판정, `env-check` `gitUserRow`가 user.name ≠ pcs.json github이면 ⚠️
- ✅ HX-7 `peer.peerWatchBanner` → peer-watch systemMessage (사람 화면 표시는 실제 세션에서 미확인)
- ✅ HX-6 상태 표시줄 `.claude/hooks/statusline.mjs`(settings.json statusLine, 사용자 결정: 프로젝트 설정·개인 표시줄 정보 포함) + `scripts/lib/status.mjs` + `scripts/status-refresh.mjs`(백그라운드, `.harness/status.json`)
- 테스트: budget·status 새 파일, sync·peer·env-check·settings 추가, issues·roadmap의 session-start 순서 테스트를 새 호출 형태로. `verify.mjs harness` 통과, 커밋 ef8645b (push 전)

## 바로 다음 할 일

1. `/pr-review` (reviewer, PR 본문 `Roadmap: HP-3`, conventions·CLAUDE.md 공통 문서 변경이라 @JoonseobKim 멘션)
2. 새 세션에서 상태 표시줄 두 줄·peer-watch 화면 알림(systemMessage)이 실제로 보이는지 확인
3. 머지 뒤 HP-4 (#165·#166, H-9~12)

## 막힌 점 / 결정 필요
- 사람: dh-home `git config user.name whans1996` (H-7 경고가 이제 세션 시작에 뜬다), 노션 팀원 초대, F-05b liveTest
- 로드맵에 E-2(D·E 기준표·쌍 비교, 12/09 전) 행 추가 필요 · `branches.mjs` 로드맵 ID 거부는 #213

## 주의
- 이 PC는 git 1회가 약 0.1초라 세션 시작이 8.4초 (예산 11초 안). 하네스 테스트도 느림(sync·peer 2분+)
- Bash heredoc 안의 `\\`가 `\`로 줄어든다 → 역슬래시가 든 치환은 Edit 도구로
- 다음 마이그레이션은 V15
