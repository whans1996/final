# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-26 13:57 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/HP-2-roadmap

## 현재 기능

**HP-2 트랙 4 PR ② 로드맵 R-1** (기획서 7.7절) — 구현 4f26a63 + reviewer 반영, verifier all ✅ → push·PR 생성 중. 사용자 지시(트랙 4): 머지 직전까지 자동, 머지는 사람이 GitHub에서

## 진행

- ✅ PR ① #192 머지(11-26 13:30), #130·#135·#136·#139 닫힘. #192·#175 본문에 `Roadmap: HP-1`·`D-1` 추가
- ✅ `docs/로드맵.md` 정본(항목 47·넘겨주는 시점 10·공통 일정 4, 점검 2절에서 옮김)
- ✅ `scripts/lib/roadmap.mjs`(파싱·상태 계산·배너·블록·nextItem·newlyOverdue) + `scripts/roadmap.mjs` CLI + `roadmap.test.mjs` 21개
- ✅ SessionStart 배너 🗺️·🤝·⚠️·📅 + 컨텍스트 "## 로드맵" 블록, `related.mjs` 로드맵 문서 → 하네스 영역
- ✅ `.github/workflows/roadmap.yml`: develop push 때 새로 지난 넘겨주기를 Mattermost로 한 번 (schedule은 기본 브랜치 main만 돌아 못 씀)
- ✅ PR 템플릿 `Roadmap:` 줄, /pr-review·/pickup·/handoff·conventions(브랜치 `feature/<ID>-slug`)·CLAUDE.md
- ✅ reviewer 반드시 수정 1(커밋·PR 제목 범위에 로드맵 ID: commit-msg SCOPE·ci.yml) + 권장 1~9(펜스 코드 무시, --search 제거, 조회 실패 때 지난 넘겨주기 접기, ID 대소문자, nextItem after, --check-body, 백필 안내, EV-1 뒤 머지 표기)
- 🟡 PR 설명 `.harness/pr-HP-2.md` → push → PR → Codex 루프

## 바로 다음 할 일

1. PR(HP-2) Codex 루프: `node scripts/codex-review.mjs <번호> --request --wait --status` (백그라운드) → COMPLETE면 사람 머지
2. 머지 뒤 `node scripts/roadmap.mjs --next --after HP-2` → HP-3 세션 시작·표시 (H-5~7, HX-6·7, reviewer 권장 10: roadmap `unmergedRemoteBranches` 동기 git을 H-5 시간 예산에 포함)
3. dh-home에서 `node scripts/setup.mjs` 한 번 (eval fetch 제외 refspec)

## 막힌 점 / 결정 필요
- 사람: dh-home git `user.name` 통일(H-7), 노션 팀원 초대, F-05b liveTest · eval 기록 브랜치 `eval/` 접두사를 김준섭에게 알리기 · 김준섭 NOW의 V-2 "12/03 고정"이 로드맵(7.5절 합의 11/28)과 다름 → 확인

## 주의
- 다음 마이그레이션은 V14 · node heredoc으로 파일 고치면 `$'`·`\r` 이스케이프가 깨짐 → Edit 도구 · 이 PC에 python 없음
- 로드맵 상태는 `gh pr list --state all`(검색 색인 안 씀)로 계산한다. 커밋 범위에 로드맵 ID 사용 가능(`feat(K-3a): …`)
