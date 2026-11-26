# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> 머리 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-26 10:08 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/common-plan2-1126

## 현재 기능

**2차 점검·진행 순서 (11-26)** — 읽기 전용 점검 5갈래 + GMS 실호출 결과를 `docs/점검/2026-11-26-2차점검.md`에 정리, 결정로그 11-26 7행. docs PR(base develop)

## 진행

- ✅ develop·main 브랜치 보호 적용 (PR 필수·CI 5개 필수·관리자 포함·force push 금지)
- ✅ 결정: GMS 키 재발급(만료 제약 아님), 개발 끝까지 public, F-33 T2-5 유지, A가 자기 FE 맡음(김준섭 합의 필요), 카드 200장+·카탈로그 개편, Colab 안 함
- ✅ GMS 실호출(DS-02-01 dev, 개요·파일): senior≈junior, XML 매퍼 오판 → 점검 문서 5절

## 바로 다음 할 일

1. 이 docs PR: CI 초록이면 GitHub에서 머지 (docs만이라 Codex 생략). 본문에서 @JoonseobKim에게 역할 조정·DS 고정 11/28·F-29 공통 PR 12/02 합의 요청
2. `/clear` 후 새 세션: 점검 문서 4절 A 항목을 follow-up 이슈로 등록(`gh issue create`, area:A) + 기획서 6절·WBS 6절·간트에 L-1·K-1~3·E-1·LP-1·S-1 반영 (docs PR)
3. D-1 문서 정정 PR (점검 문서 3절 목록) → E-1 `PromptEvalRunner.java:98` stage 제한 풀기 + 채점기 (`/spec` 필요 여부 판단)
4. dh-home V-2 블라인드 점검: `git fsck --unreachable --no-reflogs` → 작성자 JoonseobKim·11-25 커밋이 있으면 `git reflog expire --expire-unreachable=now --all && git gc --prune=now`

## 막힌 점 / 결정 필요

- 김준섭 합의: 역할 조정(A가 F-28·L-1 FE), DS-03·04 고정 11/28로 당기기, F-29 공통 PR 12/02
- 사람: 노션 팀원 초대, F-05b liveTest, 제출 zip 이름, dh-ssafy `.env` GMS 키 (현재 동작함)

## 주의

- 다음 마이그레이션은 V14 (`node scripts/next-migration.mjs`로 확인)
- 브랜치 보호 관리자 포함: develop·main 직접 push 불가, 급할 땐 GitHub Settings에서 잠시 끈다
- node 스크립트로 Java 편집 시 백슬래시가 깨짐 → Edit 도구 사용 · 이 PC에 python 없음
