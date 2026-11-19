# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-19 15:10 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-00b-common-foundation

## 현재 기능

F-00b 공통 기반 — **PR #42** (develop, `@JoonseobKim` 멘션). CI 초록, **Codex ✅ e5301af**(재리뷰 0회), spec DONE 표시 완료 → 사람이 GitHub에서 머지만 남음

## 진행

- ✅ BE AC1~6 (`a637dbd`), FE AC7~11 (`58b4a28`), 문서 (`fd38f99`), AC12 ui-verifier, F-05a 간헐 실패 수정 (`48ac033`)
- ✅ /pr-review: verifier(BE 330·FE 175), contract-checker 불일치 0(F-09 spec 3절 필드 일치), reviewer 반드시 수정 0 → 권장 4건 반영 `e5301af` (AC4 from() 테스트, findPublishedByIds Javadoc, 관리자 승격 안내, CONCEPT_404 표), 화면 설계서 SC-20·21 스크린샷
- ✅ CI 초록, Codex COMPLETE `e5301af`, `docs(F-00b): 완료 표시 (spec DONE)` 커밋

## 바로 다음 할 일

1. 사람: GitHub에서 PR #42 머지 (wip 없음, 일반 머지 가능). 머지 전 develop이 앞서 있으면 `git merge origin/develop` → push → CI 재확인. 머지 뒤 Mattermost로 김준섭에게 F-09 시작 알림
2. 머지 뒤 `git checkout develop && git pull`, 1.4 유스케이스(마감 11/23) `docs/산출물/02-유스케이스.md` (Opus/medium 새 세션)

## 막힌 점 / 결정 필요

- ui 점검 계정 `ui-admin@vibe.dev`/`vibe1234`(로컬 DB에만, ADMIN). storageState `.harness/ui-admin.json`
- 김준섭: F-09는 F-00b 머지 뒤 시작. 비로그인용 "개념 카드" 메뉴는 F-09에서 `meta.nav.show: 'all'` 추가가 필요할 수 있음 (PR #42 본문에 적음)
- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부

## 주의

- Gradle 테스트를 bootRun과 동시에 돌리지 않는다. 통합 테스트는 Docker Desktop 필요. V1~V5 수정 금지
- Git Bash heredoc·sed에서 `\n`·`\s`·`\|`가 깨진다 → 정규식·이스케이프·표(`|`)가 든 수정은 Edit 도구로 쓴다
