# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-19 11:50 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/common-codex-loop

## 현재 기능

Codex 리뷰 루프 강제 (common, 하네스) — cdb7440 push, reviewer 반드시 1·권장 7 반영, 테스트 34/34. PR 생성 전(사용자 확인 대기)

## 진행

- ✅ 원인 확인: 김준섭 PR #28·#30에 Codex 자동 리뷰 없음 = JoonseobKim 계정이 Codex 미연결 (봇 "create a Codex account" 안내, 동환 계정 요청은 정상)
- ✅ `lib/codex.mjs` setupNotice(env·account)·requestCount + 테스트, `codex-review.mjs` 계정 미연결 ERROR·요청 후 RUNNING·`--status`·`(재리뷰 n회)`
- ✅ Stop hook `.claude/hooks/codex-loop.mjs`(settings Stop 등록), `.github/workflows/codex-status.yml`
- ✅ 문서: pr-review 8·11단계(상한 5회), pickup, CLAUDE.md, 환경설정 8절, 결정로그. 하네스 테스트 30/30

## 바로 다음 할 일

1. PR 생성: `gh pr create --base develop --title "chore(harness): Codex 리뷰 루프 강제"` (본문에 @JoonseobKim 멘션, 환경설정 8절 계정 연결 요청) → `node scripts/codex-review.mjs <PR> --request --wait --status` 백그라운드
2. PR에서 `Codex status` 워크플로가 ⏳ 체크를 다는지, `node scripts/codex-review.mjs <PR> --request --wait --status`가 ✅/❌로 바꾸는지 `gh pr checks`로 확인
3. 김준섭: chatgpt.com Codex에 GitHub 연결 + Code review·Automatic reviews 켜기 (사람)
4. `/clear` 뒤 `/spec`으로 **공통 기반** spec. 결정 필요 목록은 결정로그 11-19 공통 기반 행 참고
5. 1.4 유스케이스(마감 11/23) `docs/산출물/02-유스케이스.md`

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부, 김준섭 Codex 계정 연결

## 주의

- Gradle 테스트를 bootRun과 동시에 돌리지 않는다. 통합 테스트는 Docker Desktop 필요. V1~V5 수정 금지
- 이 PC는 `node scripts/setup.mjs` 전체 점검 기록 없음
