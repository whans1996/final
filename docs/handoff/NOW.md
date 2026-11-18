# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/common-boot4-upgrade

## 현재 기능

환경 최신화 (인터뷰 결정, 계획: `C:\Users\SSAFY\.claude\plans\validated-foraging-crab.md`). PR ① BE: Boot 4.1.2·Java 21(foojay 자동 다운로드)·Gradle 9.9.1·Jackson 3·Testcontainers 2·MyBatis 4.1·springdoc 3.2·jjwt 0.13, CI java 21. PR ② FE·문서는 다음.

## 진행

- ✅ build.gradle·settings.gradle(foojay)·wrapper, Jackson 3 전환(JsonTypeHandler·JsonAuthenticationEntryPoint·Gemini/OllamaLlmClient·테스트), Boot 4 import, deprecation 0건 컴파일
- ✅ OpenApiExportTest: Jackson 3 + `"key" : value` 구분자 고정 → openapi.json 변화 없음
- ✅ verify be 통과, bootRun(8081) 기동·Swagger·에러 응답 정상, reviewer 반드시 수정 0건 (권장: asInt(0), JsonTypeHandler javadoc 반영)
- ✅ PR #12 생성. CI 초록 (1차 Spotless 실패: 로컬 JDK 17·CI 21이 google-java-format 기본 버전을 다르게 골라 생김 → 1.28.0 고정, 10c251a). Codex ✅ 10c251a
- ⏳ PR #12 머지 대기 (영역 오너가 GitHub에서)

## 바로 다음 할 일

1. (사람) PR #12 머지 → `/clear` → 새 세션 `/pickup`
2. PR ② `feature/common-fe-tooling-upgrade`(develop에서): vitest 5·jsdom 31·oxlint 1.94·eslint-plugin-vue 10.12·prettier 3.10.1, `scripts/setup.mjs` JDK 문구, CLAUDE.md·backend/CLAUDE.md:3,5,72,126(Boot 4 금지 규칙 변경 + Jackson 규칙: `com.fasterxml.jackson.databind/core` import 금지(annotation은 허용), JSON 컬럼 타입은 null 가능하면 래퍼)·README·환경설정.md·기획서 202행 + 결정 로그, 작업로그 AI 활용 줄

## 막힌 점 / 결정 필요

- 없음. 결정: MySQL 8.4·CI Node 24 유지, Jackson 3 기본값 따름(use-jackson2-defaults 안 씀), 시드 카드 Boot 3 설명은 후속 작업

## 주의

- JAVA_HOME은 17 그대로 둬도 된다 (Gradle 9 실행용). 컴파일 JDK 21은 첫 빌드 때 ~/.gradle/jdks에 자동으로 내려받는다. STS는 JDK 21 필요
- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다 (Windows 파일 잠금). V1·V2 수정 금지
