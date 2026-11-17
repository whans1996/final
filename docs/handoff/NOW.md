# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-17 · 작업자: Claude (프로젝트 뼈대) · 브랜치: feature/F-00-scaffold (PR → develop)

## 현재 기능

F-00 프로젝트 뼈대 완료 → PR 단계. spec 없이 진행한 인프라 작업 (기획서 "다음 액션 2").

## 진행

- ✅ 백엔드: Spring Boot 3.5.16, 공통 예외·JWT·Swagger·JsonTypeHandler·MockLlmClient, `V1__init.sql`(ERD P0 22개), 테스트 28개 green
- ✅ 프론트엔드: Vue 3 + Pinia + Router + axios, `api/http.js`, `stores/auth.js`, `authGuard`, 테스트 11개 green, ui-check 통과
- ✅ 하네스: setup.mjs 종료 메시지, verify `.\gradlew.bat`·UTF-8, ui-check Git Bash 경로 변환
- ✅ 문서: README 실행 방법, 환경설정(로컬 DB SQL·playwright), backend/frontend CLAUDE.md, 03-ERD, 결정 로그

## 바로 다음 할 일

1. PR(feature/F-00-scaffold → develop) CI 확인: 자동 실행이 안 되면 `gh workflow run CI --ref feature/F-00-scaffold` → 초록이면 머지 (사람)
2. 각 PC: 로컬 DB 생성(`docs/환경설정.md` 3절) + `.env` 작성 → `cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'` → Swagger 열리는지 확인 (아직 미확인)
3. `/spec F-01` (회원가입), `/spec F-02` (로그인·JWT, Refresh 전달 방식 결정) → 승인 후 `/implement`
4. W1 산출물: `02-유스케이스.md` 확정, 시드 카드(DS-05)는 `09-데이터셋.md` DS-01 규칙대로 `R__seed_concepts.sql`

## 막힌 점 / 결정 필요

- CI가 push·PR 이벤트로 자동 실행되지 않음 (PR #1부터). 저장소 Settings → Actions → General 확인 필요
- 팀원 GitHub 아이디, 제출 zip 이름용 지역·반·이름 (나중에)

## 주의

- `V1__init.sql`은 커밋됨 → 수정 금지. 컬럼 보강은 V2+ (새 V 파일 전에 `git pull`로 번호 확인)
- Boot 4로 올리지 않는다 (springdoc·Testcontainers 등 메이저 변경). PATH의 `java`는 8, 빌드는 JAVA_HOME(17)
- 평가(eval) 데이터는 프롬프트·fixture·시드 카드에 쓰지 않는다
