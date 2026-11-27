#!/usr/bin/env node
// PreToolUse(Bash|PowerShell|Desktop Commander start_process·interact_with_process) hook:
// 되돌리기 어려운 git 조작, 머지·저장소 설정 우회, 비밀 파일 접근, 김동환 PC의 eval 열람을 막는다.
// 판정은 scripts/lib/guard.mjs(셸 토큰 파서, 테스트 있음)가 한다: 따옴표·heredoc 안 글자는 무시하고,
// `cd <경로> &&`·`git -C <경로>`·`bash -c "..."`·명령 치환·`node -e` 코드 안의 명령까지 본다.
// MCP 셸 도구도 같은 판정을 받는다 (H-1: 예전에는 이 도구로 모든 가드를 우회할 수 있었다).
// 단, Desktop Commander는 Claude의 cwd가 아니라 MCP 서버 쪽 위치에서 실행하므로 상대 경로 판정(보호 브랜치·eval)은 근사다.
// 차단 시 exit 2 + stderr 메시지 → Claude에게 사유가 전달된다. 차단은 .harness/metrics/guard.jsonl에 사유만 남긴다 (주간 지표).
// 명령 원문은 남기지 않는다: 차단된 명령에 비밀값이 들어 있을 수 있다 (#135).
import { appendFileSync, existsSync, mkdirSync, statSync } from 'node:fs';
import { join } from 'node:path';
import { PROTECTED_BRANCHES, ROOT, currentBranch, git, hasCommits, isMerging, localPcId, ownerKey, readStdinJson } from '../../scripts/lib/common.mjs';
import { checkTool } from '../../scripts/lib/guard.mjs';
import { EVAL_DIR_REL, evalBlockedFor, isSecretFile, isWithin } from '../../scripts/lib/protect.mjs';

// 저장소 안 비밀 파일 [{ path, ignored }] (#188 재귀 검색 판정). 재귀 검색일 때만 부르고 한 번만 조회한다 (약 0.1초).
// 무시된 폴더(node_modules 등)는 --directory로 접어 그 안은 보지 않는다. 조회가 실패하면 루트 .env가 있다고 본다
let secrets = null;
function repoSecrets() {
  if (secrets) return secrets;
  const list = (args, ignored) => {
    const r = git(['ls-files', '-z', ...args]);
    if (!r.ok) return null;
    return r.out
      .split('\0')
      .filter((p) => p && !p.endsWith('/') && isSecretFile(p))
      .map((p) => ({ path: join(ROOT, p), ignored }));
  };
  const parts = [list([], false), list(['--others', '--exclude-standard'], false), list(['--others', '--ignored', '--exclude-standard', '--directory'], true)];
  secrets = parts.some((p) => p === null) ? [{ path: join(ROOT, '.env'), ignored: true }] : parts.flat();
  return secrets;
}

const input = readStdinJson();
const reason = checkTool(input?.tool_name, input?.tool_input || {}, {
  cwd: input?.cwd || process.cwd(),
  exists: existsSync,
  isDir: (p) => {
    try {
      return statSync(p).isDirectory();
    } catch {
      return false;
    }
  },
  // 검색 폴더 아래 비밀 파일. 저장소를 품는 폴더(홈·드라이브 루트)면 전부, 저장소 밖은 모른다 (#188)
  secretFiles: (dir) => (isWithin(ROOT, dir) ? repoSecrets() : repoSecrets().filter((f) => isWithin(f.path, dir))),
  // eval 블라인드 (HX-2): 김동환 PC에서만 data/raw/eval 접근을 막는다
  evalDir: evalBlockedFor(ownerKey(localPcId())) ? join(ROOT, EVAL_DIR_REL) : null,
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
  appendFileSync(join(dir, 'guard.jsonl'), JSON.stringify({ at: new Date().toISOString(), tool: input.tool_name, reason }) + '\n');
} catch {
  // 기록 실패는 차단에 영향을 주지 않는다
}
console.error(`🚫 하네스 가드가 명령을 차단했습니다: ${reason}`);
process.exit(2);
