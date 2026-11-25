# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-25 12:16 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/issue-91-withdraw-analyses

## 현재 기능

**V-1 전 이슈 당겨 처리** (#94·#73·#91, 각각 `feature/issue-<번호>-slug`, 모두 커밋 완료·미push). 김준섭 합의 3건은 답하지 않고 정한 대로 진행 (사용자 결정 11-25).

## 진행

- ✅ #94 `feature/issue-94-admin-tag` 00f7db2: 두 관리자 @Tag 설명 + openapi.json (AdminConceptController는 B 파일)
- ✅ #73 `feature/issue-73-json-body-bytes` 48cc6a6: `common/web/JsonBodyLimitFilter`(JSON 스트림 바이트 계수, 한도는 `max-document-length` 재사용) + `GlobalExceptionHandler.handleNotReadable` 413 + `RequestBodyLimitTest` UTF-16 2건. BE 전체 통과
- ✅ #91 `feature/issue-91-withdraw-analyses` 44fdc28: `AnalysisService.deleteAllOf`(진행 중→FAILED, soft delete+자식 행 삭제), `UserWithdrawalService.withdraw`에서 호출, `WithdrawalApiTest#AC6_탈퇴하면_내_분석은_…`, F-03 spec AC6·결정로그 1행 (사용자 결정: 실제 삭제)

## 바로 다음 할 일

1. 세 브랜치 각각 `/pr-review` → PR (모두 PR 설명 "공통 코드 변경"에 @JoonseobKim: #94 AdminConceptController, #73 common 필터·핸들러, #91 F-03 spec·UserWithdrawalService)
2. #91 PR "후속 작업": 진행 중 단계(saveFiles 등)가 탈퇴 직후 행을 저장하면 soft delete된 분석에 행이 남을 수 있음 → follow-up 이슈
3. 11/27 Figma 세션 → 11/29~ V-1 MVP 감사

## 막힌 점 / 결정 필요

- 사람: GMS 키 수령(12/01 전), 노션 팀원 초대, F-05b liveTest, 제출 zip 이름

## 주의

- 커밋 범위는 F-xx|common|harness만 (`admin` 거부됨)
- Codex: 3회차부터 P2는 follow-up 이관 · 진행현황·WBS·간트는 `/progress` docs PR에서만
