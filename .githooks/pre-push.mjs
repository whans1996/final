#!/usr/bin/env node
// git pre-push hook (사람과 Claude 모두에게 적용, H-1)
// 커밋 hook은 포맷·린트·관련 테스트만 하므로, push 전에 바뀐 영역의 전체 검증(verify be|fe|harness)을 돌린다.
//  - tip 커밋이 wip면 건너뛴다 (교대 push, handoff.mjs)
//  - develop을 합쳐 들어온 상대 변경은 빼고, 이번 push에서 develop과 달라진 파일만 본다
//  - 같은 커밋을 깨끗한 작업 트리에서 이미 통과한 영역은 다시 돌리지 않는다 (.harness/verified.json, verify.mjs가 남김)
//  - 브랜치 삭제 push는 검사하지 않는다
// stdin: <local ref> <local sha> <remote ref> <remote sha> (줄마다)
import { spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { ROOT, git } from '../scripts/lib/common.mjs';
import { prePushPlan } from '../scripts/lib/related.mjs';

const ZERO = /^0+$/;
const updates = readFileSync(0, 'utf8')
  .split('\n')
  .map((l) => l.trim().split(/\s+/))
  .filter((p) => p.length === 4 && !ZERO.test(p[1]));
if (!updates.length) process.exit(0);

const develop = ['origin/develop', 'develop'].find((b) => git(['rev-parse', '--verify', '-q', b]).ok);
let verified = {};
try {
  verified = JSON.parse(readFileSync(join(ROOT, '.harness', 'verified.json'), 'utf8'));
} catch {
  verified = {};
}

const lines = (args) => git(args).out.split('\n').filter(Boolean);
const targets = new Set();
const notes = [];
for (const [, sha, , remoteSha] of updates) {
  // 새 브랜치면 develop에서 갈라진 지점부터, 원격 sha가 로컬에 없으면(다른 PC push 등) develop 기준으로 본다
  const known = !ZERO.test(remoteSha) && git(['cat-file', '-e', `${remoteSha}^{commit}`]).ok;
  const base = known ? remoteSha : develop ? git(['merge-base', develop, sha]).out : '';
  const pushed = base ? lines(['diff', '--name-only', base, sha]) : lines(['show', '--name-only', '--format=', sha]);
  const own = develop ? new Set(lines(['diff', '--name-only', develop, sha])) : null;
  const files = own ? pushed.filter((f) => own.has(f)) : pushed;
  const plan = prePushPlan({ tipSubject: git(['log', '-1', '--format=%s', sha]).out, files, verified, sha });
  if (plan.skip === 'wip') notes.push(`⏭  ${sha.slice(0, 7)}: wip 커밋이라 검증을 건너뜁니다 (PR 전에 verify를 돌리세요)`);
  if (plan.cached.length) notes.push(`✅ ${sha.slice(0, 7)}: ${plan.cached.join(' ')}는 이 커밋에서 이미 통과했습니다`);
  plan.targets.forEach((t) => targets.add(t));
}
if (notes.length) console.log(notes.join('\n'));
if (!targets.size) process.exit(0);

console.log(`🧪 pre-push 전체 검증: verify ${[...targets].join(' ')} 실행 중… (wip push는 건너뜀)`);
const r = spawnSync(process.execPath, [join(ROOT, 'scripts', 'verify.mjs'), ...targets], { cwd: ROOT, stdio: 'inherit', windowsHide: true });
if (r.status !== 0) {
  console.error(
    [
      '',
      '❌ push가 거부되었습니다: 전체 검증에 실패했습니다.',
      '   테스트를 고쳐 커밋한 뒤 다시 push하세요. 교대 중이라 지금 올려야 하면 node scripts/handoff.mjs (wip 커밋 후 push).',
      '',
    ].join('\n'),
  );
  process.exit(1);
}
process.exit(0);
