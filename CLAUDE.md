# 바이브노트 (VibeNote)

AI로 코딩하는 주니어와 개발자 지망생이 **AI가 쓴 코드를 수준별 설명 → 개념 카드 → 퀴즈·복습**으로 이해하게 돕는 학습 플랫폼이다.
SSAFY 관통 프로젝트(2026-11-17 시작, 12-24 발표). Spring Boot 4 + MyBatis + MySQL 8 / Vue 3 + Pinia.

2인이 **영역별로 병렬 개발**한다 (11-19부터. A 김동환 = AI·분석·학습 루프, B 김준섭 = 카드·소셜, `docs/기획서.md` 4절). 각자 PC 2대(SSAFY 교육장·집, id `dh-ssafy`·`dh-home`·`js-ssafy`·`js-home`)를 오가며 Claude Code로 작업하고, 같은 사람의 PC 사이에서는 사람별 NOW 파일로 교대한다. PC는 `setup.mjs --pc`로 등록하고, 세션 시작 hook이 PC·환경 점검·PC 전환·상대 작업 요약(원격 브랜치의 상대 NOW)을 알리고, 세션 중에는 `peer-watch` hook(10분 간격)과 Mattermost가 상대 변화를 알린다 (`docs/환경설정.md` 0·7절, `docs/conventions.md` 9절). 세션은 토큰 소진으로 언제든 끊길 수 있다.
그래서 **상태를 파일에 남기는 일을 코드 작성만큼 중요하게** 다룬다.

- 개발 저장소: **GitHub public** (`origin`, PR + Actions CI). Actions 한도 때문에 개발이 끝날 때까지 public으로 둔다 (11-26). 그래서 비밀값·eval 데이터 내용이 커밋·PR·이슈에 들어가지 않게 더 엄격히 본다. develop·main은 브랜치 보호(PR 필수, CI 5개 필수, 관리자 포함, force push·삭제 금지)
- 제출 저장소: **lab.ssafy.com GitLab** (`gitlab`, 최종 산출물만. push는 사람이 직접)
- 공식 명세서(냠냠코치, 대외비)는 저장소에 없다. 그 구조와 제출물 목록은 `docs/기획서.md` 0절과 "명세서 구조 매핑"에 반영되어 있다.

## 문서 지도

| 알고 싶은 것 | 읽을 곳 |
|---|---|
| 지금 무엇을 하던 중인가 | 내 NOW 파일 `docs/handoff/NOW-dh.md`(김동환)·`NOW-js.md`(김준섭) (세션 시작 시 hook이 내 것 전문과 상대 요약을 주입) |
| 일정 (기간 × 사람 × 트랙 할 일, 넘겨주는 시점, 공통 일정) | `docs/로드맵.md` (정본. 상태는 PR 본문 `Roadmap: <ID>` 줄로 계산, `node scripts/roadmap.mjs`. 세션 시작 배너 🗺️ 줄) |
| 작업 이력 | `docs/작업로그.md` (두 사람 공용, 맨 아래 추가) |
| 기획·운영 결정 기록 | `docs/결정로그.md` (맨 아래 추가) |
| 기능 하나의 상세 요구사항 | `docs/specs/F-xx-*.md` |
| 기능 목록, ERD, API 목록, 일정, 역할 분담 | `docs/기획서.md` (길다. 필요한 섹션만 읽는다) |
| API 응답·에러 코드·네이밍·커밋·PR 규칙 | `docs/conventions.md` |
| 현재 구현된 API (정본) | `docs/api/openapi.json` (테스트가 생성), 실행 중이면 http://localhost:8080/swagger-ui/index.html |
| 제출 산출물 (요구사항 정의서, 유스케이스, ERD, WBS, 간트, 화면 설계서, API 명세, AI 활용, 데이터 셋, 보고서) | `docs/산출물/` (제출 허브는 루트 `README.md`) |
| 후속 작업 (기능 밖으로 미룬 것) | GitHub Issues `follow-up` + `area:*` 라벨 (`gh issue list --label follow-up`, 세션 시작 배너 📌 줄). 순환·닫기 규칙은 `docs/conventions.md` 7절 |
| 팀 대시보드·AI 활용 사례 (노션, 사람용 요약이며 정본 아님) | `docs/notion.md` |
| 영역 경계 (어느 파일이 누구 것인가) | `.github/CODEOWNERS`, `docs/conventions.md` 9절 (`node scripts/owners.mjs <경로>`) |
| 백엔드 규칙 | `backend/CLAUDE.md` |
| 프론트엔드 규칙 | `frontend/CLAUDE.md` |
| 외부 데이터 목록·라이선스·분할(dev/eval/demo) | `data/datasets.json`, `docs/산출물/09-데이터셋.md` |
| 개발 환경 설치 (JDK, WSL·Docker) | `docs/환경설정.md` |

## 작업 흐름

```
/pickup → /spec F-xx → (사람이 spec 승인) → /implement F-xx → /pr-review → (CI 초록 + Codex COMPLETE → 영역 오너가 머지) → /handoff
```

- 새 세션은 `/pickup`으로 상태를 확인하고 시작한다. 기능은 **내 영역**에서 고른다. 상대 브랜치·상대 NOW 파일은 건드리지 않는다.
- 상대가 develop에 머지한 것은 `git merge origin/develop`으로 받는다 (rebase·force push 금지). 세션 시작·`peer-watch` hook이 fetch 때 **자동으로 합친다** (다른 PC가 push한 내 브랜치 커밋 포함, 체크아웃 안 된 로컬 develop·main은 fast-forward). 추적 파일에 커밋 안 된 변경이 있으면 미루고, 충돌하면 되돌린 뒤 알린다. push는 하지 않는다 (`scripts/lib/sync.mjs`, 끄기: `git config vibenote.autoSync false`). 마이그레이션은 `node scripts/next-migration.mjs`로 번호를 확인하고, openapi.json 충돌은 BE 테스트로 다시 생성한다.
- 진행현황·WBS·간트는 기능 PR에서 고치지 않는다. 주간 `/progress`가 별도 docs PR로 맞춘다.
- 미룬 작업은 `follow-up` 이슈(area 라벨·담당자)로 남기고, `/spec` 때 흡수하거나 기능 사이에 `feature/issue-<번호>-slug`로 처리한다. 해결 PR에 `Closes #n`을 적으면 develop 머지 때 워크플로가 닫는다.
- 기능 구현은 상태가 `APPROVED`인 spec이 있어야 시작한다. 없으면 `/spec`부터 한다.
- 구현은 **수용 기준(AC)별로 실패하는 테스트를 먼저** 쓰고 통과시키는 순서로 한다.
- 테스트·빌드 결과는 긴 로그를 직접 읽지 말고 `verifier` 에이전트나 `node scripts/verify.mjs`의 요약으로 확인한다.
- `/progress`로 필수 요구사항 13개, 2차 백로그 티어(T0~T3), 마일스톤, **제출 산출물 준비 현황**을 점검한다 (매주 일요일. 판단 기준은 12/08 중간 점검과 12/14 기능 동결, `docs/기획서.md` 7.4b).
- 기획을 다시 정할 때(2차 기획, 재인터뷰)는 `/project-interview`로 `docs/기획서.md` 6·7절에 누적하고 결정은 결정로그에 남긴다.
- 기능을 바꾸면 영향받는 산출물(화면 설계서, ERD, 요구사항 정의서)도 같은 PR에서 고치거나 PR "후속 작업"에 적는다.

## 세션 운영 (컨텍스트·모델·토큰)

턴마다 컨텍스트 전체를 다시 읽으므로 **비용은 대화 길이에 비례한다** (11-18 실측: 한 세션에 단계를 다 몰아 하자 평균 264k·최대 463k, 비용의 90%가 메인). 그래서:

- **단계마다 새 세션**: spec 승인, PR 생성, 머지 후 정리가 끝나면 내 NOW 파일이 최신인지 확인하고 사용자에게 `/clear`를 권한다. `/clear` 후에는 SessionStart hook이 NOW 파일을 다시 주입한다. 200k를 넘으면 Stop hook도 알린다.
- **모델·effort는 세션 시작 때 고르고 중간에 바꾸지 않는다** (모델을 바꾸면 프롬프트 캐시를 처음부터 다시 쓴다).

| 세션 종류 | 메인 모델 / effort |
|---|---|
| `/spec`, `/implement`, 리뷰·Codex 지적 반영 | Opus / high |
| `/pickup`, `/progress`, `/handoff`, 문서·산출물 정리 | Opus / medium 또는 Sonnet |

- 서브에이전트는 frontmatter로 고정한다: verifier Haiku·low, contract-checker·ui-verifier Sonnet·medium, prompt-evaluator Sonnet·medium, reviewer Opus(inherit)·high.
- 사용량 확인: `node scripts/token-report.mjs [latest|--list]`.
- **진행 보고**: 도구만 여러 번 쓰는 작업이 길어지면 지금 무엇을 하는지 한국어 한 줄로 먼저 알리고 이어 간다 (사용자는 도구 출력을 보지 못한다, 11-26).

## 절대 규칙

1. **main·develop에 직접 커밋하지 않는다.** `feature/F-xx-slug` 브랜치에서 작업하고 develop에는 **PR**로 합친다. 머지는 CI가 초록이고 Codex가 COMPLETE일 때 영역 오너(PR 작성자 본인)가 GitHub에서 한다 (`gh pr merge`는 차단됨, 예외는 `docs/conventions.md` 7절). 공통 코드를 바꾼 PR은 상대를 @멘션한다.
2. **커밋 메시지는 `타입(범위): 요약`** 형식으로 쓴다. 상세 규칙은 `docs/conventions.md` 6절. git hook이 검사한다.
3. `wip`가 아닌 커밋은 git hook이 변경된 영역의 포맷·린트·관련 테스트를 돌리고(빠른 검사), push 전에는 pre-push hook이 바뀐 영역의 전체 검증을 돌린다(tip이 wip면 건너뜀). 실패하면 거부한다. **`--no-verify`로 우회하지 않는다.**
4. **이미 커밋된 Flyway 파일(`V*__*.sql`)은 수정하지 않는다.** 고칠 내용은 새 버전 파일로 추가한다. 예외: develop에 아직 머지되지 않은 내 파일은 번호가 겹칠 때 `git mv`로 **이름(번호)만** 바꿀 수 있다 (CI `check-migrations`가 알린다).
5. **`.env`와 키 파일은 읽거나 커밋하지 않는다.** 키는 환경변수로만 받고, 예시는 `.env.example`에 둔다.
6. **LLM은 기본 `mock` 프로필로 쓴다.** 실제 LLM 호출(서비스 공급자 GMS는 팀 키 크레딧을 쓰고, Ollama·평가 비교용 Gemini도 같다)은 사람이 요청할 때 `prompt-evaluator` 에이전트로만 한다. 실호출 테스트는 `@Tag("live")`로 두고 사람만 `liveTest`로 돌린다.
7. **spec 범위 밖 기능을 임의로 추가하지 않는다.** 필요하면 spec의 "결정 필요"에 적고 사람에게 묻는다.
8. 기획이 바뀌면 `docs/결정로그.md` 맨 아래에 한 줄 추가한다 (기존 행은 고치지 않는다).
9. **API를 바꾸면 `docs/api/openapi.json`도 함께 커밋한다.** CI가 코드와 다르면 실패시킨다. Controller·DTO에는 Swagger 어노테이션을 단다 (`backend/CLAUDE.md`).
10. **GitLab(`gitlab` 원격, lab.ssafy.com)에는 push하지 않는다.** 제출은 사람이 한다.
11. **평가(eval) 데이터는 프롬프트, few-shot 예시, Mock fixture, 시드 카드 작성에 쓰지 않는다.** 데이터를 쓰기 전에 `data/datasets.json`의 `split`을 확인한다 (데이터 누수 방지, `docs/산출물/09-데이터셋.md` 5절). 새 외부 데이터는 라이선스를 확인하고 이 목록과 계획서에 먼저 등록한다.

## 체크포인트 규칙

작업 단위가 끝날 때마다 **내 NOW 파일**(`node scripts/stamp.mjs --file` → `docs/handoff/NOW-<dh|js>.md`)을 **덮어써서** 갱신한다. 상대 NOW 파일은 수정하지 않는다.
작업 단위란 AC 하나 통과, 파일 묶음 완성, 방향 결정, 막힘 발생을 말한다.

- 형식은 NOW 파일에 있는 섹션 구조를 그대로 유지한다. 30줄 이내로 쓴다. `갱신:` 줄은 `node scripts/stamp.mjs --now` 출력을 쓴다 (시각·PC를 짐작해 쓰지 않는다. 갱신 줄만 바꿀 때는 `--touch-now`).
- 기록에 필요한 사실(내 마지막 기록 뒤 커밋, 바뀐 영역, 검증, PR·CI)은 `node scripts/stamp.mjs --draft`로 한 번에 본다. 작업로그는 `--append-log --did … --next … [--blocked …] [--ai …]`로 메모 3줄만 붙인다 (머리 줄·커밋 줄 자동).
- "바로 다음 할 일"은 다른 PC의 세션이 **맥락 없이 바로 시작할 수 있을 만큼** 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
- 코드를 바꾸고 NOW 파일을 갱신하지 않은 채 턴을 끝내려 하면 Stop hook이 한 번 막는다.
- PC를 옮기거나 세션을 마칠 때는 `/handoff`를 쓴다. 토큰이 떨어졌으면 사람이 `node scripts/handoff.mjs`를 실행한다.

## 명령어

| 목적 | 명령 |
|---|---|
| 최초 1회 세팅·PC 등록 (PC마다) | `node scripts/setup.mjs --pc <dh-ssafy\|dh-home\|js-ssafy\|js-home>` |
| 검증 (실패만 요약) | `node scripts/verify.mjs [be\|fe\|harness\|all] [--fix] [--staged]` (`--staged`는 커밋 hook의 빠른 검사) |
| 하네스·환경 점검 (읽기 전용) | `node scripts/doctor.mjs` (hook 권한, 남은 Gradle 잠금, CRLF, FE 의존성, NOW 파일 등) |
| 주간 초안·지표 (`/progress` 전) | `node scripts/weekly.mjs [--days 7]` |
| 화면 점검 | `node scripts/ui-check.mjs / /concepts` (FE dev 서버 실행 중일 때) |
| AI 없이 교대 | `node scripts/handoff.mjs [-m "메모"] [--no-push]` |
| 기록 머리 줄·내 NOW 파일·내 이슈 영역 라벨 | `node scripts/stamp.mjs [--now\|--log\|--file\|--area]` |
| 기록 초안·NOW 갱신 줄·작업로그 추가 | `node scripts/stamp.mjs --draft`, `--touch-now`, `--append-log --did … --next …` |
| 로드맵 (지금 할 항목·내 남은 항목·넘겨주는 시점, 다음 항목, 형식 검사) | `node scripts/roadmap.mjs [--next [--after <ID>]] [--check]` |
| 다음 마이그레이션 번호 | `node scripts/next-migration.mjs` (develop·원격 feature 브랜치 포함) |
| develop 밖에서 진행 중인 브랜치 | `node scripts/branches.mjs` (내 NOW를 바꾼 원격 브랜치), `node scripts/branches.mjs F-xx` (`/spec` 전 중복 확인) |
| 하네스 단위 테스트 | `node --test "scripts/test/*.test.mjs"` |
| PR 스크린샷 Markdown | `node scripts/pr-screens.mjs SC-18 SC-19` (`docs/산출물/screens/` 커밋·push 후) |
| 파일 영역·PR @멘션 대상 | `node scripts/owners.mjs [--diff origin/develop | <경로...>]` |
| 토큰 사용량 (메인·에이전트별) | `node scripts/token-report.mjs [latest|<세션 ID>] [--list]` |
| PR 상태·CI 확인 | `gh pr status`, `gh pr checks` |
| Codex 리뷰 판정·재요청·이관 | `node scripts/codex-review.mjs <PR> [--request --wait --status \| --resolve]` (COMPLETE/FINDINGS/RUNNING/STALE/TIMEOUT/CLOSED/ERROR). 열린 PR이 COMPLETE가 될 때까지 Stop hook `codex-loop`이 루프를 이어 가게 한다 |
| BE 실행 (mock LLM) | `cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'` |
| FE 실행 | `cd frontend && npm run dev` |

## 서브에이전트

| 이름 | 언제 |
|---|---|
| `verifier` | 테스트·빌드를 돌리고 실패만 요약 받을 때. 코드를 수정하지 않는다 |
| `reviewer` | 커밋 묶음이나 PR 전에 spec·컨벤션·보안 기준으로 리뷰할 때 |
| `contract-checker` | spec의 API 계약, `openapi.json`(BE), FE `src/api`가 일치하는지 볼 때 |
| `ui-verifier` | FE 변경 후 실제 화면 렌더링과 콘솔 에러를 볼 때 |
| `prompt-evaluator` | 프롬프트 품질을 실제 LLM으로 평가할 때. **수동 전용, GMS 크레딧 소모** (러너가 지원하는 단계는 에이전트 파일 참고) |

## 환경

- Windows 11 (김준섭 집 PC `js-home`만 macOS Apple Silicon, Homebrew로 설치. PC별 함정은 `scripts/lib/pcs.json` `notes`). JAVA_HOME은 JDK 17 이상(Gradle 실행용, 컴파일 JDK 21은 Gradle이 자동으로 받는다), Node 24, Docker Desktop(Testcontainers), MySQL 8.
- 하네스 스크립트는 셸 차이를 피하려고 전부 Node(`.mjs`)로 작성되어 있다.
- 하네스(`scripts/`·`.claude/`·hook)는 **동결하지 않는다** (11-26 해제): 고칠 것은 고치고 필요한 기능은 넣는다. 개선 목록은 `docs/기획서.md` 7.7절. 고칠 때는 `chore(harness): ...`로 커밋한다.
- 하네스는 제출물에 포함되며, 개발 과정의 AI 활용 사례는 `docs/산출물/08-외부API-생성형AI-활용.md`의 소재가 된다. 기록할 만한 사례(가드가 막은 실수, 리뷰어가 잡은 버그 등)는 작업로그의 `AI 활용` 줄에 적는다.
