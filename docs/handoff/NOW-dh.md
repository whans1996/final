# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-19 15:28 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/common-usecase

## 현재 기능

WBS 1.4 유스케이스 산출물 (`docs/산출물/02-유스케이스.md`, 마감 11/23). F-00b PR #42는 11-19 머지됨

## 진행

- ✅ `diagrams/usecase.puml` 보정: `!pragma layout smetana`(Graphviz 불필요), UC16 내 분석 목록 추가, 비회원→UC03, UC31→UC30 extend 방향 수정
- ✅ `diagrams/usecase.png` 렌더링 (PlantUML 1.2026.8 jar는 scratchpad, 저장소에 넣지 않음. 명령은 02 머리)
- ✅ UC05~UC51 흐름 작성 (UC 27개, 표·흐름·puml ID 일치 확인). spec 있는 UC10~16·20·21은 spec AC 기준, 나머지는 "(기획 기준)" 표시
- 🟡 커밋 → push → `/pr-review`(docs만: verifier 생략, reviewer로 기획서·spec 대조) → PR(develop)

## 바로 다음 할 일

1. `/pr-review` → PR 본문에 `@JoonseobKim` 확인 요청 (B 영역 UC20~23·40~42·51 흐름을 기획서·F-09/F-10 spec 기준으로 씀). CI·Codex COMPLETE 뒤 사람이 머지
2. 머지 뒤 다음 A 기능 `/spec` (F-08 개념 추출이 F-10 "등장 위치"의 데이터 원천, Opus/high 새 세션)

## 막힌 점 / 결정 필요

- ui 점검 계정 `ui-admin@vibe.dev`/`vibe1234`(로컬 DB에만, ADMIN). storageState `.harness/ui-admin.json`
- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부, 김준섭에게 F-00b 머지 알림(Mattermost)

## 주의

- Gradle 테스트를 bootRun과 동시에 돌리지 않는다. 통합 테스트는 Docker Desktop 필요. V1~V5 수정 금지
- Git Bash heredoc·sed·python stdin에서 한글·`\n`·`\|`가 깨진다 → 한글·표(`|`)가 든 수정은 Edit 도구로 쓴다
