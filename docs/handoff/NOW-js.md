# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-27 16:49 · 작업자: JoonseobKim · PC: js-ssafy · 브랜치: feature/V-4-fe-latest-request

## 현재 기능

**V-4 FE 늦은 응답 무시·요청 취소** `docs/specs/V-4-fe-latest-request.md` (**APPROVED** 김준섭 11-27) · 브랜치 `feature/V-4-fe-latest-request` · 로드맵 V-4 (12/03~12/08, 앞당김)

## 진행

- ✅ AC1~AC11 green, `verify all` 통과, ui-verifier 공개 화면 5개 문제 0건
- ✅ /pr-review: reviewer 반드시 수정 1건(`AdminChallengeListView.spec.js` "V-4 AC5 …" 테스트, da732cf) + 권장 3건(가드 테스트 begin·isCurrent, `ConceptDetailView.spec.js` "AC9 A에서 찜을 누르고…", PostWriteView 주석, b3a59b7) 반영
- ✅ PR 설명 `.harness/pr-V-4.md` (Roadmap V-4 검사 통과, @whans1996 공통 멘션, A 이관 #250)
- ⏳ 사용자 확인 대기: push·PR 생성 (제목 `refactor(V-4): FE 늦은 응답 무시·요청 취소 공통 패턴 (useLatestRequest)`)

## 바로 다음 할 일

1. 사용자 확인 후 `git push -u origin feature/V-4-fe-latest-request` (timeout 600000) → `gh pr create --base develop --title "refactor(V-4): …" --body-file .harness/pr-V-4.md`
2. `gh pr checks` 확인 → `node scripts/codex-review.mjs <PR> --request --wait --status` (run_in_background) Codex 루프
3. COMPLETE 뒤 spec 체크리스트(reviewer 반영·산출물 없음·CI 초록) 체크 + DONE(PR #n) + NOW·작업로그를 한 커밋으로

## 막힌 점 / 결정 필요
- 없음. GMS 키 만료 12/09 18:00 → EV-2 전에 재발급·연장 확인. 내 이슈 #243(FOLLOW_SYSTEM)은 화면 이전 뒤

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용을 적지 않는다.** 점수·개수·SHA만
- AC9 카드 상세 테스트는 WishButton 가드를 빼도 통과한다 (버튼이 내려가 Vue가 emit을 버림). 가드 자체는 `WishButton.spec.js` AC9가 검증
- 테스트에서 `wrapper.unmount()`하면 vue-router가 현재 위치를 '/'로 돌린다 → 이동 여부는 `router.replace` spy로 본다
