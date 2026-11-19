# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-19 11:59 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/common-codex-loop

## 현재 기능

하네스 2건 (common), 둘 다 Codex ✅·CI 대기 — **PR #35** Codex 리뷰 루프 강제(`feature/common-codex-loop`, Codex ✅ 8e433c2·99600dd 문서만) / **PR #36** Mattermost 사람별 웹훅(`feature/common-notify-per-person`, Codex ✅ 737f4fd)

## 진행

- ✅ #35: 계정 미연결 ERROR, Stop hook `codex-loop`, PR 체크 `Codex`(실제 ✅ 표시 확인), 상한 5회, reviewer 반드시 1·권장 7, Codex P2 2건 수정. 김준섭 #34(머지됨)와 충돌은 #35 쪽으로 정리(99600dd)
- ✅ #36: `notify.yml`이 행동한 사람 웹훅(`MATTERMOST_WEBHOOK_URL_DH`·`_JS`, 없으면 공용)으로 전송, senderKey·`--sender`, username 제거, 환경설정 7절
- 두 브랜치의 이 파일은 같은 내용이다 (머지 충돌 방지)

## 바로 다음 할 일

1. `gh pr checks 35`·`gh pr checks 36` 초록 → GitHub에서 본인 머지 → `git switch develop && git pull`
2. 사람: 준섭 웹훅을 secret `MATTERMOST_WEBHOOK_URL_JS`로 등록(환경설정 7절). 동환 웹훅은 공용 그대로 또는 `_DH`로
3. `/clear` 뒤 `/spec`으로 **공통 기반** spec (concept 도메인·Mapper·카드 DTO, ErrorCode·ActivityType 블록, 라우터·nav 분리, ConceptCard·Pagination, 관리자 레이아웃)
4. 1.4 유스케이스(마감 11/23) `docs/산출물/02-유스케이스.md`

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부

## 주의

- Gradle 테스트를 bootRun과 동시에 돌리지 않는다. 통합 테스트는 Docker Desktop 필요. V1~V5 수정 금지
