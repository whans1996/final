# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-26 13:25 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/common-harness-guard-1126

## 현재 기능

**트랙 4 PR ① 보안·가드** (기획서 7.7절, H-1~4·HX-1~3 + #130·#135·#136·#139) — **PR #192** 열림, head 4f3c2c4, CI 진행 중, Codex 리뷰 대기(`codex-review.mjs 192 --request --wait --status`). 사용자 지시: 머지 직전까지 자동, 머지는 사람이 GitHub에서

## 진행

- ✅ 새 lib: `protect.mjs`(비밀값 패턴·비밀 파일·eval 경로), `file-guard.mjs`(파일 도구 판정), `commit-msg.mjs`(메시지 판정, #136) + 테스트 각각
- ✅ `guard.mjs`: `checkTool`(MCP start_process·interact_with_process), H-3 checkout -f·switch -f·디렉터리 restore·stash drop/clear, H-4 grep 패턴 제외, HX-1 `node -e`·`python -c`·`--env-file`·코드 속 셸 명령, HX-2 eval 경로·clone, #130 `gh api`·curl 쓰기, #139 서브셸 cwd 범위
- ✅ hook: guard-bash(원문 미기록 #135)·guard-files(MCP·Read·Grep·Glob)·commit-msg(비밀값 검사) · settings.json matcher + `settings.test.mjs` · CODEOWNERS test llm-fixtures + `codeowners.test.mjs`(HX-3)
- ✅ setup·env-check: 김동환 PC fetch에 `^refs/heads/eval/*` (dh-ssafy 적용됨). 결정로그·09-데이터셋 5절 반영
- ✅ reviewer: 반드시 수정 3(PowerShell 괄호 범위 퇴행, `gh api -XPUT`, `node -pe`)·권장 다수 반영(4f3c2c4), 넘긴 것 #188~#191
- ✅ **Codex ✅ 6c2c91a** (재리뷰 3회: 1·2회차 P2 5건 수정, 3회차 P2 3건 → #189·#193 이관, `--resolve`). CI 첫 BE 실패는 spotless 일시 오류(재실행 통과)
- 보류 이슈 #178~#182 (중복 #183~#187은 닫음)

## 바로 다음 할 일

1. PR #192: CI 초록 + Codex COMPLETE → **사람이 GitHub에서 머지** (머지 전 develop에 새 커밋이 있으면 `git merge origin/develop` → push → CI 재확인). 머지 뒤 `git switch develop && git pull`
2. dh-home에서 `node scripts/setup.mjs` 한 번 (eval fetch 제외 refspec)
3. 트랙 4 PR ② 로드맵 R-1: `docs/로드맵.md` + `scripts/lib/roadmap.mjs` + SessionStart·`/pickup`·`/handoff` 표시 → ③ → ④ → ⑤ (기획서 7.7절)

## 막힌 점 / 결정 필요
- 사람: dh-home git `user.name` 통일(H-7), 노션 팀원 초대, F-05b liveTest · eval 기록 브랜치 `eval/` 접두사를 김준섭에게 알리기

## 주의
- 다음 마이그레이션은 V14 · node heredoc으로 파일 고치면 `$'`·`\r` 이스케이프가 깨짐 → Edit 도구 · 이 PC에 python 없음
