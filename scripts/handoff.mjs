#!/usr/bin/env node
// AI 없이 실행하는 인수인계 스크립트. 토큰이 떨어져 Claude가 멈췄을 때 사람이 실행한다.
//   1) docs/작업로그.md 끝에 자동 항목 추가 (최근 커밋, 미커밋 변경, NOW.md의 다음 할 일·막힌 점)
//   2) 모든 변경을 wip(F-xx) 커밋 (wip는 테스트 게이트 예외)
//   3) 원격에 push
//
// 사용법: node scripts/handoff.mjs [-m "메모"] [--no-push]
import { appendFileSync, readFileSync } from 'node:fs';
import { userInfo } from 'node:os';
import {
  NOW_FILE,
  PROTECTED_BRANCHES,
  WORKLOG_FILE,
  currentBranch,
  featureIdFrom,
  git,
  hasCommits,
  isGitRepo,
  mdSection,
  nowStamp,
  readText,
} from './lib/common.mjs';

const args = process.argv.slice(2);
const noPush = args.includes('--no-push');
const mIdx = args.indexOf('-m');
const memo = mIdx >= 0 ? args[mIdx + 1] || '' : '';

function die(msg) {
  console.error(`❌ ${msg}`);
  process.exit(1);
}

if (!isGitRepo()) die('git 저장소가 아닙니다.');
const branch = currentBranch();
if (!branch) die('detached HEAD 상태입니다. 작업 브랜치로 switch 한 뒤 실행하세요.');
if (PROTECTED_BRANCHES.includes(branch) && hasCommits()) {
  die(`보호 브랜치(${branch})에서는 커밋할 수 없습니다. "git switch -c feature/F-xx-slug"로 옮긴 뒤 다시 실행하세요.`);
}

const fid = featureIdFrom(branch) || 'common';
const author = git(['config', 'user.name']).out || userInfo().username;

// 최근 커밋: develop 이후 커밋, 없으면 최근 8개
const hasDevelop = git(['rev-parse', '--verify', '-q', 'develop']).ok;
const logArgs = hasDevelop && branch !== 'develop' ? ['log', '--oneline', '-15', 'develop..HEAD'] : ['log', '--oneline', '-8'];
const commits = hasCommits() ? git(logArgs).out.split('\n').filter(Boolean) : [];

const changed = git(['status', '--porcelain', '--untracked-files=all']).out.split('\n').filter(Boolean);

const nowMd = readText(NOW_FILE) || '';
const next = mdSection(nowMd, '바로 다음 할 일') || '(NOW.md에 없음)';
const blocked = mdSection(nowMd, '막힌 점') || '없음';

const indent = (text) =>
  text
    .split(/\r?\n/)
    .filter((l) => l.trim())
    .map((l) => `  ${l.trim()}`)
    .join('\n');

const entry = [
  '',
  `## ${nowStamp()} · ${author} · ${branch} (자동 handoff)`,
  `- 한 일 (이 브랜치 커밋):`,
  commits.length ? commits.map((c) => `  - ${c}`).join('\n') : '  - (커밋 없음)',
  `- 미커밋 변경 ${changed.length}개 → 이 handoff의 wip 커밋에 포함`,
  changed.length ? changed.slice(0, 15).map((c) => `  - \`${c.trim()}\``).join('\n') + (changed.length > 15 ? `\n  - … 외 ${changed.length - 15}개` : '') : '',
  `- 다음 할 일 (NOW.md):`,
  indent(next),
  `- 막힌 점: ${blocked
    .split(/\r?\n/)
    .map((l) => l.replace(/^\s*(?:[-*]|\d+\.)\s*/, '').trim())
    .filter(Boolean)
    .join(' / ')}`,
  memo ? `- 메모: ${memo}` : '',
  `- AI 활용: (있으면 이어받는 사람이 채움)`,
  `- 주의: 자동 기록이다. NOW.md가 오래되었을 수 있으니 이어받는 사람은 git diff로 마지막 변경을 확인할 것`,
  '',
]
  .filter((l) => l !== '')
  .join('\n');

const prev = readFileSync(WORKLOG_FILE, 'utf8');
appendFileSync(WORKLOG_FILE, (prev.endsWith('\n') ? '' : '\n') + entry + '\n', 'utf8');
console.log(`📝 작업로그에 항목을 추가했습니다.`);

// 스테이징 (.env 등은 .gitignore로 제외됨). 혹시 스테이징된 비밀 파일이 있으면 중단
git(['add', '-A']);
const staged = git(['diff', '--cached', '--name-only']).out.split('\n').filter(Boolean);
const secret = staged.find((f) => /(^|\/)\.env(\.(?!example$)[^/]*)?$|\.(pem|key)$/.test(f));
if (secret) {
  git(['reset', '-q', '--', secret]);
  die(`비밀 파일로 보이는 ${secret} 이(가) 스테이징되어 중단했습니다. .gitignore를 확인하세요.`);
}

const msg = `wip(${fid}): 교대 체크포인트 (handoff.mjs)${memo ? ` - ${memo}` : ''}`;
const c = git(['commit', '-m', msg]);
if (!c.ok) die(`커밋 실패:\n${c.out}\n${c.err}`);
console.log(`✅ 커밋: ${msg}`);

if (noPush) {
  console.log('⏭  --no-push: push를 건너뜁니다.');
  process.exit(0);
}
const remote = git(['remote']).out.split('\n').filter(Boolean);
if (!remote.length) {
  console.warn('⚠️  원격 저장소가 없어 push하지 못했습니다. git remote add origin <URL> 후 다시 push 하세요.');
  process.exit(0);
}
const p = git(['push', '-u', remote.includes('origin') ? 'origin' : remote[0], branch], { timeout: 60_000 });
if (!p.ok) die(`push 실패 (커밋은 로컬에 남아 있음):\n${p.err}`);
console.log(`🚀 push 완료: ${branch}`);
console.log(`\n상대방은 "git pull" 후 Claude Code에서 /pickup 으로 이어받으면 됩니다.`);
