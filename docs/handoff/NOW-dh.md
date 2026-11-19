# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-19 15:36 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/common-usecase

## 현재 기능

WBS 1.4 유스케이스 산출물 — **PR #43** (develop). CI 초록, **Codex ✅ 12c2c2c**(재리뷰 1회) → 사람이 GitHub에서 머지만 남음. F-00b PR #42는 11-19 머지됨

## 진행

- ✅ `98b047e` puml 보정(smetana, UC16, 비회원 조회), png 렌더링, UC05~UC51 흐름 (UC 27개)
- ✅ reviewer 반드시 2·권장 6 반영 `12c2c2c`: UC22·41 비회원 조회, UC24·UC30 실패 처리는 F-08·F-14 spec 결정으로, UC14·15 구현 기준, 01 요구사항 F-05/F-06 행·결정로그(내 분석 목록 = F-05a)
- PlantUML jar는 저장소 밖(Maven Central `net.sourceforge.plantuml:plantuml:1.2026.8`), 명령은 02 머리

## 바로 다음 할 일

1. 사람: PR #43 머지 (wip 없음). 머지 전 develop이 앞서 있으면 `git merge origin/develop` → push
2. 머지 뒤 `git switch develop && git pull`, 다음 A 기능 `/spec F-08` 개념 추출·연결 (Opus/high 새 세션). F-10 "등장 위치"(`analysis_concepts`)의 데이터 원천이고, UC24 실패 처리(단계 FAILED vs 연결 없이 완료)를 spec "결정 필요"에 올린다

## 막힌 점 / 결정 필요

- ui 점검 계정 `ui-admin@vibe.dev`/`vibe1234`(로컬 DB에만, ADMIN). storageState `.harness/ui-admin.json`
- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부, 김준섭에게 F-00b 머지 알림(Mattermost)

## 주의

- Gradle 테스트를 bootRun과 동시에 돌리지 않는다. 통합 테스트는 Docker Desktop 필요. V1~V5 수정 금지
- Git Bash heredoc·sed·python stdin에서 한글·`\n`·`\|`가 깨진다 → 한글·표(`|`)가 든 수정은 Edit 도구로 쓴다
