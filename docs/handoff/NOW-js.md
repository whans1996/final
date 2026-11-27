# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-27 09:34 · 작업자: JoonseobKim · PC: js-ssafy · 브랜치: feature/F-29-design-common

---

## 현재 기능

**F-29 디자인 공통 PR (토큰·폰트·공통 컴포넌트·앱 셸)** — `feature/F-29-design-common` (11/29~12/02, `Roadmap: F-29`). spec 아직 없음. **11/27 Figma 세션 끝남, 결정 5줄 결정로그(11-27)** → `/spec F-29`. 병행: **EV-1 기준선** (11/29~12/01)

## 진행

- ✅ 11/27 Figma 세션(김동환 대면 인터뷰): 토큰·폰트·Tailwind 전부 이전(기반 PR + 영역별)·공통 컴포넌트 7개·앱 셸·알림 새 기능(T1)·랜딩 3개·SD-1 범위 → 결정로그 11-27 5줄
- ✅ EV-1 시범 (js-ssafy): GMS gpt-4.1-mini, DS-03-02 stage=all level=all, 33호출·실패 0, **크레딧 918** (남은 29,078). 채점 `-Pjudge=none`, 라벨=true. 보고서 `.harness/prompt-eval/20261127-085903-*` (js-ssafy 로컬)
- ✅ V-2 넘김: 김동환에게 "평가 세트 고정(3ab0db0, 9세트)" 알림 보냄 (11/27 Mattermost, 12/01 기준선 점수 전 AI 출력 PR 머지 보류 요청)
- js-ssafy: eval 저장소 `data/raw/eval/` clone(3ab0db0), ZIP `data/raw/eval-zips/DS-03-02.zip` (`git -C data/raw/eval archive --format=zip -o … 3ab0db0:repos/<id>-<slug>`)
- 러너 `-Pfixture`는 **저장소 루트 기준** 경로 (`data/raw/eval-zips/<id>.zip`, `../` 붙이면 datasets.json에 없다고 실패)

## 바로 다음 할 일

1. EV-1 전체 (**11/27 승인, 세션 끝났으니 실행, Gemini 판정 켬**): 남은 8개 fixture(DS-02-05~10은 id, DS-03-01·03은 ZIP 먼저 만듦) × stage=all × level=all, 예상 크레딧 8~10k. `-Pjudge=gemini`(DS-03-02 시범도 다시 채점), `eval/` 브랜치에 기록. `summary.md` 점수만 12/01 김동환에게
2. Figma(11/28까지, B): 토큰 스타일·공통 컴포넌트 7개·앱 셸 3폭(≥1024 옆 메뉴 / 640~1024 아이콘 메뉴 / <640 하단 탭) 프레임
3. 새 세션(Opus/high) `/spec F-29`: 결정로그 11-27 5줄 = 12/02 기반 PR 경계(Tailwind·Reka UI·Lucide·토큰·폰트·다크 토글·컴포넌트 7개·셸 2종·meta.nav group·검색은 있는 API만·알림 자리)
4. 김동환에게 요청: 로드맵·기획서에 알림 항목(T1, B 12/09~12/14) 추가 docs PR (로드맵은 A 관리), SD-1 범위 넓힘 반영
5. /progress 때: 기획서 579·WBS 4.1의 DS 12/03 → 11/26 고정으로

## 막힌 점 / 결정 필요

- GMS 키 만료 12/09 18:00 → EV-2(12/09~12/12) 전에 재발급·연장 확인

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용(개념 tier·개수, 결함, forbidden, 채점 기준)을 적지 않는다.** 진행률·eval 커밋 SHA만
- **Figma는 공동 세션 전까지 건드리지 않는다** (김동환 요청)
