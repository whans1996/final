# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-19 11:57 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/common-notify-per-person

## 현재 기능

하네스 2건 (common) — **PR #35** Codex 리뷰 루프 강제(`feature/common-codex-loop`, Codex P2 2건 수정 8e433c2, 재리뷰 대기) / **Mattermost 사람별 웹훅**(`feature/common-notify-per-person`, PR 생성 중)

## 진행

- ✅ #35: 계정 미연결 ERROR, Stop hook `codex-loop`, PR 체크 `Codex`, 상한 5회, reviewer 반드시 1·권장 7, Codex P2 2건(끝난 --wait 오인, --resolve 체크 갱신)
- ✅ 알림: `lib/notify.mjs` senderKey, `notify-message.mjs --sender`, `notify.yml`이 행동한 사람 웹훅(`MATTERMOST_WEBHOOK_URL_DH`·`_JS`, 없으면 공용)으로 전송, username 제거, 환경설정 7절, 결정로그
- 김준섭 PR #34(계정 미연결 ERROR)는 #35의 부분집합 → 준섭님이 닫거나 먼저 머지 후 #35에서 develop 합쳐 정리

## 바로 다음 할 일

1. PR #35: `node scripts/codex-review.mjs 35 --request --wait --status` COMPLETE + CI 초록 → 본인 머지
2. 알림 PR: CI·Codex 확인 → 머지. 두 PR 모두 NOW-dh를 고쳐서 늦게 머지하는 쪽이 develop 합칠 때 NOW-dh 충돌 → 최신 내용으로 정리
3. 사람: 동환 웹훅 URL을 secret `MATTERMOST_WEBHOOK_URL_DH`로, 준섭 웹훅을 `_JS`로 등록(환경설정 7절). 준섭 Codex 계정 연결(8절, #35 머지 뒤)
4. `/clear` 뒤 `/spec`으로 **공통 기반** spec
5. 1.4 유스케이스(마감 11/23) `docs/산출물/02-유스케이스.md`

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부

## 주의

- Gradle 테스트를 bootRun과 동시에 돌리지 않는다. 통합 테스트는 Docker Desktop 필요. V1~V5 수정 금지
