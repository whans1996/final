# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.

갱신: 2026-11-24 11:52 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-21a-structure-graph

## 현재 기능

**F-21a 구조 시각화: 파일 의존 그래프** (P1) spec `docs/specs/F-21a-structure-graph.md` — **APPROVED** (whans1996 11-24), 브랜치 `feature/F-21a-structure-graph`, 구현 전

## 진행

- ✅ F-25 PR #106 머지(11-24), #99·#96·#92 닫힘 확인. P0 21개 전부 DONE
- ✅ F-21을 a(파일 의존 그래프)·b(메서드 단위 결정적 흐름)로 분할(사용자 결정). F-21a spec AC17개: JavaParser·JS 토크나이저·MyBatis XML, FE→API는 HTTP 호출 위치·URL 값 추적·메서드·세그먼트·가장 구체적 API로만 확정, 미확정은 `unresolved` 사유 목록. LLM·DB 변경 없음, 심볼 색인 저장 안 함. AI(구조 해설·F-07 흐름 검증)는 F-21b로. 결정로그 1줄

## 바로 다음 할 일

1. `/clear` → `/implement F-21a` (Opus/high): `JavaStructureReaderTest#AC1_…`부터 red. F-21b `/spec`은 F-21a 머지 뒤

## 막힌 점 / 결정 필요

- 사람: 노션 팀원 초대, #77 담당(F-08 A·F-13 B 걸침)

## 주의

- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시) · 하네스 동결(11-23): 버그·작업 막는 문제만 고친다
- mermaid는 12.x 중 출시 2주 이상 지난 정확한 버전으로 고정 (12.3.0은 11-11 출시)
- 내 영역 이슈 #104·#105(F-25 집계)는 F-21 뒤 기능 사이에 처리
