#!/usr/bin/env node
// AI 없이 실행하는 인수인계 스크립트. 토큰이 떨어져 Claude가 멈췄을 때 사람이 실행한다.
//   1) docs/작업로그.md 끝에 자동 항목 추가 (이 브랜치 커밋, 미커밋 변경, 내 NOW 파일의 다음 할 일·막힌 점)
//   2) 내 NOW 파일(docs/handoff/NOW-<dh|js>.md)의 "갱신:" 줄만 지금 값으로 바꾼다 (본문은 그대로. 다음 세션의 PC 전환 판정용)
//   3) 변경을 wip(F-xx) 커밋 (wip는 테스트 게이트 예외). 추적 중인 파일과 작업 폴더의 새 파일만 넣는다
//   4) 원격에 push
//
// 사용법: node scripts/handoff.mjs [-m "메모"] [--no-push]
import { appendFileSync, readFileSync, writeFileSync } from 'node:fs';
import { userInfo } from 'node:os';
import {
  PROTECTED_BRANCHES,
  WORKLOG_FILE,
  currentBranch,
  featureIdFrom,
  git,
  hasCommits,
  isGitRepo,
  mdSection,
  nowFileOf,
  nowRelPath,
  nowStamp,
  ownerKey,
  readText,
  recordStamp,
} from './lib/common.mjs';
import { currentPc } from './lib/env-check.mjs';
import { touchNowHeader, undoMsysPathConv } from './lib/records.mjs';

const args = process.argv.slice(2);
const noPush = args.includes('--no-push');
const mIdx = args.indexOf('-m');
// Git Bash가 '/명령'으로 시작하는 메모를 Git 설치 경로로 바꾼 것을 되돌린다 (#231)
const memo = mIdx >= 0 ? undoMsysPathConv(args[mIdx + 1] || '') : '';

// 새 파일은 이 폴더 아래 것만 커밋한다 (같은 작업 트리의 임시 파일·다른 세션 산출물이 섞이지 않게)
const NEW_FILE_DIRS = /^(backend|frontend|docs|scripts|data|\.claude|\.githooks|\.github)\//;
const SECRET_FILE = /(^|\/)\.env(\.(?!example$)[^/]*)?$|\.(pem|key|p12|jks)$/;

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
const pcId = currentPc().id || 'unknown-pc';
const key = ownerKey(pcId);

// 이 브랜치 커밋: develop에 이미 들어간 커밋(상대가 머지한 것, develop 합치기 머지)은 뺀다. develop이 없으면 최근 8개
const base = ['origin/develop', 'develop'].find((b) => git(['rev-parse', '--verify', '-q', b]).ok);
const logArgs = base ? ['log', '--oneline', '--no-merges', '-15', `${base}..HEAD`] : ['log', '--oneline', '-8'];
const commits = hasCommits() ? git(logArgs).out.split('\n').filter(Boolean) : [];

const changed = git(['status', '--porcelain', '--untracked-files=all']).out.split('\n').filter(Boolean);

// 커밋할 새 파일: 작업 폴더 아래 것만. 비밀 파일로 보이는 것이 있으면 아무것도 쓰기 전에 멈춘다 (다시 실행해도 작업로그가 중복되지 않게)
const untracked = git(['ls-files', '--others', '--exclude-standard']).out.split('\n').filter(Boolean);
const newFiles = untracked.filter((f) => NEW_FILE_DIRS.test(f));
const skipped = untracked.filter((f) => !NEW_FILE_DIRS.test(f));
const tracked = git(['diff', '--name-only', 'HEAD']).out.split('\n').filter(Boolean);
const secret = [...newFiles, ...tracked].find((f) => SECRET_FILE.test(f));
if (secret) die(`비밀 파일로 보이는 ${secret} 이(가) 커밋 대상에 있어 중단했습니다. .gitignore를 확인하세요.`);

const nowFile = key ? nowFileOf(key) : null;
const nowMd = (nowFile && readText(nowFile)) || '';
const nowName = key ? nowRelPath(key) : 'NOW 파일';
const next = mdSection(nowMd, '바로 다음 할 일') || `(${nowName}에 없음)`;
const blocked = mdSection(nowMd, '막힌 점') || '없음';

const indent = (text) =>
  text
    .split(/\r?\n/)
    .filter((l) => l.trim())
    .map((l) => `  ${l.trim()}`)
    .join('\n');

const entry = [
  '',
  `## ${recordStamp({ author, pcId, branch })} (자동 handoff)`,
  `- 한 일 (이 브랜치 커밋):`,
  commits.length ? commits.map((c) => `  - ${c}`).join('\n') : '  - (커밋 없음)',
  `- 미커밋 변경 ${changed.length}개 → 이 handoff의 wip 커밋에 포함 (작업 폴더 밖의 새 파일은 제외)`,
  changed.length ? changed.slice(0, 15).map((c) => `  - \`${c.trim()}\``).join('\n') + (changed.length > 15 ? `\n  - … 외 ${changed.length - 15}개` : '') : '',
  `- 다음 할 일 (${nowName}):`,
  indent(next),
  `- 막힌 점: ${blocked
    .split(/\r?\n/)
    .map((l) => l.replace(/^\s*(?:[-*]|\d+\.)\s*/, '').trim())
    .filter(Boolean)
    .join(' / ')}`,
  memo ? `- 메모: ${memo}` : '',
  `- AI 활용: (있으면 이어받는 사람이 채움)`,
  `- 주의: 자동 기록이다. ${nowName} 본문이 오래되었을 수 있으니 이어받는 사람은 git diff로 마지막 변경을 확인할 것`,
  '',
]
  .filter((l) => l !== '')
  .join('\n');

const prev = readFileSync(WORKLOG_FILE, 'utf8');
appendFileSync(WORKLOG_FILE, (prev.endsWith('\n') ? '' : '\n') + entry + '\n', 'utf8');
console.log(`📝 작업로그에 항목을 추가했습니다.`);

if (nowFile && nowMd) {
  const head = `갱신: ${nowStamp()} · 작업자: ${author} · PC: ${pcId} · 브랜치: ${branch} (자동 handoff, 본문 미갱신)`;
  const updated = touchNowHeader(nowMd, head);
  writeFileSync(nowFile, updated, 'utf8');
  console.log(`📝 ${nowName}의 갱신 줄을 바꿨습니다.`);
} else if (!key) {
  console.warn('⚠️  PC가 등록되지 않아 NOW 파일을 정할 수 없습니다. node scripts/setup.mjs --pc <id>');
}

// 스테이징: 추적 중인 파일 변경 + 작업 폴더 아래 새 파일 (위에서 비밀 파일 검사를 마쳤다)
const addTracked = git(['add', '-u']);
if (!addTracked.ok) die(`git add -u 실패 (작업로그·NOW 변경은 커밋되지 않고 남아 있음):\n${addTracked.err}`);
if (newFiles.length) {
  const addNew = git(['add', '--', ...newFiles]);
  if (!addNew.ok) die(`새 파일 스테이징 실패 (작업로그·NOW 변경은 커밋되지 않고 남아 있음):\n${addNew.err}`);
}
const staged = git(['diff', '--cached', '--name-only']).out.split('\n').filter(Boolean);
const stagedSecret = staged.filter((f) => SECRET_FILE.test(f));
if (stagedSecret.length) {
  git(['reset', '-q', '--', ...stagedSecret]);
  die(`비밀 파일로 보이는 ${stagedSecret.join(', ')} 이(가) 스테이징되어 중단했습니다. .gitignore를 확인하세요.`);
}
if (skipped.length) console.log(`⏭  작업 폴더 밖의 새 파일 ${skipped.length}개는 커밋하지 않았습니다: ${skipped.slice(0, 5).join(', ')}${skipped.length > 5 ? ' …' : ''}`);

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
console.log(`\n이어받을 때(내 다른 PC 또는 상대): "git switch ${branch} && git pull" 후 Claude Code에서 /pickup`);
