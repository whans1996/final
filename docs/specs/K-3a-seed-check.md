# K-3a 시드 카드 원본 형식 + 검수 스크립트 ①형식 ②예제 실행

| 항목 | 값 |
|---|---|
| 상태 | APPROVED <!-- DRAFT → APPROVED(사람 승인) → DONE(PR 머지) --> |
| 기능 ID | K-3a (로드맵 트랙 3 카드, 11/26~11/28, 다음 K-3b ③출처 대조) |
| 우선순위 | T1 (기획서 7.4b, K-3 200장의 첫 단계) |
| 영역 오너 | A 김동환 (AI·분석·학습 루프. 11-26 합의로 K-3 기간 `R__seed_concepts.sql`도 A) |
| 관련 필수 요구사항 | 산출물 09 데이터셋(DS-05 시드 카드 검증), NF-04 |
| 의존 spec | F-13(관리자 카드 관리, 시드 upsert AC12), F-09(카테고리 필터) |
| 관련 이슈 | 없음 (#204 카테고리 표기는 K-1) |
| 승인 | 김동환 2026-11-27 |

## 1. 목적

K-3은 시드 카드를 85장에서 200장 이상으로 늘리고, 수준별 본문 3개(K-1)를 Claude Code가 쓴다 (결정로그 11-26 "카드·TS·React", "Max 처리량 반영 재검토"). 사람이 사실을 판단하지 않아도 되게 기계 검사로 걸러야 하는데, 지금 시드는 2549줄짜리 SQL 하나라서 카드별로 편집·diff·플래그를 다루기 어렵다. 예제 코드에는 언어 표시가 없어 검사할 수도 없다.
이 spec은 ① 시드 원본을 **카드별 Markdown + SQL 생성기**로 바꾸고 ② 검수 4단계 중 **①형식 ②예제 실행**을 스크립트·테스트로 만든다. 그리고 기존 85장을 이 형식으로 옮겨 오류 0으로 맞춘다. ③출처 대조는 K-3b, 새 카드는 K-3c·K-3d가 맡는다.

## 2. 수용 기준 (Acceptance Criteria)

> 테스트 이름에 AC 번호를 넣는다. 심각도는 **오류**(CI 실패)와 **경고**(보고서에만, K-3c 플래그 목록의 재료) 두 가지다.

| ID | Given | When | Then |
|---|---|---|---|
| AC1 | `data/seed/concepts/<category>/<slug>.md` 카드 파일들이 있을 때 | `node scripts/seed-build.mjs`를 실행하면 | `R__seed_concepts.sql`이 생성된다 (머리 주석 "생성 파일, 직접 고치지 말 것"과 F-13 AC12 upsert 문 유지). 같은 입력이면 바이트까지 같은 출력이 나온다. 작은따옴표와 **역슬래시**도 이스케이프해서, `It's`·`\d+`가 든 카드를 적용하면 DB 값이 원본과 같다 |
| AC2 | develop의 기존 `R__seed_concepts.sql` 85장이 있을 때 | 카드 파일로 변환한 뒤 다시 생성해 적용하면 | 85장 전부 name·category·difficulty·summary·body·example_code·reference_url이 변환 전과 같다. 단 ②에서 오류로 고친 카드는 제외하고, PR 본문 표에 이름·사유·바뀐 칸을 적는다. 기존 `SeedConceptsTest`가 통과한다 |
| AC3 | 카드 파일을 고치고 SQL을 다시 생성하지 않았거나 SQL을 손으로 고쳤을 때 | `node scripts/seed-build.mjs --check`를 실행하면 | 종료 코드 1과 "생성본이 원본과 다름" 메시지가 나오고 CI 하네스 잡이 실패한다 |
| AC4 | 카드 파일에 형식 문제가 있을 때 | ①형식 검사를 하면 | 다음은 **오류**다: 필수 frontmatter(`name`·`category`·`difficulty`·`reference`)나 섹션(`summary`·`body`·`example`)이 없음 / 모르는 키·섹션이 있음 / `category`가 `ConceptCategories.ALL` 밖이거나 폴더 이름과 다름 / 난이도가 3단계 밖 / name 100자 초과·summary 500자 초과(DB 한도) / 본문·예제가 비어 있음 / 예제 코드 블록에 언어 태그가 없거나 허용 목록 밖 / reference가 `https://`가 아니거나 `data/datasets.json` DS-01 출처의 호스트가 아님. 각 오류는 파일 경로·항목·이유로 나온다 |
| AC5 | 두 카드 이름이 대소문자·악센트만 다를 때 (MySQL `utf8mb4_0900_ai_ci`에서 같은 값) | ①형식 검사를 하면 | 두 파일 모두 **오류**다 (한 INSERT 안에서 앞 행이 조용히 덮어써짐). 공백·기호만 다른 이름은 **경고**다 |
| AC6 | 카드가 권장 범위를 벗어나거나 요약이 비슷할 때 | ①형식 검사를 하면 | 다음은 **경고**다: name 60자 초과 / summary 40~200자 밖 / body 150~2000자 밖 / 예제 60줄 초과 / 요약 쌍의 문자 3-gram Jaccard가 0.5 이상(두 카드와 점수를 보고) |
| AC7 | 카드에 `java` 블록이 있을 때 | ②Java 검사(BE 테스트, JDK 21, BE 테스트 클래스패스에 Spring·MyBatis 포함)를 하면 | 같은 카드의 java 블록을 함께 컴파일한다. 조각은 ⓐ그대로 컴파일 단위 → ⓑ클래스 본문 → ⓒ메서드 본문 순으로 감싸 보고, 문법 오류가 없는 첫 형태를 쓴다. 셋 다 문법 오류면 **오류**다. `java.util.*`·`java.util.function.*`·`java.util.stream.*`·`java.time.*`·`java.util.concurrent.*`는 자동으로 import한다. 맥락이 없어서 나는 진단(심볼·패키지 없음: `compiler.err.cant.resolve*`·`doesnt.exist`)은 **경고**, 그 밖의 진단(타입 불일치, 잘못된 override, 문법 등)은 **오류**다 |
| AC8 | 카드에 `sql`·`xml`·`yaml` 블록이 있을 때 | ②BE 검사를 하면 | `sql`은 Testcontainers MySQL에서 카드마다 빈 스키마를 만들고 블록 순서대로 실행한다. 문법 오류(1064) 등은 **오류**, 테이블·컬럼 없음(1146·1054)은 **경고**다. `xml`은 well-formed 검사(MyBatis 매퍼 조각은 `<mapper>`로 감쌈), `yaml`은 SnakeYAML 파싱으로, 실패하면 **오류**다 |
| AC9 | 카드에 `js`·`jsx`·`ts`·`tsx`·`vue`·`json` 블록이 있을 때 | ②FE 검사(Node, `frontend/node_modules`의 `typescript`·`@vue/compiler-sfc`)를 하면 | js·jsx는 문법을, ts·tsx는 문법과 타입을 검사한다(이름·모듈을 못 찾는 진단 TS2304·TS2307·TS2552·TS2582·TS7016은 **경고**). vue는 SFC 파싱·템플릿·스크립트 컴파일 오류가 **오류**이고, `<script setup lang="ts">`면 ts 규칙도 적용한다. json은 `JSON.parse` 실패가 **오류**다. `http`·`text`·`bash`·`properties`·`gradle`·`kotlin`은 "검사 안 함"으로 보고한다 |
| AC10 | 검사를 돌릴 때 | `node scripts/seed-check.mjs [--be] [--card <slug>]`를 실행하면 | `.harness/seed-check/report.md`·`report.json`에 카드 파일·단계(①·②)·심각도·메시지와 합계(카드 수, 오류, 경고, 검사 안 한 블록)가 남는다. 오류가 있으면 종료 코드 1, 경고만 있으면 0이다. `--be`는 Gradle로 AC7·AC8 테스트를 돌려 `backend/build/seed-check/result.json`을 합친다(Docker 필요, 없으면 "BE 검사 건너뜀"을 알리고 종료 코드는 바꾸지 않음) |
| AC11 | `data/seed/**`·`data/datasets.json`이 바뀐 PR에서 | CI가 돌면 | harness 잡(AC3), backend 잡(AC7·AC8, `SeedExampleTest`), frontend 잡(AC4~AC6·AC9)이 각각 돌고, 오류가 하나라도 있으면 그 잡이 실패한다. 기존 85장을 옮긴 이 PR의 상태는 **오류 0**이다 |

## 3. API 계약

없음 (API·openapi.json 변경 없음).

## 4. DB 변경

- Flyway: 새 V 파일 없음. `R__seed_concepts.sql`은 생성본으로 바뀐다. 체크섬이 바뀌어 다시 적용되지만 name 기준 upsert라 id·찜·리뷰·관리자 숨김은 그대로다 (F-13 AC12).
- 사용 테이블: `concepts` (변경 없음)

### 카드 파일 형식

~~~~markdown
---
name: MyBatis 동적 SQL
category: mybatis
difficulty: JUNIOR
reference: https://mybatis.org/mybatis-3/ko/dynamic-sql.html
---

## summary

한두 문장 요약.

## body

본문 Markdown (지금 `body` 칸과 같은 규칙: 제목·목록·코드·굵게).

## example

```java
List<Post> search(@Param("keyword") String keyword);
```

```xml
<select id="search" resultType="Post"> ... </select>
```
~~~~

- 경로 `data/seed/concepts/<category>/<slug>.md`. slug는 영문 소문자 kebab. 폴더 = category.
- `example_code`는 블록 내용을 순서대로 빈 줄 하나(`\n\n`)로 이어 붙인 값이다. 언어 태그는 검사에만 쓰고 DB에 저장하지 않는다 (FE·API 그대로).
- `status`·`source`는 파일에 쓰지 않는다. 생성기가 `PUBLISHED`·`SEED`로 넣는다.
- 허용 언어 태그. 검사함: `java` `sql` `xml` `yaml` `js` `jsx` `ts` `tsx` `vue` `json`. 검사 안 함: `http` `text` `bash` `properties` `gradle` `kotlin`.
- 생성 순서: 카테고리 순(java·spring·web·security·mybatis·vue, 그 밖은 이름순) → 파일 이름순. 새 DB의 id 순서만 정하고, Mock fixture는 이름으로 연결해서 영향이 없다 (#49).

## 5. 프론트엔드

화면 변경 없음. `frontend/package.json` devDependencies에 `typescript`를 명시한다 (지금은 간접 의존성이라 버전이 고정되지 않음).

## 6. LLM (AI 기능만)

해당 없음. 이 단계는 LLM을 호출하지 않는다 (③출처 대조의 GMS 판정은 K-3b). 기존 85장 변환·수정은 Claude Code가 하지만, 런타임 LLM이나 크레딧은 쓰지 않는다.

## 7. 테스트 계획

| AC | 테스트 위치 | 종류 |
|---|---|---|
| AC1 | `scripts/test/seed-build.test.mjs#AC1_*` (결정성·이스케이프) + `SeedConceptsTest#AC1_역슬래시와_작은따옴표가_원본_그대로_들어간다` | node:test + BE 통합 |
| AC2 | 구현 중 1회 비교 스크립트(develop 파일 vs 생성본, 행 단위) 결과를 PR 본문에 + 기존 `SeedConceptsTest` 전체 | 일회 검증 + BE 통합 |
| AC3 | `scripts/test/seed-build.test.mjs#AC3_*` + CI harness 잡 `seed-build.mjs --check` 단계 | node:test |
| AC4~AC6 | `scripts/test/seed-format.test.mjs#AC4_*`~`AC6_*` (임시 디렉터리 픽스처) | node:test |
| AC7·AC8 | `SeedExampleTest` (실제 카드 전체, 오류면 실패) + `SeedExampleCheckerTest#AC7_*`·`AC8_*` (문자열 픽스처로 감싸기 3단계·진단 분류·SQL 오류 코드 분류) | BE 단위 + Testcontainers |
| AC9 | `scripts/test/seed-examples.test.mjs#AC9_*` (FE 의존성이 없으면 skip, frontend CI 잡에서 실행) | node:test |
| AC10 | `scripts/test/seed-check.test.mjs#AC10_*` (보고서 형식·종료 코드, `--be` 없이) | node:test |
| AC11 | `.github/workflows/ci.yml` 변경(경로 필터에 `data/seed/**`·`data/datasets.json`, harness·frontend 잡 단계) + PR CI 결과 | CI |

## 8. 범위 밖

- ③출처 대조(reference URL 열림 확인, 페이지 본문으로 문장별 근거 판정, GMS) → K-3b
- 카테고리 12종·수준별 본문 3개·요약 열·카드 스키마 변경 → K-1 (K-1이 카드 파일 형식에 섹션을 추가한다)
- 새 카드 작성(+60장, 200장)과 플래그 목록 → K-3c·K-3d
- DS-01 출처 확대(react.dev·typescriptlang.org·dev.mysql.com 등 새 카테고리 공식 문서 등록) → 새 카테고리 카드를 쓰는 K-3c에서 라이선스 확인 뒤 `datasets.json`에 등록
- 예제를 실제로 실행해 출력 비교, 매퍼 XML 안의 SQL 실행, Spring 컨텍스트 기동
- 관리자 화면(S-1)·이해도 표본(SF-1)
- 사람 이해도 검수(④)

## 9. 결정 필요

- (없음) 원본 형식·검사 강도·CI 판정은 11-27 사용자 선택으로 정했다 (결정로그).
- 알림: `R__seed_concepts.sql`(B 소유)이 생성 파일이 되어 손으로 고칠 수 없게 되고, `frontend/package.json`·`ci.yml`·`SeedConceptsTest`도 공통 파일이다 → PR에서 @JoonseobKim. `data/seed/`는 CODEOWNERS에 A로 추가한다.

## 10. 구현 체크리스트

- [x] 브랜치 `feature/K-3a-seed-check`
- [ ] Flyway 마이그레이션 (없음, R__ 생성본)
- [ ] AC별 실패 테스트 작성 (red 확인)
- [ ] 생성기·①형식 검사 (green)
- [ ] 기존 85장 변환 + AC2 비교
- [ ] ②BE 검사 `SeedExampleChecker`·`SeedExampleTest` (green)
- [ ] ②FE 검사·`seed-check.mjs` 보고서 (green)
- [ ] 85장 오류 0으로 수정 (PR 본문 표)
- [ ] CI 경로 필터·단계, `verify.mjs`가 `data/seed` 변경을 be·fe 영역으로 판정
- [ ] 산출물 09 DS-05(보관 방법·검증 결과) 갱신
- [ ] reviewer 지적 사항 반영
- [ ] PR CI 초록
- [ ] 상태 DONE (PR 머지 후)
