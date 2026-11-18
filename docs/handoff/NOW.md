# NOW — 현재 작업 스냅샷

> 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "바로 다음 할 일"은 다른 사람이 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).

갱신: 2026-11-18 · 작업자: whans1996 (Claude, SSAFY 교육장 PC) · 브랜치: feature/F-05a-analysis-zip

## 현재 기능

F-05a 분석 요청(ZIP 업로드 + 수집·필터·선별 파이프라인 + 상태·목록 + FE) — spec APPROVED(whans1996 11-18), 구현 전. PR #12·#13(환경 최신화)은 11-18 develop 머지 완료.

## 진행

- ✅ 사용자 결정: F-05를 F-05a(ZIP)·F-05b(URL)로 분할, 일일 한도 5회(오늘 KST, FAILED 제외, soft delete 포함)
- ✅ `docs/specs/F-05a-analysis-request-zip.md` DRAFT 작성 (AC 16개, V3 `analyses.error_code`, 새 에러 코드 ANALYSIS_400/404/413/415/429/500)
- ✅ `docs/기획서.md` 결정 로그 1줄 추가
- ✅ spec 승인, `docs(F-05a): spec 승인` 커밋
- ✅ 하네스: commit-msg hook·CI PR 제목·`featureIdFrom`이 `F-05a` 범위를 거부해 소문자 접미사 1자 허용 (dc9dcde). 커밋·PR 제목은 `feat(F-05a): ...`

## 바로 다음 할 일

1. `/clear` → `/implement F-05a`: V3 마이그레이션 → `AnalysisControllerTest#AC1_ZIP을_올리면_202와_analysisId를_받는다` red부터
2. F-05a 머지 후 `/spec F-05b`(URL 수집), 그다음 F-09·F-10 `/spec`(B, category 필터 6종)

## 막힌 점 / 결정 필요

- 없음

## 주의

- 파이프라인 테스트는 `run(analysisId)` 직접 호출로 동기 검증(커밋 후 시작이라 `@Transactional` 테스트에서는 afterCommit이 안 탐). 테스트 ZIP은 코드로 생성(eval 데이터 금지)
- 후속(PR #13 본문): 시드 카드 Boot 3 테스트 API·`JsonProcessingException`·"빌드는 17" 문구 갱신, Lombok 제거 검토
- verify(Gradle)를 bootRun·다른 Gradle과 동시에 돌리지 않는다 (Windows 파일 잠금). V1·V2 수정 금지
