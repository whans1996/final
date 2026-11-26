# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-26 16:02 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/issue-208-comment-page

---

## 현재 기능

**#208 F-18 댓글 200개 초과** (`feature/issue-208-comment-page`, 로드맵 V-1b 나머지) — **최근 200개를 오래된순으로** 보여 주기로 결정(11-26, 사용자). API 형식은 그대로. V-1b의 F-17 묶음(PR #215)은 머지됨, #206 #207 #209 #210 닫힘

## 진행

- ✅ BE `CommentMapper.xml#findByPost`: 서브쿼리 `ORDER BY created_at DESC, id DESC LIMIT 200` → 바깥 `ORDER BY created_at, id`. 테스트 `CommentListApiTest#AC2_issue208_…` 2건(시각 다름·같은 시각 동점)
- ✅ FE `PostComments.vue` `hiddenCount`로 목록 **위** "이전 댓글 n개는 보이지 않습니다". 테스트 `PostComments.spec.js` AC11 #208
- ✅ spec F-18, 화면설계서 SC-15, 유스케이스 UC41, 결정로그 1줄, openapi 설명. verify all ✅, reviewer 반드시 0·권장 3 반영(ea2eab6), contract 0건
- 🔀 **PR #216** (`Roadmap: V-1b`, `Closes #208`, @whans1996 멘션) — **Codex ✅ ea2eab6**, CI Backend 대기 (나머지 초록)

## 바로 다음 할 일

1. PR #216 CI Backend 초록이면 **사람이 GitHub에서 머지** (그 전 develop 새 커밋이면 `git merge origin/develop` → push) → `git switch develop && git pull`, #208 닫힘 확인 (spec은 이미 DONE이라 10단계 완료 표시 없음)
2. V-2 DS-03·04 고정 **11/28** (결정로그 11-26 R-1): eval `sets.json` `frozenAt` 기록·push → 메타데이터 docs PR(`Roadmap: V-2`). 라벨 9/9 reviewed
3. 11/27 Figma 공동 세션 → F-29 `/spec` (11/29~)

## 막힌 점 / 결정 필요

- SC-01(랜딩 구현 때): 샘플 분석 데이터 출처, 로그인 회원 `/` → 대시보드, 비회원 랜딩 인기순 공개 카드 (F-16 spec 9절)

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용(개념 tier·개수, 결함, forbidden, 채점 기준)을 적지 않는다.** 진행률·eval 커밋 SHA만
- **Figma는 공동 세션 전까지 건드리지 않는다** (김동환 요청)
- #211(F-25 A 영역 화면)은 V-1b에서 뺌, B 여유 작업과 함께
- B 일정: V-1b·V-2 ~11/28, EV-1 11/29~12/01, F-29 11/29~12/02, V-4 FE 12/03~08, U-1 12/09~14. 기획서 579·WBS 4.1의 DS 12/03은 /progress 때 11/28로
