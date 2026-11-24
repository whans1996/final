# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-24 17:40 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/F-21a-structure-graph

## 현재 기능

**F-21a 구조 시각화: 파일 의존 그래프** (P1) spec `docs/specs/F-21a-structure-graph.md` — APPROVED. PR #118 (**draft로 전환**, 구조 정리 중). 11-24 사용자 결정: Codex 지적을 하나씩 막지 않고 전수 점검 결과로 **구조를 바꿔 한 번에** + 캐시(#117) 흡수 → spec **5.3절**이 작업 목록, 결정로그 11-24 행

## 진행

- ✅ Codex 1~9차 지적 반영 (9차 3건 `a94d89e`: 경로 비교 메모리 P1, class 이름 가림, 객체 리터럴 뒤 나눗셈). 재리뷰 9회
- ✅ 전수 점검 4영역 약 40건 → spec 5.3절 1~7로 정리
- ✅ 5.3-3 완료: `d22972e` WorkBudget·파일별 예외 격리, `e4adde6` 제곱 경로 색인화(`JsScopeIndex` 등, `StructureWorkloadTest` 12개), `c5a45f7` 메모리 상한(토큰 파일 50만·합계 200만, unresolved 상한+1 보관, Java AST 파일마다 두 번 파싱). spec 5.3-3 끝에 구현 메모
- ✅ 5.3-1 완료 `d796d18`: `JsDecl.Kind.UNKNOWN`(TS식 매개변수·catch 이름, `with` 본문), `JsModule.lookupKnown`으로 가린 바깥 이름이 HTTP면 `HttpLinker`가 `SHADOWED` → URL_UNKNOWN. 키워드·계산된·`#` 메서드, 구조 분해 catch·대입, for 머리, 다시 대입 래퍼, 최상위 래퍼만 다른 파일, 전개 인자, import axios만 믿음. `StructureAnalyzerTest` "5.3절 1" 8개, 구조 테스트 122개 통과. spec 5.3-1 끝에 구현 메모
- ✅ 5.3-5 완료 (7개): `5a3aa56` 인라인 먼저·호스트 자리 동적, `baa446a` 외부 호스트 NO_MATCHING_API, `8a961a8` 와일드카드·메서드 모름은 한 파일일 때만·Spring 순위, `312d7e5` axios defaults 변형·인터셉터·fetch 바꿔 끼우기, `b2ec249` profiles 맵, `6249215` ApiDetector 불완전 시 리터럴 일치만(나머지 AMBIGUOUS). spec 5.3-5 끝에 구현 메모. 구조 관련 테스트 144개

## 바로 다음 할 일

1. spec 5.3-2·6 토크나이저 (`JsTokenizer`, `JsTokenizerTest`에 실패 테스트 먼저): `.vue` `<script lang="ts|tsx">`는 PARSE_ERROR, `scriptBlocks`의 `SCRIPT_OPEN`을 `<script(?=[s>/])`·선형으로, 속성 따옴표 안 `>`, regex 안 `` 다음 줄바꿈 실패, CR 단독·U+2028·U+2029 줄 끝, 값 뒤 TS `!` 다음 `/`는 나눗셈, `?.` 뒤 숫자는 `?`+`.5` → 5.3-4 캐시(`StructureService`, 선별 파일 불변·soft delete 확인 먼저, `StructureServiceTest` 새로) → 5.3-7 follow-up 이슈(area:A)
2. `node scripts/verify.mjs all` → push → `gh pr ready 118` → `node scripts/codex-review.mjs 118 --request --wait --status` (background)
3. Codex COMPLETE + CI 초록 → `docs(F-21a): 완료 표시 (spec DONE)` → 사람이 GitHub에서 머지

## 막힌 점 / 결정 필요

- 사람: 노션 팀원 초대

## 주의

- 리뷰 지적은 '무시 가능'도 직접 확인한다 (11-20 사용자 지시) · 하네스 동결(11-23): 버그·작업 막는 문제만 고친다
- 이 PC의 Git Bash heredoc에서 python `'''` 문자열은 백슬래시가 풀리거나 파싱이 깨진다 → Java 편집은 Edit 도구나 Write로 만든 스크립트 파일로
- 간선은 (from, to)마다 첫 줄만 남는다 → 테스트에서 "이 줄은 간선이 아니다"를 볼 때는 다른 API로 구분한다
- 내 영역 이슈 #104·#105(F-25 집계)는 F-21 뒤 기능 사이에 처리
- PR 설명 "공통 코드 변경"에 `application.yml`(structure 예산 2줄) 적고 @JoonseobKim 멘션
