// .claude/settings.json의 PreToolUse matcher가 가드할 도구를 모두 잡는가 (H-1: MCP 셸·파일 도구로 가드를 우회하던 구멍)
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { test } from 'node:test';
import { ROOT } from '../lib/common.mjs';

const settings = JSON.parse(readFileSync(join(ROOT, '.claude', 'settings.json'), 'utf8'));
const DC = 'mcp__plugin_desktop-commander_desktop-commander__';

/** 그 도구 이름에 걸리는 PreToolUse hook 스크립트 이름들 (matcher는 전체 일치로 본다) */
const hooksFor = (tool) =>
  (settings.hooks?.PreToolUse || [])
    .filter((e) => new RegExp(`^(?:${e.matcher})$`).test(tool))
    .flatMap((e) => e.hooks.map((h) => /hooks\/([\w-]+)\.mjs/.exec(h.command)?.[1]));

test('셸 도구는 guard-bash, 파일 도구는 guard-files가 받는다 (내장·MCP)', () => {
  for (const tool of ['Bash', 'PowerShell', `${DC}start_process`, `${DC}interact_with_process`]) assert.deepEqual(hooksFor(tool), ['guard-bash'], tool);
  for (const tool of ['Edit', 'Write', 'MultiEdit', 'NotebookEdit', 'Read', 'Grep', 'Glob', ...['write_file', 'write_pdf', 'edit_block', 'move_file', 'read_file', 'read_multiple_files', 'start_search', 'list_directory', 'get_file_info'].map((t) => DC + t)])
    assert.deepEqual(hooksFor(tool), ['guard-files'], tool);
  // 가드할 필요 없는 도구에는 걸지 않는다 (hook마다 node 프로세스가 뜬다)
  for (const tool of ['WebFetch', 'TodoWrite', `${DC}list_processes`, `${DC}read_process_output`]) assert.deepEqual(hooksFor(tool), [], tool);
});

test('settings.json의 hook 스크립트가 실제로 있다', () => {
  const commands = Object.values(settings.hooks || {}).flatMap((entries) => entries.flatMap((e) => e.hooks.map((h) => h.command)));
  for (const c of commands) {
    const rel = /\$CLAUDE_PROJECT_DIR\/([^"]+)/.exec(c)?.[1];
    assert.ok(rel, c);
    assert.doesNotThrow(() => readFileSync(join(ROOT, rel)), rel);
  }
});

test('statusLine 스크립트가 실제로 있고 git·gh 없이 그린다 (HX-6)', () => {
  const rel = /\}\/([^"]+)/.exec(settings.statusLine?.command ?? '')?.[1];
  assert.ok(rel, settings.statusLine?.command);
  assert.doesNotThrow(() => readFileSync(join(ROOT, rel)), rel);
  // 표시줄은 assistant 메시지마다 돈다. 외부 명령을 띄우는 것은 백그라운드 새로 고침뿐이다
  const src = readFileSync(join(ROOT, rel), 'utf8');
  assert.doesNotMatch(src, /spawnSync|execSync|execFileSync|\bgit\(/);
});

test('permissions.deny가 비밀 파일 읽기를 도구 수준에서 한 겹 더 막고, .env.example은 막지 않는다 (#249)', async () => {
  const { matchesGlob } = await import('node:path/posix');
  const { SECRET_SAMPLES } = await import('../lib/protect.mjs');
  const deny = settings.permissions?.deny || [];
  const readGlobs = deny.map((r) => /^Read\((.+)\)$/.exec(r)?.[1]).filter(Boolean);
  const denied = (path) => readGlobs.some((g) => matchesGlob(path, g));
  for (const name of SECRET_SAMPLES) assert.ok(denied(`backend/${name}`), name);
  // 저장소 밖으로 옮긴 .env (#249)
  assert.ok(deny.includes('Read(~/.vibenote/**)'));
  assert.ok(deny.includes('Edit(~/.vibenote/**)'));
  // deny는 allow보다 먼저 적용되므로 .env.* 같은 넓은 규칙을 두면 예시 파일까지 못 읽는다
  assert.ok(!denied('.env.example') && !denied('backend/.env.example'));
});
