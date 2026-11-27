# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-27 16:51 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/issue-249-env-outside

## 현재 기능

**#249 .env 저장소 밖으로·가드 범위 고정** — 브랜치 `feature/issue-249-env-outside` (f27f158 + reviewer 반영 커밋 중). 아직 push·PR 전

## 진행

- ✅ 제안 1~4 구현(f27f158): application.yml import(루트 → `~/.vibenote`), env-file.mjs·`setup --move-env`, settings deny, conventions 7절·AGENTS.md, `codex-stopped` 라벨 skip
- ✅ verify all 통과(FE는 `npm install`로 tailwind 의존성 채운 뒤. `npm ci`는 oxlint .node 잠김으로 EPERM)
- ✅ reviewer 반영: guard가 AI의 codex-stopped 라벨 추가 차단(guard.test), 키 단위 병합 문구, moveEnv 실패 처리(env-file.test 4개), yml 순서 단언(EnvFileImportTest 4개), AGENTS.md 면제 축소, 환경설정 Windows 경로
- ✅ 이 PC `.env` → `C:\Users\SSAFY\.vibenote\.env` 이동 완료 (머지 전에는 이 브랜치에서만 BE 기동)

## 바로 다음 할 일

1. reviewer 반영 커밋 hook 결과 확인 (`git log --oneline -1`이 fix(harness): codex-stopped 라벨은 사람만…)
2. follow-up 이슈 생성(area:common): 4대 PC 이전 체크리스트 + 이전 끝나면 application.yml `../.env` 줄 삭제·envFileRow root 분기 정리·guard 재귀 검색 판정 단순화(#249 제안 5)
3. 사용자 확인 후 `gh label create codex-stopped` → push → `gh pr create --base develop` (본문 `.harness/pr-issue-249.md`, `Closes #249`, @JoonseobKim 멘션: application.yml·settings.json 공통, 머지 뒤 `--move-env`)
4. Codex 루프(/pr-review 8단계) → 11/29부터 P-1a `/spec`

## 막힌 점 / 결정 필요
- 사람: 노션 팀원 초대, F-05b liveTest · 로드맵 E-2 행

## 주의
- 인라인 node -e에 .env 문자열을 넣으면 가드가 막음(스크립트 파일로) · scripts/에 prettier 금지 · 다음 마이그레이션 V15 · 하네스 빈틈: switch로 package.json이 바뀌어도 npm ci 안내 없음(이슈 후보)
