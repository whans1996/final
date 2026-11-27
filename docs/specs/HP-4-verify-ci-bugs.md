# HP-4 하네스 PR ④ 검증·CI·버그

| 항목 | 값 |
|---|---|
| 상태 | DONE (PR #238) <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | HP-4 (로드맵 트랙 4 하네스, 11/28~11/29) |
| 우선순위 | 하네스 (기능 동결 대상 아님) |
| 영역 오너 | A 김동환 (하네스 주도). 공통 파일(`ci.yml`·`CODEOWNERS`·스킬)을 바꾸므로 PR에 @JoonseobKim |
| 관련 필수 요구사항 | 없음 (개발 과정 AI 활용 사례 → 산출물 08 소재) |
| 의존 spec | 없음. 범위 근거는 `docs/기획서.md` 7.7절 (H-9~H-12, HX-4 #165, HX-5 #166, 보류 H-8 #178) |
| 관련 이슈 | #165 #166 #133 #134 #178 #196 #198 #224 (구현 PR에 `Closes`) |
| 승인 | 김동환 2026-11-27 |

## 1. 목적

검증·CI·Codex 판정이 **틀리게 통과시키거나(놓침) 쓸데없이 다시 도는(낭비)** 경우를 없앤다.
- 놓침: NOW 형식 오류·하네스 설정 파일 변경이 커밋·pre-push를 지나 CI에서만 잡힘(#166, H-10), 검증 중 생긴 커밋이 통과로 기록됨(#133), 코드를 커밋한 뒤 NOW 없이 멈춰도 막지 않음(H-11), push 뒤 STALE 재요청을 놓침(#178), 넘겨주기 알림 실패가 조용히 사라짐(#196)
- 낭비·오판: verifier가 gradle 잠금을 우회(H-9), `scripts/`가 조금만 바뀌어도 BE·FE 전체 CI(H-12), docs만 push해도 BE·FE CI 재실행(#224), docs만 PR의 Codex 체크가 pending(#165), 영수증으로 COMPLETE된 커밋이 docs 판정 기준에서 빠짐(#198), `stamp --draft` 영역에 develop에서만 바뀐 파일이 섞임(#134)

이미 앞선 PR에서 처리된 부분은 다시 하지 않는다: settings.json hook 경로 존재 테스트(`settings.test.mjs`), guard-files 판정 단위 테스트(`file-guard.test.mjs`).

## 2. 수용 기준 (Acceptance Criteria)

> 하네스 테스트는 `scripts/test/*.test.mjs`(node:test). 테스트 이름 앞에 `HP-4 ACn`을 붙인다.

### 검증 (verify·커밋 hook·Stop hook)

| ID | Given | When | Then |
|---|---|---|---|
| AC1 (H-9) | 특정 BE 테스트만 돌리고 싶을 때 | `node scripts/verify.mjs be --tests ConceptExtractorTest --tests 'Eval*'`를 실행하면 | gradle 잠금(`acquireLock`)을 잡고 `test --tests …`만 돌려 실패만 요약한다. 부분 실행이므로 `.harness/verified.json`에 남기지 않는다. `be` 외 영역에 `--tests`를 주면 사용법 오류(exit 2). `verifier.md`는 `gradlew --tests` 직접 호출 대신 이 명령을 쓴다 |
| AC2 (#133) | 전체 검증이 도는 중에 다른 커밋이 생겨 | 검증이 끝났을 때 HEAD가 시작 때와 다르면 | `verified.json`에 기록하지 않고 "검증 중 HEAD가 바뀌어 기록하지 않음"을 알린다. 시작 때 HEAD로 같으면 기존대로 기록한다 (판정은 순수 함수로 테스트) |
| AC3 (#166, H-10) | 다음 파일만 스테이징했을 때: `docs/handoff/NOW-*.md`, `.claude/settings.json`, `scripts/lib/pcs.json`, `.github/CODEOWNERS`, `.github/workflows/ci.yml` | `areasOf`·`relatedHarnessTests`가 영역을 고르면 | `harness` 영역이 되고 그 파일을 읽는 테스트를 고른다 (NOW → `common.test.mjs`, settings → `settings.test.mjs`, pcs → `env-check.test.mjs`, CODEOWNERS → `codeowners.test.mjs`·`owners.test.mjs`, ci.yml → AC7의 필터 테스트). 그래서 머리말이 깨진 NOW 파일 커밋은 커밋 hook에서 거부된다 |
| AC4 (#166) | NOW 본문을 새로 쓸 때 | `node scripts/stamp.mjs --write-now <본문 파일>`을 실행하면 | 지금 NOW 파일의 머리말(제목·`>` 안내 줄)은 그대로 두고, `갱신:` 줄을 `--now` 출력으로 새로 쓰고, 그 아래를 본문 파일 내용으로 바꾼다. 본문에 `갱신:`으로 시작하는 줄이 있거나 `## 현재 기능`·`## 바로 다음 할 일`이 없으면 파일을 바꾸지 않고 exit 1. 결과가 30줄을 넘으면 경고만 한다. `/handoff`·`/implement`·CLAUDE.md 체크포인트 규칙의 NOW 갱신 방법을 이 명령으로 바꾼다 |
| AC5 (H-11) | 이 세션이 코드를 고쳐 **커밋까지 한 뒤** NOW 파일을 갱신하지 않고 | 턴을 끝내려 하면 | checkpoint-nudge가 한 번 막는다. 기준: 현재 브랜치의 merge가 아닌 커밋 중 작성자가 나(git `user.email`)이고, 커밋 시각이 NOW 파일 mtime보다 늦고, 작업 파일(`WORK_WATCH`)을 바꿨고, 그 파일을 이 세션이 건드린 커밋. 자동 동기화로 들어온 merge·상대 커밋, 다른 세션이 건드린 파일, `stop_hook_active`, 계획 모드는 막지 않는다. 판정은 `scripts/lib/checkpoint.mjs`로 옮겨 단위 테스트한다 (미커밋 변경 판정 포함) |
| AC6 (#134) | 브랜치가 develop보다 뒤처져 develop에서만 바뀐 파일이 있을 때 | `stamp --draft`가 바뀐 영역을 계산하면 | `merge-base(develop, HEAD)` 대비 바뀐 파일 + 작업 트리 변경만 센다. develop에서만 바뀐 파일은 넣지 않는다 |

### CI

| ID | Given | When | Then |
|---|---|---|---|
| AC7 (H-12) | PR이 BE·FE job이 실행하지 않는 하네스 파일만 바꿨을 때 (예: `scripts/roadmap.mjs`, `.claude/hooks/*`, `.github/workflows/roadmap.yml`) | `changes` job이 판별하면 | `be`·`fe`는 false다. 필터의 `scripts` 항목은 BE·FE job이 실제로 실행하는 파일(`scripts/verify.mjs`와 import 닫힘, `scripts/seed-check.mjs`와 import 닫힘, `scripts/test/seed-examples.test.mjs`, `.github/workflows/ci.yml`)만 나열한다. 하네스 테스트가 **이 import 닫힘이 ci.yml 필터에 모두 들어 있는지** 검사해, 새 의존이 생기면 실패한다 |
| AC8 (#224) | PR에 BE 변경이 있고 직전에 **성공한** CI 실행(head S)이 있을 때 | `S..새 head`에서 바뀐 파일이 `docs/`뿐(`docs/api/openapi.json` 제외)인 push(`synchronize`)가 오면 | `be`·`fe`는 false라 Backend·Frontend job이 건너뛰어지고(skipped = 필수 체크 통과), 로그에 "직전 성공 S 이후 문서만 변경"을 남긴다. 직전 성공이 없거나, 그 사이에 문서 밖 파일이 바뀌었거나, S가 head의 조상이 아니면(브랜치 재작성) 기존대로 PR 전체 diff로 판별한다. `push`(develop)·`opened`·`reopened`는 기존대로. 판정은 `scripts/lib/ci-changes.mjs` 순수 함수로 테스트한다 |
| AC9 (#224 운영) | `/pr-review` 10~11단계에서 spec DONE·NOW·작업로그를 커밋할 때 | 스킬을 따르면 | 문서 커밋을 하나로 묶어 한 번만 push한다고 스킬 본문에 적혀 있다 |

### Codex·로드맵 알림

| ID | Given | When | Then |
|---|---|---|---|
| AC10 (#165) | PR 전체 변경(develop merge-base 대비)이 문서만이고 Codex 리뷰가 없거나 실패했을 때 | `codex-review.mjs <PR> --status`가 판정하면 | `COMPLETE "문서만 PR, Codex 생략 (conventions 7절)"`이고 `codex-status.yml`의 "Codex" 체크는 `success`다. `onlyDocsChanged`를 재사용한다 |
| AC11 (#198) | 상태 댓글은 지적이 있던 커밋 A에 머물러 있고, A의 지적보다 늦은 "지적 없음" 영수증이 커밋 B(A 뒤)에 달렸을 때 | head = B + docs만 바꾼 커밋이면 | `COMPLETE "문서만 변경 (Codex ✅ B 이후)"`. 영수증보다 늦은 미해결 인라인 지적이 있으면 기존대로 COMPLETE가 아니다 |
| AC12 (#178) | codex-loop가 3분 안에 이미 판정했고 로컬 HEAD는 그대로인데 | 그 사이 push로 `origin/<브랜치>`가 바뀌었으면 | `skipRecheck`가 false라 다시 판정해 push 뒤의 STALE을 놓치지 않는다. 판정 기록(`prev`)에 판정 때의 원격 브랜치 sha를 함께 남긴다 |
| AC13 (#196) | develop push 때 지난 넘겨주기 알림을 보내야 하는데 | gh 조회나 Mattermost 전송이 실패하면 | `roadmap.yml` job이 실패(exit 1)하고 "Re-run으로 다시 보내기"를 로그에 남긴다. 같은 실행을 Re-run하면 같은 커밋 기준으로 다시 계산해 보낸다. 웹훅 secret이 없을 때 건너뛰는 동작은 그대로다 |

## 3. API 계약

없음 (하네스).

## 4. DB 변경

없음.

## 5. 프론트엔드

없음.

## 6. LLM

없음.

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1 | `verify.test.mjs`(새로) — 인자 해석 `parseVerifyArgs`: `--tests` 여러 개, be 외 영역 오류, 부분 실행은 기록 안 함 | node:test |
| AC2 | `verify.test.mjs` — `shouldRecord({ startHead, endHead, dirty })` | node:test |
| AC3 | `related.test.mjs` — 파일별 영역·고른 테스트 | node:test |
| AC4 | `records.test.mjs` 또는 `stamp.test.mjs` — 머리말 유지·갱신 줄 교체·금지 본문 거부·30줄 경고 (임시 디렉터리) | node:test |
| AC5 | `checkpoint.test.mjs`(새로) — 미커밋·커밋 후 미기록·merge·상대 커밋·다른 세션 파일·stop_hook_active | node:test |
| AC6 | `records.test.mjs` — 영역 계산 기준(merge-base) | node:test |
| AC7 | `ci-filter.test.mjs`(새로) — verify·seed-check import 닫힘 ⊆ ci.yml be·fe 필터, 하네스 전용 파일 미포함 | node:test |
| AC8 | `ci-changes.test.mjs`(새로) — 직전 성공 이후 docs만 / openapi.json 포함 / 직전 성공 없음 / 조상 아님 / push 이벤트 | node:test + PR에서 실제 docs push로 skipped·필수 체크 통과 수동 확인 |
| AC9 | 수동 (스킬 본문) | 문서 |
| AC10~AC12 | `codex.test.mjs` | node:test |
| AC13 | `roadmap.test.mjs` — 전송 실패 시 실패 종료 결정 함수 | node:test + 워크플로 수동 확인 |

## 8. 범위 밖

- HP-5 항목(HX-9 주차별 보관, HX-10 API 명세 생성)
- 보류 follow-up: #131 gradle.lock 경합·PID 재사용, #140·#181 weekly 집계, #180 `.harness/logs` 정리
- CI job 이름·브랜치 보호 필수 체크 변경 (이름은 그대로 둔다)
- FE `--tests` 옵션 (vitest 직접 실행은 잠금 문제가 없다)

## 9. 결정 필요

- 없음 (11-27 사용자 결정: 한 PR, #196은 실패 시 job 실패, #166은 `--write-now` 옵션까지)

## 10. 구현 체크리스트

`/implement`가 진행하며 체크한다.

- [x] 브랜치 `feature/HP-4-verify-ci-bugs`
- [x] AC별 실패 테스트 작성 (red 확인)
- [x] 하네스 구현 (green, `node --test "scripts/test/*.test.mjs"`)
- [x] 스킬·에이전트·CLAUDE.md·`docs/conventions.md` 문구 반영 (verifier.md, handoff·implement·pr-review SKILL.md)
- [x] reviewer 지적 사항 반영 (반드시 1·권장 3, AC5 놓치는 경우는 PR 본문)
- [ ] PR에서 AC8 실제 확인 (docs만 push → Backend·Frontend skipped, 필수 체크 통과)
- [x] PR CI 초록, @JoonseobKim 멘션 (#238, Codex ✅ 9bd4c97)
- [x] 상태 DONE (머지 직전 완료 표시)
