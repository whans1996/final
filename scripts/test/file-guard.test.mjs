// guard-files hook의 파일 도구 판정 (H-1 MCP 쓰기·읽기 도구, H-2 비밀값, HX-1 비밀 파일 읽기, HX-2 eval 경로)
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { checkFileTool } from '../lib/file-guard.mjs';

const DC = 'mcp__plugin_desktop-commander_desktop-commander__';
const base = {
  cwd: '/repo',
  root: '/repo',
  committedMigration: (rel) => rel.endsWith('V1__init.sql'),
  peerNowRel: 'docs/handoff/NOW-js.md',
};
const check = (tool, input, ctx = {}) => checkFileTool(tool, input, { ...base, ...ctx });
const no = (tool, input, ctx) => assert.ok(check(tool, input, ctx), `차단되어야 함: ${tool} ${JSON.stringify(input)}`);
const ok = (tool, input, ctx) => assert.equal(check(tool, input, ctx), null, `차단되면 안 됨: ${tool} ${JSON.stringify(input)}`);
const V1 = '/repo/backend/src/main/resources/db/migration/V1__init.sql';

test('쓰기: .env·커밋된 마이그레이션·상대 NOW (내장 도구와 MCP 도구 모두)', () => {
  no('Write', { file_path: '/repo/.env', content: 'x' });
  no('Edit', { file_path: V1, old_string: 'a', new_string: 'b' });
  no('MultiEdit', { file_path: '/repo/docs/handoff/NOW-js.md', edits: [{ old_string: 'a', new_string: 'b' }] });
  no(`${DC}write_file`, { path: '/repo/backend/.env.local', content: 'x' });
  no(`${DC}edit_block`, { file_path: 'docs/handoff/NOW-js.md', old_string: 'a', new_string: 'b' });
  no(`${DC}move_file`, { source: V1, destination: '/repo/x.sql' });
  no(`${DC}move_file`, { source: '/repo/a.md', destination: '/repo/docs/handoff/NOW-js.md' });
  no(`${DC}move_file`, { source: '/repo/.env', destination: '/repo/env.txt' });
  ok('Write', { file_path: '/repo/.env.example', content: 'GMS_API_KEY=' });
  ok(`${DC}write_file`, { path: '/repo/backend/src/main/resources/db/migration/V14__new.sql', content: 'select 1;' });
  ok(`${DC}edit_block`, { file_path: '/repo/docs/handoff/NOW-dh.md', old_string: 'a', new_string: 'b' });
  // 상대 NOW를 모르는 PC(미등록)는 그 검사를 건너뛴다
  ok('Edit', { file_path: '/repo/docs/handoff/NOW-js.md', old_string: 'a', new_string: 'b' }, { peerNowRel: null });
});

test('쓰기 내용의 비밀값 (H-2)', () => {
  no(`${DC}write_file`, { path: '/repo/a.yml', content: 'key: ' + 'ghp_' + 'a'.repeat(36) });
  no('Edit', { file_path: '/repo/a.md', old_string: 'x', new_string: 'https://meeting.ssafy.com/hooks/' + 'a1b2c3d4e5f6g7h8i9j0k1l2m3' });
  no('Write', { file_path: '/repo/a.txt', content: 'github_pat_' + 'A'.repeat(30) });
  no('Write', { file_path: '/repo/.env.example', content: 'GMS_API_KEY=' + 'x'.repeat(24) });
  no(`${DC}edit_block`, { file_path: '/repo/a.md', old_string: 'x', new_string: 'AIza' + 'q'.repeat(35) });
  ok('Write', { file_path: '/repo/.env.example', content: 'GMS_API_KEY=\n# GMS_BASE_URL=https://gms.ssafy.io/gmsapi/api.openai.com/v1' });
});

test('읽기·검색: 비밀 파일 (HX-1)', () => {
  no(`${DC}read_file`, { path: '/repo/.env' });
  no(`${DC}read_multiple_files`, { paths: ['/repo/README.md', '/repo/backend/.env.local'] });
  no('Read', { file_path: '/repo/certs/server.pem' });
  no('Grep', { pattern: 'KEY', path: '/repo/.env' });
  no('Grep', { pattern: 'KEY', glob: '.env*' });
  no(`${DC}start_search`, { path: '/repo', pattern: 'KEY', searchType: 'content', filePattern: '.env' });
  no(`${DC}get_file_info`, { path: '/repo/.env' });
  ok('Grep', { pattern: '\\.env', path: '/repo/scripts' });
  ok(`${DC}start_search`, { path: '/repo', pattern: '.env', searchType: 'files' });
  ok(`${DC}read_file`, { path: '/repo/.env.example' });
  ok(`${DC}read_file`, { path: 'https://example.com/a', isUrl: true });
  ok('Read', { file_path: '/repo/README.md' });
  ok('Glob', { pattern: '**/.env*' });
});

test('eval 경로: 김동환 PC(evalDir 지정)에서만 막는다 (HX-2)', () => {
  const ev = { evalDir: '/repo/data/raw/eval', exists: () => true };
  no('Read', { file_path: '/repo/data/raw/eval/sets.json' }, ev);
  no('Read', { file_path: 'data/raw/eval/sets.json' }, ev);
  no('Glob', { pattern: '**/*', path: '/repo/data/raw/eval' }, ev);
  no('Glob', { pattern: 'data/raw/eval/**' }, ev);
  no('Grep', { pattern: 'x', path: '/repo/data/raw/eval' }, ev);
  no(`${DC}list_directory`, { path: '/repo/data/raw/eval' }, ev);
  no(`${DC}write_file`, { path: '/repo/data/raw/eval/x.md', content: 'x' }, ev);
  // gitignore를 따르지 않는 MCP 검색이 상위 폴더에서 시작하면 eval 폴더가 있을 때만 막는다
  no(`${DC}start_search`, { path: '/repo', pattern: 'x', searchType: 'content' }, ev);
  ok(`${DC}start_search`, { path: '/repo', pattern: 'x', searchType: 'content' }, { ...ev, exists: () => false });
  ok(`${DC}start_search`, { path: '/repo/scripts', pattern: 'x', searchType: 'content' }, ev);
  ok('Grep', { pattern: 'x', path: '/repo' }, ev); // 내장 Grep은 .gitignore(data/raw/)를 따른다
  ok('Read', { file_path: '/repo/data/raw/README.md' }, ev);
  ok('Read', { file_path: '/repo/data/raw/eval/sets.json' }); // 김준섭 PC
});

test('모르는 도구는 통과', () => {
  ok('WebFetch', { url: 'https://x/.env' });
  ok(`${DC}list_processes`, {});
});
