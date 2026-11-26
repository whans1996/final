#!/usr/bin/env node
// 상태 표시줄 (HX-6, .claude/settings.json statusLine). Claude Code가 stdin으로 세션 정보(JSON)를 주고, stdout 줄마다 한 행으로 보인다.
// assistant 메시지마다 다시 돌므로 git·gh를 부르지 않고 파일만 읽는다 (scripts/lib/status.mjs).
// PR·CI·로드맵 캐시가 오래되면 scripts/status-refresh.mjs를 백그라운드로 띄우고, 이번에는 있는 캐시로 그린다.
// 어떤 오류도 빈 표시줄로 끝나지 않게 최소 한 줄은 낸다.
import { spawn } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { ROOT, localPcId, ownerKey, readStdinJson } from '../../scripts/lib/common.mjs';

let out = '[바이브노트]';
try {
  const { loadPcs } = await import('../../scripts/lib/env-check.mjs');
  const { seoulToday } = await import('../../scripts/lib/roadmap.mjs');
  const status = await import('../../scripts/lib/status.mjs');

  const input = readStdinJson();
  const id = localPcId();
  const pcs = loadPcs();
  const pc = id && pcs[id] ? { id, owner: pcs[id].owner, key: ownerKey(id) } : { id: null };
  const branch = status.gitBranchAt(ROOT);
  const cache = status.readStatus();
  // codex-loop hook(Stop)이 브랜치마다 남긴 마지막 판정
  let codex = null;
  try {
    codex = branch ? (JSON.parse(readFileSync(status.CODEX_LOOP_FILE, 'utf8'))[branch] ?? null) : null;
  } catch {
    codex = null;
  }
  out = status.statusLines({ input, pc, branch, cache, codex, today: seoulToday() }).join('\n');

  if (pc.id && branch && status.needsRefresh(cache, branch)) {
    status.writeStatus({ refreshingAt: Date.now() });
    const child = spawn(process.execPath, [join(ROOT, 'scripts', 'status-refresh.mjs')], { cwd: ROOT, detached: true, stdio: 'ignore', windowsHide: true });
    child.on('error', () => {});
    child.unref();
  }
} catch {
  // 표시줄은 부가 기능이다. 실패하면 이름만 보인다
}
process.stdout.write(`${out}\n`);
