# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-27 09:07 · 작업자: JoonseobKim · PC: js-ssafy · 브랜치: feature/F-29-design-common

---

## 현재 기능

**F-29 디자인 공통 PR (토큰·폰트·공통 컴포넌트·앱 셸)** — `feature/F-29-design-common` (11/29~12/02, `Roadmap: F-29`). spec 아직 없음. 11/27 Figma 공동 세션 뒤 `/spec F-29`. 병행: **EV-1 기준선** (11/29~12/01)

## 진행

- ✅ 11/27 Figma 세션 준비 문서 (claude.ai "11/27 Figma 세션 준비: F-29 현황 조사 · 랜딩 결정 3개", 김동환에게 공유 필요)
- ✅ EV-1 시범 (js-ssafy): GMS gpt-4.1-mini, DS-03-02 stage=all level=all, 33호출·실패 0, **크레딧 918** (남은 29,078). 채점 `-Pjudge=none`, 라벨=true. 보고서 `.harness/prompt-eval/20261127-085903-*` (js-ssafy 로컬)
- ✅ V-2 넘김: 김동환에게 "평가 세트 고정(3ab0db0, 9세트)" 알림 보냄 (11/27 Mattermost, 12/01 기준선 점수 전 AI 출력 PR 머지 보류 요청)
- js-ssafy: eval 저장소 `data/raw/eval/` clone(3ab0db0), ZIP `data/raw/eval-zips/DS-03-02.zip` (`git -C data/raw/eval archive --format=zip -o … 3ab0db0:repos/<id>-<slug>`)
- 러너 `-Pfixture`는 **저장소 루트 기준** 경로 (`data/raw/eval-zips/<id>.zip`, `../` 붙이면 datasets.json에 없다고 실패)

## 바로 다음 할 일

1. 11/27 Figma 세션: 준비 문서 체크리스트대로 진행 → 결정을 결정로그에 한 줄씩
2. 새 세션(Opus/high) `/spec F-29`: 세션 결정 + 준비 문서의 토큰 표·컴포넌트 후보 표 기준
3. EV-1 전체 (**11/27 승인: Figma 세션 끝나면 실행, Gemini 판정 켬**): 남은 8개 fixture(DS-02-05~10은 id, DS-03-01·03은 ZIP) × stage=all × level=all, 예상 크레딧 8~10k. 채점 `-Pjudge=gemini`(DS-03-02 시범도 다시 채점), `eval/` 브랜치에 기록. `summary.md` 점수만 12/01 김동환에게
4. /progress 때: 기획서 579·WBS 4.1의 DS 12/03 → 11/26 고정으로

## 막힌 점 / 결정 필요

- SC-01 결정 3개(준비 문서 추천: FE 고정 JSON·DS-02-04 demo / 회원 `/`→대시보드 / 비회원 인기순 공개 카드) → 11/27 김동환과 확정, SD-1 형식 합의
- GMS 키 만료 12/09 18:00 → EV-2(12/09~12/12) 전에 재발급·연장 확인

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용(개념 tier·개수, 결함, forbidden, 채점 기준)을 적지 않는다.** 진행률·eval 커밋 SHA만
- **Figma는 공동 세션 전까지 건드리지 않는다** (김동환 요청)
