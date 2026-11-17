#!/usr/bin/env node
// SessionStart hook: 릴레이 교대 직후에도 AI가 바로 상태를 알도록
// 브랜치, git 상태, 최근 커밋, NOW.md, 작업로그 마지막 항목을 컨텍스트에 주입한다.
import {
  NOW_FILE,
  PROTECTED_BRANCHES,
  WORKLOG_FILE,
  currentBranch,
  git,
  hasCommits,
  isGitRepo,
  readStdinJson,
  readText,
} from '../../scripts/lib/common.mjs';

const input = readStdinJson();
const parts = ['# 바이브노트 세션 컨텍스트 (SessionStart hook 자동 주입)'];
const warnings = [];

if (!isGitRepo()) {
  warnings.push('git 저장소가 아닙니다.');
} else {
  const branch = currentBranch() || '(detached)';
  parts.push(`## Git\n- 브랜치: ${branch}`);

  // 원격과 비교 (네트워크가 느리면 건너뜀)
  if (git(['remote']).out) {
    git(['fetch', '--quiet'], { timeout: 5000 });
    const ab = git(['rev-list', '--left-right', '--count', 'HEAD...@{u}']);
    if (ab.ok) {
      const [ahead, behind] = ab.out.split(/\s+/).map(Number);
      if (behind > 0) warnings.push(`원격보다 ${behind}커밋 뒤처져 있습니다. 작업 전에 git pull 하세요 (상대가 교대 후 push했을 수 있음).`);
      if (ahead > 0) parts.push(`- push 안 된 커밋: ${ahead}개`);
    }
  }

  const status = git(['status', '--short']).out.split('\n').filter(Boolean);
  parts.push(`- 미커밋 변경: ${status.length}개${status.length ? '\n' + status.slice(0, 20).map((s) => `  ${s}`).join('\n') : ''}`);
  if (hasCommits()) parts.push(`- 최근 커밋:\n${git(['log', '--oneline', '-5']).out.split('\n').map((l) => `  ${l}`).join('\n')}`);

  if (PROTECTED_BRANCHES.includes(branch) && hasCommits()) {
    warnings.push(`보호 브랜치(${branch})에 있습니다. 코드를 바꾸기 전에 feature/F-xx-slug 브랜치를 만드세요.`);
  }
  if (git(['config', 'core.hooksPath']).out !== '.githooks') {
    warnings.push('git hook이 연결되지 않았습니다. 사용자에게 `node scripts/setup.mjs` 실행을 요청하세요.');
  }
}

const now = readText(NOW_FILE);
parts.push(`## docs/handoff/NOW.md\n${now ? now.trim() : '(없음)'}`);

const log = readText(WORKLOG_FILE);
if (log) {
  const entries = log.split(/\r?\n(?=## )/);
  const last = entries[entries.length - 1];
  if (last && last.startsWith('## ')) parts.push(`## 작업로그 마지막 항목\n${last.trim()}`);
}

if (warnings.length) parts.splice(1, 0, `## ⚠️ 주의\n${warnings.map((w) => `- ${w}`).join('\n')}`);
parts.push(
  input.source === 'compact'
    ? '컨텍스트가 압축되었다. 위 NOW.md와 현재 spec을 기준으로 이어서 작업한다.'
    : '이어서 작업하려면 /pickup 을 사용한다. 작업 단위가 끝날 때마다 NOW.md를 갱신한다.',
);

process.stdout.write(
  JSON.stringify({
    hookSpecificOutput: { hookEventName: 'SessionStart', additionalContext: parts.join('\n\n') },
  }),
);
