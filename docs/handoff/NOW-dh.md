# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-25 13:05 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/issue-91-withdraw-analyses

## 현재 기능

**V-1 전 이슈 당겨 처리** (#94·#73·#91, 각각 `feature/issue-<번호>-slug`. #91은 PR #142, #94·#73은 커밋 완료·미push). 김준섭 합의 3건은 답하지 않고 정한 대로 진행 (사용자 결정 11-25).

## 진행

- ✅ #94 `feature/issue-94-admin-tag` 00f7db2: 두 관리자 @Tag 설명 + openapi.json (AdminConceptController는 B 파일)
- ✅ #73 `feature/issue-73-json-body-bytes` 48cc6a6: `common/web/JsonBodyLimitFilter`(JSON 스트림 바이트 계수, 한도는 `max-document-length` 재사용) + `GlobalExceptionHandler.handleNotReadable` 413 + `RequestBodyLimitTest` UTF-16 2건. BE 전체 통과
- 🔄 #91 **PR #142** (head c7fd63e): `deleteAllOf` + reviewer 지적 반영(`lockInProgress`로 saveFiles·ConceptLinkService.save 차단, `complete()` users 먼저 잠금, SC-19 탈퇴 안내 문구·스크린샷, 09-데이터셋 6절·ERD·유스케이스·F-19 spec). 로컬 전체 검증 통과. CI·Codex 대기 중 (`node scripts/codex-review.mjs 142 --request --wait --status`)

## 바로 다음 할 일

1. PR #142 Codex 루프: COMPLETE면 CI 초록 확인 후 GitHub에서 머지(사람). FINDINGS면 지적 판단 → 테스트 먼저 → `fix(F-03): … (Codex 리뷰)`
2. `git switch feature/issue-94-admin-tag` → `/pr-review` (@JoonseobKim: AdminConceptController), 이어서 `feature/issue-73-json-body-bytes` → `/pr-review` (@JoonseobKim: common 필터·핸들러)
3. 11/27 Figma 세션 → 11/29~ V-1 MVP 감사

## 막힌 점 / 결정 필요

- 사람: GMS 키 수령(12/01 전), 노션 팀원 초대, F-05b liveTest, 제출 zip 이름

## 주의

- 커밋 범위는 F-xx|common|harness만 (`admin` 거부됨)
- Codex: 3회차부터 P2는 follow-up 이관 · 진행현황·WBS·간트는 `/progress` docs PR에서만
