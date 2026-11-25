# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-25 15:13 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/common-ds03-eval-set

---

## 현재 기능

**V-2 평가 데이터 DS-03·DS-04 (12/03 고정)**. 블라인드라 메인 저장소에는 메타데이터만 올린다. 평가 세트는 private `JoonseobKim/vibenote-eval`(김동환 미초대, 기본 브랜치 `eval`)에 있고 로컬 `data/raw/eval/`에 clone한다. 다른 PC는 `cd data/raw && gh repo clone JoonseobKim/vibenote-eval eval`

## 진행

- ✅ 저장 위치·구성 확정(사용자 결정): 팀 생성 3개(DS-03-01 풀스택 게시판·엉성한 AI 코드 80~120파일, 02 소규모 Spring BE 25~40, 03 Vue FE 40~60) + 공개 DS-02-09(ecommerce-store 500파일 TS)·10(Project-Keystone 272파일 Spring+React TS)
- ✅ eval 저장소: README(규칙), `LABEL_SCHEMA.md`(files·apis·concepts must/ok/wrong·forbidden), `sets.json`, `concepts.tsv`(시드 85개)
- ✅ 메인 docs **PR #152** (head c9cf3b5): datasets.json DS-02-09·10·DS-03·04, 09-데이터셋(DS-02 표 10건·DS-03·04·5절 기록 경유 누수·수집 단계 거부), 기획서 V-2, 결정로그 2행. verifier ✅, reviewer 반드시 2건(SHA 오기·표 8건)·권장 5건 반영

## 바로 다음 할 일

1. PR #152: CI ✅ · Codex ✅ c9cf3b5 → GitHub에서 머지(사람). 머지 후 develop 최신화
2. DS-03-01 생성: `data/raw/eval/repos/DS-03-01-board-fullstack/`에 주니어 페르소나로 AI 생성(Spring Boot+MyBatis XML+Vue 3), 일부러 남긴 결함은 `generation/DS-03-01.md`에 기록 → eval 저장소 커밋. 이어서 02·03
3. 라벨: `labels/<id>.json`을 LABEL_SCHEMA대로 (DS-03-01~03, DS-02-05~10 총 9개), 12/03까지 커밋 후 `sets.json` `frozenAt` 기록
4. 11/27 Figma 공동 세션(F-29) → `/spec F-29`. V-1 MVP 감사(11/29~12/02)

## 막힌 점 / 결정 필요

- (해결 11-25) 라벨은 Claude 초안 → 김준섭 검수, 초안·검수 커밋 분리
- SC-01(랜딩 구현 때): 샘플 분석 데이터 출처, 로그인 회원 `/` → 대시보드, 비회원 랜딩 인기순 공개 카드 (F-16 spec 9절)

## 주의

- 하네스 가드가 다른 저장소에도 main push·`gh repo edit`을 막는다 → eval 저장소 기본 브랜치를 `eval`로 씀
- B 일정: F-29 11/27~12/20, U-1 12/07~14, F-28 FE 12/10~14, V-4 FE 12/03~10, DS-03·04 12/03
