#!/usr/bin/env node
// git commit-msg hook (사람과 Claude 모두에게 적용). 메시지 판정은 scripts/lib/commit-msg.mjs (테스트 있음)
//  1) main·develop 직접 커밋 금지 (첫 커밋, merge 진행 중은 예외. 제목이 "Merge "여도 막는다, #136)
//  2) 형식: 타입(범위): 요약   — docs/conventions.md 6절 (git 자동 메시지는 통과, "Merge …"는 merge 진행 중만)
//  3) 스테이징된 추가 줄에 비밀값(토큰·웹훅·GMS 키 등)이 있으면 거부 (wip 포함, merge는 충돌 해결 줄만, H-2)
//  4) wip가 아니면 스테이징된 영역(backend/frontend/하네스)의 빠른 검사(verify --staged)를 돌려 실패 시 거부
import { spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { ROOT, currentBranch, hasCommits, isMerging } from '../scripts/lib/common.mjs';
import { commitVerdict } from '../scripts/lib/commit-msg.mjs';
import { scanDiffForSecrets, scanMergeForSecrets } from '../scripts/lib/protect.mjs';

const msgFile = process.argv[2];
const raw = readFileSync(msgFile, 'utf8');
const subject = raw.split(/\r?\n/).find((l) => l.trim() && !l.startsWith('#')) || '';

const fail = (lines) => {
  console.error(['', '❌ 커밋이 거부되었습니다.', ...lines.map((l) => `   ${l}`), ''].join('\n'));
  process.exit(1);
};

const verdict = commitVerdict({ subject, branch: currentBranch(), hasCommits: hasCommits(), merging: isMerging() });
if (verdict.fail) fail(verdict.fail);

// 3) 비밀값: 값은 출력하지 않고 파일과 종류만 알린다. merge 커밋은 diff가 커서 버퍼를 넉넉히 준다
const stagedDiff = (...base) => {
  const d = spawnSync('git', ['-c', 'core.quotepath=false', 'diff', '--cached', '--no-color', '--no-ext-diff', '-U0', ...base], {
    cwd: ROOT,
    encoding: 'utf8',
    maxBuffer: 256 * 1024 * 1024,
    windowsHide: true,
  });
  if (d.status !== 0) fail([`스테이징된 변경을 읽지 못해 비밀값 검사를 할 수 없습니다: ${(d.stderr || d.error?.message || '').trim()}`]);
  return d.stdout;
};
// merge 커밋은 충돌 해결로 새로 쓴 줄만 본다: develop에서 들어온 줄은 원래 커밋의 검사·push protection이 맡는다
const secrets = isMerging() ? scanMergeForSecrets(stagedDiff('HEAD'), stagedDiff('MERGE_HEAD')) : scanDiffForSecrets(stagedDiff());
if (secrets.length) {
  fail([
    '비밀값으로 보이는 문자열이 스테이징되어 있습니다:',
    ...secrets.map((s) => `  - ${s.file} (${s.label})`),
    '키는 환경변수로 참조하고 예시는 .env.example에 이름만 둡니다. 테스트용 가짜 값은 문자열을 나눠 이어 붙이세요.',
  ]);
}

if (!verdict.verify) process.exit(0);

// 4) 빠른 검사: 스테이징된 영역의 포맷·린트·관련 테스트만 (H-1). 빌드·전체 테스트는 pre-push hook과 CI가 한다
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
