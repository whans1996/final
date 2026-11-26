# NOW — 김동환 현재 작업 스냅샷

> 김동환(A 영역: AI·분석·학습 루프) 전용. 덮어쓰는 파일이다. 작업 단위(AC 통과, 파일 묶음 완성, 방향 결정, 막힘)가 끝날 때마다 갱신한다. 30줄 이내.
> "갱신:" 줄은 `node scripts/stamp.mjs --now` 출력으로 쓴다. "바로 다음 할 일"은 다른 PC에서 맥락 없이 바로 시작할 수 있을 만큼 구체적으로 쓴다 (파일, 메서드, 테스트 이름).
> 상대 파일(`NOW-js.md`)은 읽기만 한다.
갱신: 2026-11-26 09:13 · 작업자: whans1996 · PC: dh-ssafy · 브랜치: feature/common-progress-1126

## 현재 기능

**/progress W2 (11-26)** — 진행현황·WBS·간트 갱신 PR(base develop, docs만). 노션 대시보드 "진행 현황" 섹션도 갱신함. 직전 F-27 PR #161은 11/26 머지됨

## 진행

- ✅ 2차 T0 중 A 3건 완료: H-1(PR #138·#141), F-26a(PR #157·#160), F-27(PR #161). spec 30개 DONE, 마이그레이션 V13
- ✅ 진행현황: T1 머지는 V-2 기준선(12/04) 뒤로 둔다는 판단 기록. WBS 1.8·6.1~6.3 ✅, 4.1 🟡, 간트 실적 막대 4개 추가
- ✅ PR #161 리뷰 제안은 follow-up #163(F-27b)

## 바로 다음 할 일

1. 진행현황 PR: CI 초록이면 GitHub에서 머지 (docs만이라 Codex 루프 생략, conventions 7절) → `git switch develop && git pull` → 로컬 `feature/F-26a-index-selection`·`feature/F-27-gms-provider` `git branch -d`
2. V-2 블라인드 점검(김준섭 요청): dh-ssafy에 ds03 브랜치 고아 커밋 7개(11-25 16:53~17:44, NOW-js·작업로그) 남음, 내용 안 열었음. 사람 승인 시 `git reflog expire --expire-unreachable=now --all && git gc --prune=now`. dh-home도 같은 점검(`git fsck --unreachable --no-reflogs` → 작성자 JoonseobKim·11-25 커밋). 김준섭에게 답장
3. `/clear` → `/spec F-26b` (커버리지 개편, #121·#156 흡수, 12/04 기준선 전엔 머지 안 함). 짬 나면 공통 하네스 이슈 #130·#135(보안 성격) 먼저
4. 11/27 Figma 세션 → 11/29~ V-1 MVP 감사

## 막힌 점 / 결정 필요

- **GMS 키 2026-12-09 18:00 만료** (발표 12-24 전) → 재발급·연장 사람 확인 필요
- V-2 기준선 앞당기기: DS-03·04를 12/03 전에 고정할 수 있는지 김준섭과 합의
- 사람: 노션 팀원 초대, F-05b liveTest, 제출 zip 이름, dh-ssafy `.env`에 GMS 키

## 주의

- 다음 마이그레이션은 V14 (김준섭도 V14 이상 예정이라 `node scripts/next-migration.mjs`로 확인)
- ui-verifier가 로컬 DB에 테스트 계정 `f27shot@example.com`을 만들어 둠 (로컬 전용)
- node 스크립트로 Java 편집 시 백슬래시가 깨짐 → Edit 도구 사용 · 이 PC에 python 없음
