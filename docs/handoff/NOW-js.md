# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-25 23:12 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/common-v2-eval-progress

---

## 현재 기능

**V-2 평가 데이터 DS-03·DS-04 (12/03 고정)** — DS-03 생성 3/3, DS-04 라벨 초안 9/9·검수 9/9 완료. 블라인드라 메인 저장소에는 존재·진행률·고정 시점만 적는다(09-데이터셋 5절 기록 경유 누수). 평가 세트는 private `JoonseobKim/vibenote-eval`(김동환 미초대, 기본 브랜치 `eval`)에 있고 로컬 `data/raw/eval/`에 clone한다. 다른 PC는 `cd data/raw && gh repo clone JoonseobKim/vibenote-eval eval`

## 진행

- ✅ 메인 docs **PR #152** 머지(11-25): datasets.json DS-02-09·10·DS-03·04, 09-데이터셋, 기획서 V-2, 결정로그
- ✅ DS-03-01~03 생성·eval push. DS-04 라벨 초안 9/9(eval `dbd1eaf`) → 김준섭 검수 9/9(eval 마지막 `ff79d14`), 초안·검수 커밋 분리. 세부 내용·AI 활용 원문은 eval `notes/`·`generation/`에만 있다
- ✅ 기록 정리(11-25): 이전 브랜치 `feature/common-ds03-labels`의 NOW·작업로그에 라벨 요약이 들어가 이 브랜치로 다시 씀. datasets.json DS-02-08 stack 정정만 옮김, 09-데이터셋 변경 이력에 기록

## 바로 다음 할 일

1. **PR #162** Codex ✅ 10d0d6c (이후 docs만) → CI 초록 확인 뒤 GitHub에서 머지 (wip 없음, 일반 머지). 상대 PR(#161)이 먼저 머지되면 `git merge origin/develop` → push → CI 재확인. 원격 `feature/common-ds03-labels`는 삭제함(로컬에만 남음)
2. 김동환에게 이전 원격 브랜치를 열람하지 않았는지 확인 요청 (라벨 내용은 말하지 않는다)
3. 12/03에 eval `sets.json`에 `frozenAt` 기록·push (V-2 고정)
4. 11/27 Figma 공동 세션(F-29) → `/spec F-29`. V-1 MVP 감사(11/29~12/02)
5. 평가 종료 뒤(W5) eval `notes/main-repo-log-2026-11-25.md`의 AI 활용 사례를 작업로그·08 산출물로 옮김

## 막힌 점 / 결정 필요

- SC-01(랜딩 구현 때): 샘플 분석 데이터 출처, 로그인 회원 `/` → 대시보드, 비회원 랜딩 인기순 공개 카드 (F-16 spec 9절)

## 주의

- **NOW·작업로그·PR·커밋 메시지에 라벨 내용(개념 tier·개수, 결함, forbidden, 채점 기준)을 적지 않는다.** 진행률·eval 커밋 SHA만
- 라벨 JSON은 손으로 정렬된 파일이 있다 → `JSON.stringify`로 다시 쓰지 말고 원문 문자열을 치환한 뒤 파싱 결과가 같은지 확인한다
- **Figma는 공동 세션 전까지 건드리지 않는다** (김동환 요청)
- 하네스 가드가 다른 저장소에도 main push·`gh repo edit`을 막는다 → eval 저장소 기본 브랜치를 `eval`로 씀
- B 일정: F-29 11/27~12/20, U-1 12/07~14, F-28 FE 12/10~14, V-4 FE 12/03~10, DS-03·04 12/03
