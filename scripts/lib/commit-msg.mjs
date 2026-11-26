// commit-msg hook의 메시지 판정 (테스트 있음). 실행·검사 호출은 .githooks/commit-msg.mjs가 한다
import { PROTECTED_BRANCHES } from './common.mjs';

// git이 자동으로 만드는 메시지 (형식 검사·빠른 검사 생략). "Merge …"는 merge 진행 중일 때만 (손으로 쓴 제목으로 빠른 검사를 건너뛰지 않게)
const AUTO = /^(Revert "|fixup! |squash! |amend! )/;
// 범위: 기능 ID F-xx, 로드맵 항목 ID(docs/로드맵.md, roadmap.mjs ROADMAP_ID와 같은 형식: K-3a·P-1a·HP-3·F-26b2), common, harness
const SCOPE = '(F-\\d{2}[a-z]?|[A-Z]{1,4}-\\d{1,2}[a-z0-9]{0,2}|common|harness)';
const SCOPED = new RegExp(`^(feat|fix|refactor|test|wip)\\(${SCOPE}\\): \\S.*$`);
const OPTIONAL = new RegExp(`^(docs|chore)(\\(${SCOPE}\\))?: \\S.*$`);

/**
 * { fail: 거부 사유 줄들 | null, verify: 빠른 검사(verify --staged)를 돌릴지 }
 * 보호 브랜치 검사가 자동 메시지 예외보다 먼저다 (#136): develop에서 `git commit -m "Merge x"`도 막는다. merge 진행 중과 첫 커밋만 예외
 */
export function commitVerdict({ subject, branch, hasCommits, merging }) {
  if (branch && PROTECTED_BRANCHES.includes(branch) && hasCommits && !merging) {
    return {
      fail: [`보호 브랜치(${branch})에는 직접 커밋할 수 없습니다.`, `git switch -c feature/F-xx-slug 로 브랜치를 만든 뒤 커밋하고, develop에는 PR로 합치세요.`],
      verify: false,
    };
  }
  if (AUTO.test(subject) || (merging && subject.startsWith('Merge '))) return { fail: null, verify: false };
  if (!SCOPED.test(subject) && !OPTIONAL.test(subject)) {
    return {
      fail: [
        `커밋 메시지 형식이 맞지 않습니다: "${subject}"`,
        `형식: 타입(범위): 요약`,
        `  타입: feat fix refactor test wip (범위 필수) / docs chore (범위 생략 가능)`,
        `  범위: F-xx(분할 spec은 F-05a) | 로드맵 항목 ID(K-3a, P-1a, HP-3) | common | harness`,
        `예: feat(F-05): 분석 요청 API 추가 / wip(F-05): 중간 저장 / docs: README 수정`,
      ],
      verify: false,
    };
  }
  return { fail: null, verify: subject.split(/[(:]/)[0] !== 'wip' }; // 교대용 wip 커밋은 테스트 게이트 예외
}
