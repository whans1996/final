# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-19 14:15 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/common-js-harness-docs

## 현재 기능

하네스 (common) 김준섭 사용 감사 후속 — `feature/common-js-harness-docs` (커밋 완료, PR 전)

## 진행

- ✅ PR #40(이슈 순환) 머지됨
- ✅ 감사: js 입력으로 사람 판별·영역·이슈·peer-watch·notify 함수 확인, macOS 분기 정상, 실깨짐 없음
- ✅ `stamp.mjs --area`(내 이슈 영역 라벨) + handoff·pr-review 스킬·conventions 9절·CLAUDE.md 안내, prompt-evaluator `./gradlew` 안내, develop에 잘못 머지된 `scripts/tmp-codex-probe.mjs` 삭제. 하네스 테스트 43/43

## 바로 다음 할 일

1. `/pr-review` → PR (공통 아님·하네스. 본문에 PR #37 오머지 경위와 @JoonseobKim 안내)
2. `/clear` 뒤 `/spec`으로 **공통 기반** spec (concept 도메인·Mapper·카드 DTO, ErrorCode·ActivityType 블록, 라우터·nav 분리, ConceptCard·Pagination, 관리자 레이아웃)
3. 1.4 유스케이스(마감 11/23) `docs/산출물/02-유스케이스.md`

## 막힌 점 / 결정 필요

- 김준섭: PR #37("머지 금지")이 14:13 develop에 머지됨 → NOW-js.md 끝 `<!-- [임시] ... -->` 줄은 본인이 지울 것. 무료 private 저장소라 브랜치 보호 없음(403) — "머지 금지" 가드 필요 여부 결정
- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부, 이 PC `node scripts/setup.mjs` 전체 점검 1회

## 주의

- Gradle 테스트를 bootRun과 동시에 돌리지 않는다. 통합 테스트는 Docker Desktop 필요. V1~V5 수정 금지
- Git Bash heredoc·sed에서 `\n`·`\s`·`\|`가 깨진다 → 정규식·이스케이프·표(`|`)가 든 수정은 Edit 도구로 쓴다
