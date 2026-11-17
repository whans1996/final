# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-17 · 작업자: Claude (하네스 세팅) · 브랜치: main

## 현재 기능

없음. 하네스 세팅 완료, 프로젝트 뼈대 생성 전.

## 진행

- 하네스(CLAUDE.md, hooks, 스킬, 에이전트, scripts) ✅

## 바로 다음 할 일

1. 두 PC 모두 JDK 17 이상과 Docker Desktop을 설치하고 `node scripts/setup.mjs` 실행
2. GitLab 원격 저장소 연결: `git remote add origin <URL>` → `git push -u origin main develop`
3. 프로젝트 뼈대 생성 (`feature/F-00-scaffold` 브랜치): Spring Boot 3 + Vue 3, Spotless·ESLint·Prettier·Vitest·Playwright, `IntegrationTestBase`, `JsonTypeHandler`, `LlmClient` Mock, `V1__init.sql`

## 막힌 점 / 결정 필요

- GitLab 원격 저장소 URL 미정
- Refresh 토큰 저장 위치 (F-02 spec에서 확정)

## 주의

- 현재 PC의 Java는 1.8이다. Spring Boot 3에는 JDK 17 이상이 필요하다.
