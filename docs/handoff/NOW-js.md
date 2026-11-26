# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-26 14:52 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/issue-170-offset

---

## 현재 기능

**#170 F-04 응답 시각 오프셋** (`feature/issue-170-offset`) — 김동환 합의 댓글(11-26 14:38): DTO 4개 `FeedItemResponse`·`FollowUserResponse`·`MyProfileResponse`·`UserProfileResponse`를 `OffsetDateTime`(`atZone(ClockConfig.ZONE)`)으로. 조건: PR에서 @whans1996 멘션, openapi.json·F-04 spec 예시 갱신

## 진행

- ✅ red 5건 확인(`FeedApiTest` AC6·AC7, `FollowQueryApiTest` AC5, `UserProfileApiTest` AC1·AC2 기대값 `+09:00`) → DTO 4개 `OffsetDateTime` + Swagger example, spotlessApply
- ✅ spec 예시 F-04(3곳)·F-03(2곳), FE fixture(`FeedItem.spec.js`·`UserFollowsView.spec.js`·`MySettingsView.spec.js`·`UserProfileView.spec.js`) `+09:00`

## 바로 다음 할 일

1. ✅ `bd3b1b6` 커밋 + develop 병합(e79cb0d), `/pr-review` 2단계: verify all ✅, contract-checker 지적 4건 반영(미커밋: `utils/relativeTime.js` 주석, `relativeTime.spec.js` 입력·NOW `+09:00`, `FeedView.spec.js` fixture, `UserProfileView.vue` 주석)
2. reviewer 결과 반영 → `fix(F-04): FE 시각 주석·테스트를 오프셋 형식으로 정리` 커밋 → push → `gh pr create --base develop --title "fix(F-04): 피드·팔로우·프로필 응답 시각에 오프셋 추가" --body-file .harness/pr-issue-170.md` (사용자 확인 후) → Codex 루프
3. 11/27 Figma 공동 세션 → 새 세션(Opus/high)에서 `node scripts/branches.mjs F-29` → `/spec F-29`
4. DS-03·04 고정일 확인: 기획서 7절 표(`docs/기획서.md:695`)는 11/28, 이 NOW는 12/03이었음

## 막힌 점 / 결정 필요

- SC-01(랜딩 구현 때): 샘플 분석 데이터 출처, 로그인 회원 `/` → 대시보드, 비회원 랜딩 인기순 공개 카드 (F-16 spec 9절)

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용(개념 tier·개수, 결함, forbidden, 채점 기준)을 적지 않는다.** 진행률·eval 커밋 SHA만
- **Figma는 공동 세션 전까지 건드리지 않는다** (김동환 요청)
- B 일정: F-29 11/27~12/20, U-1 12/07~14, F-28 FE 12/10~14, V-4 FE 12/03~10, DS-03·04 고정(위 4번 확인)
