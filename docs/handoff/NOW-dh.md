# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-25 09:20 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-21a-structure-graph

## 현재 기능

**F-21a 구조 시각화: 파일 의존 그래프** (P1) spec `docs/specs/F-21a-structure-graph.md` — APPROVED. PR #118 (**CI 초록 + Codex COMPLETE(13차 P2 이관), spec DONE 커밋 → 사람이 GitHub에서 머지 대기**). 11-24 사용자 결정: Codex 지적을 하나씩 막지 않고 전수 점검 결과로 **구조를 바꿔 한 번에** + 캐시(#117) 흡수 → spec **5.3절**이 작업 목록, 결정로그 11-24 행

## 진행

- ✅ Codex 1~9차 지적 반영(재리뷰 9회) → 전수 점검 약 40건을 spec 5.3절 1~7로 정리 (11-24 결정)
- ✅ 5.3절 1~7 모두 완료 (각 항목 끝에 spec 구현 메모): 1 `d796d18` 이름 UNKNOWN·`lookupKnown` · 3 `d22972e`·`e4adde6`·`c5a45f7` WorkBudget·색인화·메모리 상한 · 5 `5a3aa56`~`6249215` 흔한 코드의 틀린 간선 7개 · 2·6 `0fab493` 토크나이저(TS script PARSE_ERROR, 줄 끝 4종, `<script` 경계, TS `!`, `?.5`) · 4 `6f950d5` 캐시(#117: DONE만 LRU 64, 같은 분석 하나만 계산, 동시 2개·10초 대기 뒤 `ANALYSIS_007` 503 — 사용자 결정, 예산 다 쓴 결과는 캐시 안 함, 삭제 이벤트로 비움) · 7 follow-up #121
- ✅ verify all 통과, push, PR 설명 갱신(Closes #117, 공통: ErrorCode·application.yml·conventions → @JoonseobKim), `gh pr ready 118`, Codex 재리뷰 요청
- ✅ Codex 10~12차 반영: 10차 P1 `ea80c31` context-path 복사, P2 `c03dd31` 템플릿 안 선언 UNKNOWN · 11차 `6b60a84` 기본값 config 매개변수(템플릿 안 동적 import는 #121) · 12차 `5e59617` config 매개변수 속성 변경·템플릿 안 인터셉터·require 가림·정규식 미결정 AMBIGUOUS(조건만 다른 컨트롤러는 #122). 구조 테스트 169개
- **11-25 사용자 결정: 13차부터 P2는 follow-up 이슈로 넘기고 `--resolve`, P1만 고침** (결정로그)
- ✅ Codex 13차 P2 3건 → #123(인스턴스 기본 method)·#124(템플릿 안 config 변경)·#125(`#` 비공개 멤버), 스레드 답글, `--resolve` → COMPLETE. PR 설명 후속 작업 갱신. `8909d8b` spec DONE

## 바로 다음 할 일

1. **사람**: GitHub에서 PR #118 머지 (CI 초록·Codex COMPLETE 확인됨, wip 커밋 없음)
2. 머지 뒤 `git switch develop && git pull`, PR 본문 `Closes #117`이 닫혔는지 확인 (안 닫혔으면 close-issues 워크플로 실패)
3. 김준섭 2차 방향 초안에 답하기: SSAFY LLM 키 여부, 시연 혼합 방식 #112, 디자인 일정 12/04~12/21, WBS 4.3 범위 확대 (작업로그 11-25 08:08)
4. 그다음: 내 영역 이슈 #104·#105(F-25 집계) 또는 F-21 이관 이슈(#121~#125)

## 막힌 점 / 결정 필요

- 사람: 노션 팀원 초대

## 주의

- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시) · 하네스 동결(11-23): 버그·작업 막는 문제만 고친다
- 이 PC의 Git Bash heredoc에서 python `'''` 문자열은 백슬래시가 풀리거나 파싱이 깨진다 → Java 편집은 Edit 도구나 Write로 만든 스크립트 파일로
- 간선은 (from, to)마다 첫 줄만 남는다 → 테스트에서 "이 줄은 간선이 아니다"를 볼 때는 다른 API로 구분한다
- 내 영역 이슈 #104·#105(F-25 집계)는 F-21 뒤 기능 사이에 처리
