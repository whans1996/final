# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-26 14:00 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/issue-169-concept-updated-at

---

## 현재 기능

**V-1 MVP 감사 (B 몫, 앞당겨 11-26)** — reviewer 3개(팀 / 커뮤니티·팔로우·피드 / 개념 카드)로 B 코드 전체 감사: P1·P2 0건, P3 5건 코드 확인 후 이슈 #168~#172(area:B). #171·#172는 PR #194로 머지·닫힘(11-26). 다음 #169 브랜치 `feature/issue-169-concept-updated-at` (코드 변경 없음). V-2(DS-03·04)는 생성·검수 끝, 12/03 고정만 남음 (private `JoonseobKim/vibenote-eval`, 로컬 `data/raw/eval/`)

## 진행

- ✅ #171·#172 PR #194 머지 (b078b35), 이슈 닫힘 확인
- ↪️ #168 F-20c 팀 경합 500은 PR #176(team 영역 A 이관)으로 area:A·김동환 담당 → 내 목록에서 뺌
- ⬜ 남은 내 이슈: #169 F-13 updated_at 자동 갱신(mapper/concept 공통 → PR에 @whans1996), #170 F-04 시각 오프셋(user/ DTO 공통 → 김동환과 먼저 합의)

## 바로 다음 할 일

1. #169 (이 브랜치): `ConceptWishMapper.refreshWishCount`·`ConceptReviewMapper.refreshAvgRating` XML SET에 `updated_at = updated_at` 추가. 먼저 찜·리뷰 뒤 `concepts.updated_at` 불변 테스트(red) → 수정 → `/pr-review`, 공통 파일이라 PR에 @whans1996
2. 김동환에게: 이전 원격 브랜치 `feature/common-ds03-labels` 열람 여부 확인(라벨 내용은 말하지 않음), #170 user DTO 오프셋 변경 합의
3. 11/27 Figma 공동 세션(F-29) → 새 세션(Opus/high) `/spec F-29` (`node scripts/branches.mjs F-29` 먼저). 12/03 eval `sets.json` `frozenAt`

## 막힌 점 / 결정 필요

- SC-01(랜딩 구현 때): 샘플 분석 데이터 출처, 로그인 회원 `/` → 대시보드, 비회원 랜딩 인기순 공개 카드 (F-16 spec 9절)

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용(개념 tier·개수, 결함, forbidden, 채점 기준)을 적지 않는다.** 진행률·eval 커밋 SHA만. 감사 에이전트에도 `data/raw/eval/` 금지를 명시함
- **Figma는 공동 세션 전까지 건드리지 않는다** (김동환 요청)
- B 일정: F-29 11/27~12/20, U-1 12/07~14, F-28 FE 12/10~14, V-4 FE 12/03~10, DS-03·04 12/03
