# NOW — 김준섭 현재 작업 스냅샷

> 김준섭(B 영역: 카드·소셜) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
갱신: 2026-11-27 16:55 · 작업자: JoonseobKim · PC: js-ssafy · 브랜치: feature/V-4-fe-latest-request

## 현재 기능

**V-4 FE 늦은 응답 무시·요청 취소** `docs/specs/V-4-fe-latest-request.md` (**DONE (PR #252)**) · 브랜치 `feature/V-4-fe-latest-request` · 로드맵 V-4

## 진행

- ✅ PR #252 생성 (develop 대상, @whans1996 공통 멘션, A 이관 #250)
- ✅ CI 초록 (Frontend·하네스·제목, Backend는 변경 없어 건너뜀), Codex ✅ 66e7989 (지적 없음)
- ✅ spec DONE·체크리스트 완료 표시 (이 문서 커밋, docs만이라 Codex 재리뷰 없음)

## 바로 다음 할 일

1. 사람이 GitHub에서 PR #252 머지 (머지 전 develop에 새 커밋이 있으면 `git merge origin/develop` → push → CI 초록 재확인)
2. 머지 뒤 `/clear` → `/pickup`: develop으로 전환, `node scripts/roadmap.mjs`로 다음 B 항목 확인 (LP-1 랜딩은 SD-1·CD-1은 K-1 대기 → 남는 시간엔 내 영역·공통 follow-up 이슈)

## 막힌 점 / 결정 필요
- 없음. GMS 키 만료 12/09 18:00 → EV-2 전에 재발급·연장 확인. 내 이슈 #243(FOLLOW_SYSTEM)은 화면 이전 뒤

## 주의

- **NOW·작업로그·PR·커밋 메시지에 eval 라벨 내용을 적지 않는다.** 점수·개수·SHA만
- A 영역 화면 26개의 useLatestRequest 이관은 #250 (김동환 담당). 새 조회는 `frontend/CLAUDE.md` V-4 규칙
