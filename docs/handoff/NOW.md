# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-17 · 작업자: Claude (하네스 GitHub 전환) · 브랜치: feature/harness-github

## 현재 기능

없음. 하네스 GitHub 전환·Swagger 규칙·산출물 체계·기획서 보강(F-24·25 등) 완료, 프로젝트 뼈대 생성 전.

## 진행

- 하네스(GitHub PR·CI, 제출 원격 보호) ✅ · 기획서 명세서 반영 ✅ · 산출물 01·04·05 ✅, 02·03 초안, 06~10 골격

## 바로 다음 할 일

1. 두 PC에 JDK 17 이상·Docker Desktop 설치 → `node scripts/setup.mjs`
2. GitHub private 저장소 생성 → `git remote add origin <URL>` → `git push -u origin main develop` → 팀원 collaborator 초대
3. 프로젝트 뼈대 (`feature/F-00-scaffold`, `chore(common)` 커밋): Spring Boot 3(Security·JWT 골격, springdoc + `OpenApiConfig` + `OpenApiExportTest`, Flyway `V1__init.sql`, `IntegrationTestBase`, `JsonTypeHandler`, `LlmClient` Mock) + Vue 3(`lint:check` 스크립트 포함). 첫 PR로 CI 동작 확인
4. 산출물 W1 마감분: `02-유스케이스.md` 검토·확정, ERD를 `V1__init.sql`로 옮기며 `03-ERD.md` 갱신

## 막힌 점 / 결정 필요

- GitHub 저장소 URL, 팀원 GitHub 아이디(PR 리뷰어 지정용)
- Refresh 토큰 저장 위치 (F-02 spec에서 확정)
- 제출 zip 이름용 지역·반·이름

## 주의

- 현재 PC의 Java는 1.8이다. Spring Boot 3에는 JDK 17 이상이 필요하다.
- 공식 명세서 PDF는 대외비라 저장소에 올리지 않는다.
