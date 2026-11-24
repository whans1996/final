# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-24 09:37 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/issue-95-team-challenge-date-range

---

## 현재 기능

**#95** F-20c 팀 목표 시작일 상한 (follow-up, `feature/issue-95-team-challenge-date-range`). F-20c PR #98은 머지됨(#89 닫힘)

## 진행

- ✅ red: `TeamChallengeApiTest` AC2 케이스 추가(`2027-11-25`·`+10000-01-01` → 400, 경계 `2027-11-24` 통과) → 실패 확인
- ✅ `TeamChallengeService.validate`에 `MAX_START_AHEAD_DAYS = 365` 검사, `CreateTeamChallengeRequest.startDate` Swagger 설명 수정
- ✅ spec F-20c AC2 문구, 결정로그 11-24 한 줄 추가
- ✅ green: `TeamChallengeApiTest` 22개 + `OpenApiExportTest`(openapi.json 재생성), 커밋

## 바로 다음 할 일

1. `/pr-review` → PR 본문 `Closes #95` (새 세션 권장)
2. 다음 이슈: #96 (숫자 필드 타입 오류가 팀원에게 403보다 400 먼저), 그다음 #92

## 막힌 점 / 결정 필요

- 없음 (FE `TeamChallengeForm.vue` startDate `:max`는 선택, 서버 details로 이미 안내됨)

## 주의

- 공통 코드 변경 없음
