---
name: implement
description: 승인된 spec(F-xx)을 TDD로 구현한다. 브랜치 생성 → 마이그레이션 → AC별 실패 테스트 → 구현 → 검증(verifier, contract-checker, ui-verifier) → 체크포인트·커밋을 반복한다. "/implement F-05", "F-05 구현하자", "이어서 구현" 요청 시 사용.
argument-hint: F-xx
---

# /implement F-xx — TDD 구현 루프

## 0. 전제 확인 (하나라도 아니면 멈추고 보고)

- `docs/specs/F-xx-*.md`가 있고 상태가 **APPROVED** 또는 진행 중(체크리스트 일부 체크)이다. DRAFT면 `/spec`으로 돌려보낸다.
- 의존 spec이 DONE이거나 필요한 부분이 구현되어 있다.
- `backend/`·`frontend/` 프로젝트가 생성되어 있다.

## 1. 준비

1. 브랜치: `feature/F-xx-<slug>`가 있으면 switch, 없으면 최신 develop(`git switch develop && git pull`)에서 생성. 상대 브랜치에는 커밋하지 않는다.
2. spec을 다시 읽고 **이번 세션의 목표 AC**를 정한다 (보통 1~3개). 내 NOW 파일(`node scripts/stamp.mjs --file`)에 적는다.
3. DB 변경이 있으면 `git merge origin/develop` 후 `node scripts/next-migration.mjs`가 알려 준 번호로 `V{n}__설명.sql`을 추가한다 (develop과 상대 feature 브랜치의 번호까지 본다). 커밋된 V 파일의 내용은 절대 수정하지 않는다. 상대가 같은 번호를 먼저 머지했으면(CI `check-migrations` 실패) develop을 합친 뒤 **아직 develop에 없는 내 파일만** `git mv`로 번호를 바꾼다.

## 2. AC 하나씩 반복 (red → green → 정리)

각 AC마다:

1. **Red**: AC를 검증하는 테스트를 먼저 작성한다. 테스트 이름에 AC 번호를 넣는다.
   - BE: Controller는 MockMvc 통합 테스트(`IntegrationTestBase` 상속), 복잡한 로직은 Service 단위 테스트.
   - FE: Vitest. API 모듈은 `vi.mock`.
   - `verifier`로 실행해 **의도한 이유로 실패하는지** 확인한다 (컴파일 에러로 실패하는 것은 red가 아니다 → 최소 시그니처만 만들어 컴파일되게 한다).
2. **Green**: 테스트를 통과시키는 최소 구현. `backend/CLAUDE.md`, `frontend/CLAUDE.md`, `docs/conventions.md` 규칙을 따른다.
   - 테스트를 통과시키려고 테스트를 고치지 않는다. 테스트가 spec과 다르게 쓰였을 때만 고치고 이유를 커밋 메시지에 남긴다.
3. **검증**: `verifier`로 해당 영역 전체 테스트 실행 (다른 테스트를 깨지 않았는지).
4. **체크포인트**: spec 체크리스트와 내 NOW 파일 진행 상태를 갱신하고 커밋 `feat(F-xx): AC1 ...` (git hook이 테스트를 다시 돌린다).

막히면(같은 실패 3회, 30분 이상 진전 없음): 멈추고 NOW 파일 막힌 점에 시도한 것을 적은 뒤 사용자에게 묻는다.

## 3. 기능 단위 마무리

- API를 만들거나 바꿨으면 `contract-checker` 실행 → 불일치 수정.
- FE 화면을 바꿨으면 dev 서버와 BE(local 프로필)를 띄우고 `ui-verifier` 실행 → 콘솔 에러·빈 화면 수정. 서버를 띄울 수 없으면 사용자에게 요청한다.
- AI 기능이면 mock fixture로 파이프라인 테스트가 통과하는지 확인한다. 실제 LLM 품질 확인이 필요하면 사용자에게 `prompt-evaluator` 실행을 제안한다 (직접 실행하지 않는다).
- API를 바꿨으면 `OpenApiExportTest`가 갱신한 `docs/api/openapi.json`을 같은 커밋에 포함한다. 새 Controller·DTO에는 Swagger 어노테이션을 단다. develop을 합칠 때 openapi.json이 충돌하면 손으로 고치지 말고 아무 쪽이나 고른 뒤 BE 테스트(`OpenApiExportTest`)로 다시 생성해 커밋한다.
- 공통 코드(SecurityConfig, GlobalExceptionHandler, 공통 응답·ErrorCode, FE 라우터·레이아웃·공용 컴포넌트·api 클라이언트)를 바꿨으면 PR 본문에서 상대를 @멘션하도록 NOW 파일 주의에 적는다.
- 화면·테이블·API가 추가되었으면 `docs/산출물/`의 화면 설계서·ERD·요구사항 정의서에 반영한다 (짧게라도. 최종 다듬기는 W5).
- 모든 AC가 green이면 `/pr-review`를 제안한다.

## 규칙

- spec에 없는 기능, 리팩터링, 의존성 추가는 하지 않는다. 필요하면 spec "결정 필요"에 적고 묻는다.
- 긴 빌드·테스트 로그를 메인 컨텍스트로 가져오지 않는다. 항상 verifier 요약을 쓴다.
- 커밋은 AC 단위로 작게. 세션이 끊겨도 마지막 커밋 + 내 NOW 파일로 복원 가능해야 한다.
