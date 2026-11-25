#!/usr/bin/env node
// PreToolUse(Bash|PowerShell) hook: 되돌리기 어려운 git 조작과 비밀 파일 접근을 막는다.
// 판정은 scripts/lib/guard.mjs(셸 토큰 파서, 테스트 있음)가 한다: 따옴표·heredoc 안 글자는 무시하고,
// `cd <경로> &&`·`git -C <경로>`·`bash -c "..."`·명령 치환 안의 명령까지 본다.
// 차단 시 exit 2 + stderr 메시지 → Claude에게 사유가 전달된다. 차단은 .harness/metrics/guard.jsonl에 남긴다 (주간 지표).
import { appendFileSync, existsSync, mkdirSync } from 'node:fs';
import { join } from 'node:path';
import { PROTECTED_BRANCHES, ROOT, currentBranch, hasCommits, isMerging, readStdinJson } from '../../scripts/lib/common.mjs';
import { checkCommand } from '../../scripts/lib/guard.mjs';

const input = readStdinJson();
const command = String(input?.tool_input?.command || '');
if (!command) process.exit(0);

const reason = checkCommand(command, {
  cwd: input.cwd || process.cwd(),
  // PowerShell 도구는 \가 경로 구분자이고 백틱이 이스케이프다
  shell: input.tool_name === 'PowerShell' ? 'powershell' : 'bash',
  exists: existsSync,
  // 보호 브랜치 직접 커밋 (첫 커밋, merge 진행 중은 예외)
  commitBlocked: (cwd) => {
    const branch = currentBranch(cwd);
    return branch && PROTECTED_BRANCHES.includes(branch) && hasCommits(cwd) && !isMerging(cwd) ? branch : null;
  },
});
if (!reason) process.exit(0);

try {
  const dir = join(ROOT, '.harness', 'metrics');
  mkdirSync(dir, { recursive: true });
  appendFileSync(join(dir, 'guard.jsonl'), JSON.stringify({ at: new Date().toISOString(), tool: input.tool_name, reason, command: command.slice(0, 300) }) + '\n');
} catch {
  // 기록 실패는 차단에 영향을 주지 않는다
}
console.error(`🚫 하네스 가드가 명령을 차단했습니다: ${reason}`);
process.exit(2);
