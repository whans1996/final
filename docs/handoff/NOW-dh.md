# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-27 16:36 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/issue-249-env-outside

## 현재 기능
**#249 .env 저장소 밖으로·가드 범위 고정** — 브랜치 `feature/issue-249-env-outside` (develop 0c5d87c에서 분기, 아직 커밋 전). #246은 11/27 16:22 머지, Closes 7건 닫힘 확인
## 진행
- ✅ 1 BE: `application.yml` import에 `${user.home}/.vibenote/.env` 추가(루트 `../.env` 다음, 뒤가 이김). `EnvFileImportTest` 3개 green(홈 읽기·없음·순서) · 하네스: `scripts/lib/env-file.mjs`(envFileRow·moveEnv) + `env-file.test.mjs`, env-check 연결, `setup.mjs --move-env`
- ✅ 2 settings.json deny 확장(키 파일·`~/.vibenote/**`, `.env.*`는 deny가 allow를 이겨 안 씀) · ✅ 3 conventions 7절 가드 범위·`AGENTS.md` 신규
- ✅ 4 `codex-stopped` 라벨 → codex-loop가 그 PR을 건너뜀 (codex.mjs STOP_LABEL). 문서·결정로그도 반영
- 🟡 verifier(harness·be) 실행 중
## 바로 다음 할 일
1. `node scripts/verify.mjs be` 통과 확인(EnvFileImportTest 3개 포함) → `chore(harness): .env를 저장소 밖 ~/.vibenote로, 가드 범위 고정·codex-stopped 라벨 (#249)` 커밋
2. `/pr-review`: PR 본문 `Closes #249`, @JoonseobKim 멘션(공통: application.yml·EnvFileImportTest, 두 PC에서 `node scripts/setup.mjs --move-env` 요청). `gh label create codex-stopped` 필요
3. 5번(guard 재귀 검색 판정 단순화)은 4대 PC 이전 뒤로 → follow-up 이슈 생성
4. 11/29부터 P-1a `/spec` (Opus/high)
## 막힌 점 / 결정 필요
- 사람: 노션 팀원 초대, F-05b liveTest · 로드맵 E-2 행
## 주의
- 정규식·이스케이프가 든 수정은 Edit 도구로. scripts/에 prettier 금지 · 다음 마이그레이션 V15 · 인라인 node -e에 .env 문자열을 넣으면 가드가 막음(스크립트 파일로)
