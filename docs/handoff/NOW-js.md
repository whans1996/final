# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-27 17:35 · 작업자: JoonseobKim · PC: js-ssafy · 브랜치: feature/issue-241-fe-deps-check

## 현재 기능

**#241 FE 의존성 점검이 불완전한 설치를 ✅로 표시** (harness, 이슈만, spec 없음) · 브랜치 `feature/issue-241-fe-deps-check` (develop 89f8f79+동기화에서 만듦, 아직 구현 커밋 없음)

## 진행

- ✅ 11/27 Figma 세션 후속을 이슈 #254로 김동환에게 넘김 (알림·A 화면 개편 로드맵 등록, 재인터뷰 5단계 UI, SD-1 범위, Figma 프레임 그릴지·#243 시점은 두 사람 결정)
- ⬜ #241 구현

## 바로 다음 할 일

1. `gh issue edit 241 --add-assignee @me` 후 `scripts/lib/env-check.mjs:160` FE 의존성 판정을 폴더 존재 → `frontend/node_modules/.package-lock.json`과 `node_modules/vite/package.json`(또는 `.bin/vite`) 존재로 바꾸고, 없으면 warn + `cd frontend && npm ci` 안내. 실패 테스트를 `scripts/test/`에 먼저 쓴다 (`node --test "scripts/test/*.test.mjs"`)
2. `scripts/doctor.mjs:71` 동기화 검사: `.package-lock.json`이 없으면 건너뛰지 말고 "설치 불완전" 경고
3. `chore(harness): ...` 커밋 → /pr-review (Closes #241, area:common이라 @whans1996 멘션)
4. 그 뒤 B 로드맵: CD-1(K-1 12/02 대기)·LP-1(SD-1 12/03 대기). 남는 시간엔 공통 follow-up

## 막힌 점 / 결정 필요
- **사람 결정**: Figma 프레임을 그릴지 (가) 생략, `/dev/components`·화면 설계서 2-1절 스크린샷을 정본으로 / (나) 화면 개편 시안용으로 그림 → 김동환과 정해 결정로그 한 줄 (#254)
- 세션 준비 문서(claude.ai, 비공개)는 사람이 김동환에게 직접 공유
- GMS 키 만료 12/09 18:00 → EV-2 전에 재발급·연장 확인. 내 이슈 #243(FOLLOW_SYSTEM)은 화면 이전 뒤

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용을 적지 않는다.** 점수·개수·SHA만
- A 영역 화면 26개의 useLatestRequest 이관은 #250 (김동환 담당). 새 조회는 `frontend/CLAUDE.md` V-4 규칙
