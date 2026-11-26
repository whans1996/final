# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-26 16:11 · 작업자: JoonseobKim · PC: js-home · 브랜치: feature/V-2-eval-freeze

---

## 현재 기능

**V-2 평가 세트 DS-03·04 고정** (`feature/V-2-eval-freeze`, 로드맵 11/26~11/28) — eval 저장소 `sets.json` `frozenAt: 2026-11-26` 커밋 `3ab0db0` push 완료. 메인 저장소는 메타데이터만(datasets.json DS-03·04 status, 09-데이터셋.md 수집 일자·변경 이력)

## 진행

- ✅ eval 고정 커밋 3ab0db0 (9개 세트 라벨 9/9 reviewed)
- ✅ `data/datasets.json`, `docs/산출물/09-데이터셋.md` 갱신 → docs PR(`Roadmap: V-2`) 열기

## 바로 다음 할 일

1. V-2 docs PR CI·Codex COMPLETE → 사람이 GitHub에서 머지. 머지 뒤 김동환에게 "평가 세트 고정(3ab0db0)" 알림 (11/28 B→A 넘김)
2. 11/27 Figma 공동 세션 → F-29 `/spec` (11/29~)
3. EV-1 기준선 (11/29~, E-1 머지 후)
4. /progress 때: 기획서 579·WBS 4.1의 DS 12/03 → 11/26 고정으로

## 막힌 점 / 결정 필요

- SC-01(랜딩 구현 때): 샘플 분석 데이터 출처, 로그인 회원 `/` → 대시보드, 비회원 랜딩 인기순 공개 카드 (F-16 spec 9절)

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용(개념 tier·개수, 결함, forbidden, 채점 기준)을 적지 않는다.** 진행률·eval 커밋 SHA만
- **Figma는 공동 세션 전까지 건드리지 않는다** (김동환 요청)
- #211(F-25 A 영역 화면)은 V-1b에서 뺌, B 여유 작업과 함께
