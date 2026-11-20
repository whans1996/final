# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-20 14:04 · 작업자: whans1996@naver.com · PC: dh-home · 브랜치: feature/F-16-recommendation

## 현재 기능

F-16 맞춤 개념 카드 추천 — spec `docs/specs/F-16-recommendation.md` APPROVED, **AC1~AC10 구현·테스트 green**, `/pr-review` 전

## 진행

- ✅ BE AC1~AC7 (bf9fa54), FE AC8~AC10 (cfcb696), verify be·fe 통과
- ✅ contract-checker 불일치 0, ui-verifier 통과(SC-04·SC-11 빈 상태 데스크톱·375px, 로그인 후 `/dashboard`, 기간 이동 시 추천 재호출 없음), 스크린샷 4장
- ✅ 산출물 01·02(UC32)·03(signal_key)·06(SC-04 절 신설, SC-11)·08 반영, mock fixture를 근거와 무관한 일반 문장으로 교체

## 바로 다음 할 일

1. `/clear` 후 `/pr-review` (Opus/high): verifier → reviewer → PR. 직전에 `git rev-list --count HEAD..origin/develop` 확인 (B PR #61 F-17이 열려 있음, 먼저 머지되면 openapi.json 태그 순서 재생성)
2. PR 본문: 공통 파일(ConceptMapper·XML·ConceptQueryService, router/index.js, V6, CODEOWNERS) @JoonseobKim 멘션, 실제 LLM 품질은 사용자에게 prompt-evaluator(RECOMMEND) 제안
3. 참고: SC-04 스크린샷의 이유 문장은 fixture 교체 전 문장이다 (레이아웃은 같음)

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부
- 공통 `ConceptMapper`에 F-16 블록(`findAllPublishedSummaries`, `findWishedByUser`) 추가 → PR에 @JoonseobKim 멘션
- backend/CLAUDE.md MyBatis 절은 "추천 집계는 삭제 분석을 거르지 않는다"인데 F-16 spec(승인)은 거른다 → spec을 따르고 CLAUDE.md 문구를 고친다

## 주의

- `router/index.js`(공통)를 F-16에서 고친다(`redirectTarget` 기본값, import 한 줄) → PR @JoonseobKim
- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시)
