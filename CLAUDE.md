# 바이브노트 (VibeNote)

AI로 코딩하는 주니어와 개발자 지망생이 **AI가 쓴 코드를 수준별 설명 → 개념 카드 → 퀴즈·복습**으로 이해하게 돕는 학습 플랫폼이다.
SSAFY 관통 프로젝트(2026-11-17 시작, 12-24 발표). Spring Boot 4 + MyBatis + MySQL 8 / Vue 3 + Pinia.

2인이 서로 다른 PC에서 Claude Code로 **릴레이 개발**한다. 세션은 토큰 소진으로 언제든 끊길 수 있다.
그래서 **상태를 파일에 남기는 일을 코드 작성만큼 중요하게** 다룬다.

- 개발 저장소: **GitHub private** (`origin`, PR + Actions CI)
- 제출 저장소: **lab.ssafy.com GitLab** (`gitlab`, 최종 산출물만. push는 사람이 직접)
- 공식 명세서(냠냠코치, 대외비)는 저장소에 없다. 그 구조와 제출물 목록은 `docs/기획서.md` 0절과 "명세서 구조 매핑"에 반영되어 있다.

## 문서 지도

| 알고 싶은 것 | 읽을 곳 |
|---|---|
| 지금 무엇을 하던 중인가 | `docs/handoff/NOW.md` (세션 시작 시 hook이 자동 주입) |
| 교대 이력 | `docs/작업로그.md` |
| 기능 하나의 상세 요구사항 | `docs/specs/F-xx-*.md` |
| 기능 목록, ERD, API 목록, 일정, 결정 로그 | `docs/기획서.md` (길다. 필요한 섹션만 읽는다) |
| API 응답·에러 코드·네이밍·커밋·PR 규칙 | `docs/conventions.md` |
| 현재 구현된 API (정본) | `docs/api/openapi.json` (테스트가 생성), 실행 중이면 http://localhost:8080/swagger-ui/index.html |
| 제출 산출물 (요구사항 정의서, 유스케이스, ERD, WBS, 간트, 화면 설계서, API 명세, AI 활용, 데이터 셋, 보고서) | `docs/산출물/` (제출 허브는 루트 `README.md`) |
| 백엔드 규칙 | `backend/CLAUDE.md` |
| 프론트엔드 규칙 | `frontend/CLAUDE.md` |
| 외부 데이터 목록·라이선스·분할(dev/eval/demo) | `data/datasets.json`, `docs/산출물/09-데이터셋.md` |
| 개발 환경 설치 (JDK, WSL·Docker) | `docs/환경설정.md` |

## 작업 흐름

```
/pickup → /spec F-xx → (사람이 spec 승인) → /implement F-xx → /pr-review → (CI 초록 → 영역 오너가 머지) → /handoff
```

- 새 세션은 `/pickup`으로 상태를 확인하고 시작한다.
- 기능 구현은 상태가 `APPROVED`인 spec이 있어야 시작한다. 없으면 `/spec`부터 한다.
- 구현은 **수용 기준(AC)별로 실패하는 테스트를 먼저** 쓰고 통과시키는 순서로 한다.
- 테스트·빌드 결과는 긴 로그를 직접 읽지 말고 `verifier` 에이전트나 `node scripts/verify.mjs`의 요약으로 확인한다.
- `/progress`로 필수 요구사항 13개, 마일스톤, **제출 산출물 준비 현황**을 점검한다 (매주 일요일, 12/07 판단 기준).
- 기능을 바꾸면 영향받는 산출물(화면 설계서, ERD, 요구사항 정의서)도 같은 PR에서 고치거나 PR "후속 작업"에 적는다.

## 세션 운영 (컨텍스트·모델·토큰)

턴마다 컨텍스트 전체를 다시 읽으므로 **비용은 대화 길이에 비례한다** (11-18 실측: 한 세션에 단계를 다 몰아 하자 평균 264k·최대 463k, 비용의 90%가 메인). 그래서:

- **단계마다 새 세션**: spec 승인, PR 생성, 머지 후 정리가 끝나면 NOW.md가 최신인지 확인하고 사용자에게 `/clear`를 권한다. `/clear` 후에는 SessionStart hook이 NOW.md를 다시 주입한다. 200k를 넘으면 Stop hook도 알린다.
- **모델·effort는 세션 시작 때 고르고 중간에 바꾸지 않는다** (모델을 바꾸면 프롬프트 캐시를 처음부터 다시 쓴다).

| 세션 종류 | 메인 모델 / effort |
|---|---|
| `/spec`, `/implement`, 리뷰·Codex 지적 반영 | Opus / high |
| `/pickup`, `/progress`, `/handoff`, 문서·산출물 정리 | Opus / medium 또는 Sonnet |

- 서브에이전트는 frontmatter로 고정한다: verifier Haiku·low, contract-checker·ui-verifier Sonnet·medium, prompt-evaluator Sonnet·medium, reviewer Opus(inherit)·high.
- 사용량 확인: `node scripts/token-report.mjs [latest|--list]`.

## 절대 규칙

1. **main·develop에 직접 커밋하지 않는다.** `feature/F-xx-slug` 브랜치에서 작업하고 develop에는 **PR**로 합친다. 머지는 CI가 초록일 때 영역 오너가 GitHub에서 한다 (`gh pr merge`는 차단됨).
2. **커밋 메시지는 `타입(범위): 요약`** 형식으로 쓴다. 상세 규칙은 `docs/conventions.md` 6절. git hook이 검사한다.
3. `wip`가 아닌 커밋은 git hook이 변경된 영역의 테스트를 돌리고, 실패하면 거부한다. **`--no-verify`로 우회하지 않는다.**
4. **이미 커밋된 Flyway 파일(`V*__*.sql`)은 수정하지 않는다.** 고칠 내용은 새 버전 파일로 추가한다.
5. **`.env`와 키 파일은 읽거나 커밋하지 않는다.** 키는 환경변수로만 받고, 예시는 `.env.example`에 둔다.
6. **LLM은 기본 `mock` 프로필로 쓴다.** 실제 Gemini 호출은 사람이 요청할 때 `prompt-evaluator` 에이전트로만 한다.
7. **spec 범위 밖 기능을 임의로 추가하지 않는다.** 필요하면 spec의 "결정 필요"에 적고 사람에게 묻는다.
8. 기획이 바뀌면 `docs/기획서.md` 결정 로그에 한 줄 추가한다.
9. **API를 바꾸면 `docs/api/openapi.json`도 함께 커밋한다.** CI가 코드와 다르면 실패시킨다. Controller·DTO에는 Swagger 어노테이션을 단다 (`backend/CLAUDE.md`).
10. **GitLab(`gitlab` 원격, lab.ssafy.com)에는 push하지 않는다.** 제출은 사람이 한다.
11. **평가(eval) 데이터는 프롬프트, few-shot 예시, Mock fixture, 시드 카드 작성에 쓰지 않는다.** 데이터를 쓰기 전에 `data/datasets.json`의 `split`을 확인한다 (데이터 누수 방지, `docs/산출물/09-데이터셋.md` 5절). 새 외부 데이터는 라이선스를 확인하고 이 목록과 계획서에 먼저 등록한다.

## 체크포인트 규칙

작업 단위가 끝날 때마다 `docs/handoff/NOW.md`를 **덮어써서** 갱신한다.
작업 단위란 AC 하나 통과, 파일 묶음 완성, 방향 결정, 막힘 발생을 말한다.

- 형식은 NOW.md에 있는 섹션 구조를 그대로 유지한다. 30줄 이내로 쓴다.
- "바로 다음 할 일"은 상대가 **맥락 없이 바로 시작할 수 있을 만큼** 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
- 코드를 바꾸고 NOW.md를 갱신하지 않은 채 턴을 끝내려 하면 Stop hook이 한 번 막는다.
- 상대에게 넘길 때는 `/handoff`를 쓴다. 토큰이 떨어졌으면 사람이 `node scripts/handoff.mjs`를 실행한다.

## 명령어

| 목적 | 명령 |
|---|---|
| 최초 1회 세팅 (PC마다) | `node scripts/setup.mjs` |
| 검증 (실패만 요약) | `node scripts/verify.mjs [be\|fe\|all] [--fix]` |
| 화면 점검 | `node scripts/ui-check.mjs / /concepts` (FE dev 서버 실행 중일 때) |
| AI 없이 교대 | `node scripts/handoff.mjs [-m "메모"] [--no-push]` |
| PR 스크린샷 Markdown | `node scripts/pr-screens.mjs SC-18 SC-19` (`docs/산출물/screens/` 커밋·push 후) |
| 토큰 사용량 (메인·에이전트별) | `node scripts/token-report.mjs [latest|<세션 ID>] [--list]` |
| PR 상태·CI 확인 | `gh pr status`, `gh pr checks` |
| Codex 리뷰 판정·재요청 | `node scripts/codex-review.mjs <PR> [--request --wait]` (COMPLETE/FINDINGS/RUNNING/STALE) |
| BE 실행 (mock LLM) | `cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'` |
| FE 실행 | `cd frontend && npm run dev` |

## 서브에이전트

| 이름 | 언제 |
|---|---|
| `verifier` | 테스트·빌드를 돌리고 실패만 요약 받을 때. 코드를 수정하지 않는다 |
| `reviewer` | 커밋 묶음이나 PR 전에 spec·컨벤션·보안 기준으로 리뷰할 때 |
| `contract-checker` | spec의 API 계약, `openapi.json`(BE), FE `src/api`가 일치하는지 볼 때 |
| `ui-verifier` | FE 변경 후 실제 화면 렌더링과 콘솔 에러를 볼 때 |
| `prompt-evaluator` | 프롬프트 품질을 실제 LLM으로 평가할 때. **수동 전용, 무료 한도 소모** |

## 환경

- Windows 11. JAVA_HOME은 JDK 17 이상(Gradle 실행용, 컴파일 JDK 21은 Gradle이 자동으로 받는다), Node 24, Docker Desktop(Testcontainers), MySQL 8.
- 하네스 스크립트는 셸 차이를 피하려고 전부 Node(`.mjs`)로 작성되어 있다.
- 하네스 자체를 고칠 때는 `chore(harness): ...`로 커밋한다.
- 하네스는 제출물에 포함되며, 개발 과정의 AI 활용 사례는 `docs/산출물/08-외부API-생성형AI-활용.md`의 소재가 된다. 기록할 만한 사례(가드가 막은 실수, 리뷰어가 잡은 버그 등)는 작업로그의 `AI 활용` 줄에 적는다.
