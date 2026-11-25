# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-25 17:46 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-26a-index-selection

## 현재 기능

**F-26a 수집·선별 개편** `/pr-review` 8단계 (Codex 루프). **PR #157** (develop 대상) 열림, head `b5cabc6`. spec APPROVED, AC1~AC11 ✅

## 진행

- ✅ AC1~AC11 구현 (`a00e0ee`~`68e7dea`), 로컬 verify all 통과 (BE 1233 + FE 717), pre-push 통과
- ✅ PR 전 점검 반영: `05ac577` status errorCode Swagger 설명 + openapi.json (contract-checker) · `e7fcd80` ZipArchiveReader `Candidate.bundle` 한 번만 판정·루트 README 묶음 0 우선, `findIndexFiles`는 README만 content(CASE WHEN), `AnalysisService.chunks` 100행/400만 자, ErrorCode 413 주석 · `b5cabc6` spec 5.2·5.5·AC11, UC14, 결정로그 11-25 행
- 새 테스트: `ZipArchiveReaderTest#AC2_루트_README는_doc이어도_개수_상한보다_먼저_담는다`, `AnalysisPipelineTest#F26a_AC9_개요용_인덱스_조회는_루트_README만_내용을_읽는다`, `AnalysisServiceChunkTest` 3개
- 후속 이슈: #155 FE·화면설계서·F-05a의 "200개 · 10MB" 문구, #156 readingOrder 미선별 경로 버려짐

## 바로 다음 할 일

1. `node scripts/codex-review.mjs 157 --request --wait --status`(백그라운드) 결과 확인 → FINDINGS면 spec·보안 위반만 고치고 P2는 follow-up 이관 (`/pr-review` 8단계)
2. `gh pr checks 157` CI 초록 확인 → Codex COMPLETE면 `docs(F-26a): 완료 표시 (spec DONE)` (spec 헤더 `DONE (PR #157)`, 체크리스트 "PR CI 초록"·"상태 DONE") → 사람이 GitHub에서 머지

## 막힌 점 / 결정 필요

- 사람: GMS 키 수령(12/01 전), 노션 팀원 초대, F-05b liveTest, 제출 zip 이름

## 주의

- 커밋 범위는 F-xx|common|harness만 · Git Bash에서 `/pr-review` 같은 인자는 MSYS 경로 변환됨 (`MSYS_NO_PATHCONV=1`)
- PR #157이 공통 파일(ErrorCode·V12·application.yml·WithdrawalApiTest·openapi.json)을 바꿈 → 본문에 @JoonseobKim 멘션함. 상대가 먼저 머지하면 머지 전 `git merge origin/develop` 후 push
- node 스크립트로 Java 편집 시 백슬래시가 깨짐 → Edit 도구 사용 · 이 PC에 python 없음
