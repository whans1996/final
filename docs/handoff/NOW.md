# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-17 · 작업자: Claude (데이터 계획·환경) · 브랜치: feature/data-plan (PR → develop)

## 현재 기능

없음. 데이터 출처 조사·데이터 활용 계획서(`docs/산출물/09-데이터셋.md`, `data/datasets.json`) 완료, GitHub 원격(whans1996/final, private) 연결 완료. 프로젝트 뼈대 생성 전.

## 진행

- 하네스 ✅ · 기획서 명세서 반영 ✅ · 산출물 01·04·05·09(계획) ✅, 02·03 초안, 06~08·10 골격 · origin push ✅ · JAVA_HOME(사용자)=JDK 17 ✅

## 바로 다음 할 일

1. 이 PR(feature/data-plan)의 CI 확인 후 develop에 머지 (사람)
2. WSL·Docker Desktop 설치 (`docs/환경설정.md` 2절, 관리자·재부팅) → Claude Code 재시작 → `node scripts/setup.mjs` 전부 ✅
3. 프로젝트 뼈대 (`feature/F-00-scaffold`): Spring Boot 3(Security·JWT 골격, springdoc + `OpenApiConfig` + `OpenApiExportTest`, Flyway `V1__init.sql`, `IntegrationTestBase`, `JsonTypeHandler`, `LlmClient` Mock) + Vue 3(`lint:check` 포함)
4. W1 산출물: `02-유스케이스.md` 확정, `V1__init.sql`로 ERD 확정(`concepts.reference_url` 포함), 시드 카드(DS-05)는 `09-데이터셋.md` DS-01 카드 작성 규칙대로

## 막힌 점 / 결정 필요

- **CI가 push·PR 이벤트로 자동 실행되지 않음** (2026-11-17, PR #1): check suite 자체가 생성되지 않음. 수동 실행(`gh workflow run CI --ref <브랜치>`)은 정상·초록. 저장소 Settings → Actions → General과 GitHub 상태 확인 필요. 그 전까지 PR마다 수동 실행으로 확인
- 팀원 GitHub 아이디(collaborator 초대·리뷰어), 제출 zip 이름용 지역·반·이름 (나중에)
- Refresh 토큰 저장 위치 (F-02 spec에서 확정)

## 주의

- 이 PC: 사용자 JAVA_HOME=JDK 17, PATH의 `java`는 8(Zulu, 다른 수업용으로 유지). setx 이후 재시작해야 반영
- 평가(eval) 저장소는 프롬프트·fixture·시드 카드에 쓰지 않는다 (`data/datasets.json` split)
- 공식 명세서 PDF는 대외비라 저장소에 올리지 않는다
