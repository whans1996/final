# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/common-boot4-upgrade

## 현재 기능

환경 최신화 (인터뷰 결정, 계획: `C:\Users\SSAFY\.claude\plans\validated-foraging-crab.md`). PR ① BE: Boot 4.1.2·Java 21(foojay 자동 다운로드)·Gradle 9.9.1·Jackson 3·Testcontainers 2·MyBatis 4.1·springdoc 3.2·jjwt 0.13, CI java 21. PR ② FE·문서는 다음.

## 진행

- ✅ build.gradle·settings.gradle(foojay)·wrapper, Jackson 3 전환(JsonTypeHandler·JsonAuthenticationEntryPoint·Gemini/OllamaLlmClient·테스트), Boot 4 import, deprecation 0건 컴파일
- ✅ OpenApiExportTest: Jackson 3 + `"key" : value` 구분자 고정
- ⏳ `node scripts/verify.mjs be` 결과 확인 중 (미커밋)

## 바로 다음 할 일

1. verify be 실패 수정. Jackson 3 기본값(속성 정렬·날짜) 차이는 테스트 대신 설정으로 기존 출력 유지 (`JsonTypeHandlerTest`, API jsonPath 테스트)
2. `git diff docs/api/openapi.json` 검토(springdoc 3 변화) → 커밋 `chore(common): Spring Boot 4.1·Java 21·Gradle 9로 백엔드 업그레이드` → bootRun 점검 → `/pr-review`
3. PR ② `feature/common-fe-tooling-upgrade`: vitest 5·jsdom 31·oxlint 1.94·eslint-plugin-vue 10.12·prettier 3.10.1, `scripts/setup.mjs` JDK 문구, CLAUDE.md·backend/CLAUDE.md:5(Boot 4 금지 규칙 변경)·README·환경설정.md·기획서 202행 + 결정 로그

## 막힌 점 / 결정 필요

- 없음. 결정: MySQL 8.4·CI Node 24 유지, 시드 카드 Boot 3 설명은 후속 작업

## 주의

- JAVA_HOME은 17 그대로 둬도 된다 (Gradle 9 실행용). 컴파일 JDK 21은 첫 빌드 때 ~/.gradle/jdks에 자동으로 내려받는다. STS는 JDK 21 필요
- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다 (Windows 파일 잠금). V1·V2 수정 금지
