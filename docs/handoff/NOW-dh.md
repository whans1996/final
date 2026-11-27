# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-27 16:18 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/issue-188-guard-bypass

## 현재 기능

**⑤ 가드 우회 묶음 PR #246** — 브랜치 `feature/issue-188-guard-bypass` (코드 head 4276394 + 문서 커밋). `Closes #188 #189 #190 #191 #193 #245 #248`. **사용자 결정으로 Codex 루프 중단(7회차까지 반영, 판정 STALE) → CI 초록이면 사람이 머지** (결정로그 11-27)

## 진행

- ✅ 이슈 7건 + reviewer 12건 + Codex 7회 재리뷰 지적 반영. guard 테스트 55개·하네스 전체 통과, 재판정 19,026개 차이 14건(의도·하위 폴더·문자열 예시)
- ✅ 리뷰가 수렴하지 않는 원인 정리: .env가 저장소 안 + 셸 문자열 해석. sandbox는 Windows 네이티브 미지원(claude-code-guide 확인)
- ✅ 다음 작업 이슈 #249 (.env를 `~/.vibenote/`로, permissions.deny, 가드 범위 고정·AGENTS.md, codex-loop 사람 중단 표시)

## 바로 다음 할 일

1. 사람이 GitHub에서 #246 머지 (CI 초록 확인. Codex 체크는 STALE이어도 필수 체크 아님). 머지 뒤 Closes 7건 닫힘 확인
2. #249 `/pickup` → `feature/issue-249-env-outside`: 김준섭 PC도 .env를 옮겨야 하므로 PR에서 @JoonseobKim 멘션. `application.yml` `spring.config.import`에 `${user.home}/.vibenote/.env` 먼저, 루트 `../.env`는 전환 기간 유지
3. 11/29부터 P-1a `/spec` (Opus/high)

## 막힌 점 / 결정 필요
- 사람: 노션 팀원 초대, F-05b liveTest · 로드맵에 E-2 행 추가 필요

## 주의
- codex-loop Stop hook이 #246 STALE로 재요청을 지시할 수 있음: 사용자 결정으로 무시 (#249 4번에서 hook 고침)
- 정규식·이스케이프가 든 수정은 Edit 도구로. scripts/에 prettier를 돌리지 않는다 · 다음 마이그레이션 V15
