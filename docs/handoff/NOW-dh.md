# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-20 14:20 · 작업자: whans1996@naver.com · PC: dh-home · 브랜치: feature/F-16-recommendation

## 현재 기능

F-16 맞춤 개념 카드 추천 — `/pr-review` 진행 중 (PR 아직 안 열림)

## 진행

- ✅ verify all 통과, contract-checker 불일치 0, reviewer 반드시 수정 0, 권장 2건 반영 커밋 `928eb83` (프롬프트 근거 "3번 이상"·예시 근거만)
- ⏳ `git merge origin/develop`(B PR #61 F-17 머지분) **충돌 해결 후 머지 커밋 전**: `frontend/src/__tests__/App.spec.js` 헤더 메뉴를 대시보드→개념 카드→커뮤니티 순으로 해결·스테이징, `navItems.spec.js` 제목에 커뮤니티 추가
- ⏳ 머지 트리에서 `verify all` 실행 중 (openapi.json 태그 재생성 확인)
- ✅ PR 설명 초안 `.harness/pr-F-16.md` (스크린샷 자리 `SCREENSHOTS_PLACEHOLDER`)

## 바로 다음 할 일

1. verify all 통과 → `git add -A docs/api/openapi.json frontend` 후 `git commit --no-edit`(머지 커밋)
2. `git push -u origin feature/F-16-recommendation` → `node scripts/pr-screens.mjs SC-04 SC-11` 출력으로 `.harness/pr-F-16.md` 스크린샷 자리 교체
3. `git rev-list --count HEAD..origin/develop` 0 확인 → 사용자 확인 후 `gh pr create --base develop --title "feat(F-16): 맞춤 개념 카드 추천과 대시보드(SC-04)" --body-file .harness/pr-F-16.md`
4. `gh pr checks`, Codex 루프 `node scripts/codex-review.mjs <PR> --request --wait --status` (background)

## 막힌 점 / 결정 필요

- 사람: 이슈 #16, 노션 팀원 초대, 고아 폴더 `../final-harness` 삭제 여부
- 실제 LLM 품질은 사용자 요청 시 prompt-evaluator(RECOMMEND)

## 주의

- 공통 파일(ConceptMapper·XML·ConceptQueryService, router/index.js, V6, CODEOWNERS) → PR 본문에 @JoonseobKim 멘션 (초안에 반영됨)
- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시) — 7건 확인 완료
