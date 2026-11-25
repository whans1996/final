# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-25 13:50 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/issue-73-json-body-bytes

## 현재 기능

**V-1 전 이슈 당겨 처리** (#94·#73·#91). #91 PR #142·#94 PR #144 머지(이슈 닫힘). #73 **PR #145** 열림. 김준섭 합의 3건은 답하지 않고 정한 대로 진행 (사용자 결정 11-25).

## 진행

- 🟡 #73 **PR #145** (head 00f7791, develop 합침, verify all ✅, reviewer 반드시 수정 0·권장 3 반영): `common/web/JsonBodyLimitFilter`(JSON 스트림 바이트 계수) + `GlobalExceptionHandler.handleNotReadable` 413 + `RequestBodyLimitTest` 5건 + `JsonBodyLimitFilterTest` 4건. @JoonseobKim 멘션. **Codex ✅ 00f7791** (지적 없음), BE CI 대기
- ✅ #94 PR #144, #91 PR #142 develop 머지 (11-25)

## 바로 다음 할 일

1. PR #145: `gh pr checks 145`로 Backend CI 초록 확인 → GitHub에서 머지(사람). 그 전에 develop이 바뀌었으면 `git merge origin/develop` → push → CI 재확인
2. 11/27 Figma 세션 → 11/29~ V-1 MVP 감사

## 막힌 점 / 결정 필요

- 사람: GMS 키 수령(12/01 전), 노션 팀원 초대, F-05b liveTest, 제출 zip 이름

## 주의

- 커밋 범위는 F-xx|common|harness만 (`admin` 거부됨)
- Codex: 3회차부터 P2는 follow-up 이관 · 진행현황·WBS·간트는 `/progress` docs PR에서만
