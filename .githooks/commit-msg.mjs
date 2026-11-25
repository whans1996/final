#!/usr/bin/env node
// git commit-msg hook (사람과 Claude 모두에게 적용)
//  1) 형식: 타입(범위): 요약   — docs/conventions.md 6절
//  2) main·develop 직접 커밋 금지 (첫 커밋, merge 커밋은 예외)
//  3) wip가 아니면 스테이징된 영역(backend/frontend/하네스)의 빠른 검사(verify --staged)를 돌려 실패 시 거부
import { spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { PROTECTED_BRANCHES, ROOT, currentBranch, hasCommits, isMerging } from '../scripts/lib/common.mjs';

const msgFile = process.argv[2];
const raw = readFileSync(msgFile, 'utf8');
const subject = raw.split(/\r?\n/).find((l) => l.trim() && !l.startsWith('#')) || '';

const fail = (lines) => {
  console.error(['', '❌ 커밋이 거부되었습니다.', ...lines.map((l) => `   ${l}`), ''].join('\n'));
  process.exit(1);
};

// git이 자동으로 만드는 메시지는 통과
if (/^(Merge |Revert "|fixup! |squash! |amend! )/.test(subject)) process.exit(0);

// 2) 보호 브랜치
const branch = currentBranch();
if (branch && PROTECTED_BRANCHES.includes(branch) && hasCommits() && !isMerging()) {
  fail([
    `보호 브랜치(${branch})에는 직접 커밋할 수 없습니다.`,
    `git switch -c feature/F-xx-slug 로 브랜치를 만든 뒤 커밋하고, develop에는 PR로 합치세요.`,
  ]);
}

// 1) 형식
const SCOPE = '(F-\\d{2}[a-z]?|common|harness)';
const scoped = new RegExp(`^(feat|fix|refactor|test|wip)\\(${SCOPE}\\): \\S.*$`);
const optional = new RegExp(`^(docs|chore)(\\(${SCOPE}\\))?: \\S.*$`);
if (!scoped.test(subject) && !optional.test(subject)) {
  fail([
    `커밋 메시지 형식이 맞지 않습니다: "${subject}"`,
    `형식: 타입(범위): 요약`,
    `  타입: feat fix refactor test wip (범위 필수) / docs chore (범위 생략 가능)`,
    `  범위: F-xx(분할 spec은 F-05a) | common | harness`,
    `예: feat(F-05): 분석 요청 API 추가 / wip(F-05): 중간 저장 / docs: README 수정`,
  ]);
}

const type = subject.split(/[(:]/)[0];
if (type === 'wip') process.exit(0); // 교대용 커밋은 테스트 게이트 예외

// 3) 빠른 검사: 스테이징된 영역의 포맷·린트·관련 테스트만 (H-1). 빌드·전체 테스트는 pre-push hook과 CI가 한다
const r = spawnSync(process.execPath, [join(ROOT, 'scripts', 'verify.mjs'), '--staged'], {
  cwd: ROOT,
  stdio: 'inherit',
  windowsHide: true,
});
if (r.status !== 0) {
  fail([
    `검증에 실패했습니다. 테스트를 고친 뒤 다시 커밋하세요.`,
    `교대 직전이라 지금 저장만 해야 한다면 wip(${subject.match(/\(([^)]+)\)/)?.[1] || 'F-xx'}): ... 로 커밋하세요.`,
  ]);
}
process.exit(0);
