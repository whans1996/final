// CI changes job의 be·fe 경로 필터 (HP-4 AC7, H-12).
// BE·FE job이 실제로 실행하는 하네스 파일(verify.mjs·seed-check.mjs와 그 import 닫힘)만 필터에 넣는다.
// 넓은 'scripts/**'는 하네스 스크립트만 바꾼 PR에서도 BE·FE 전체 CI를 돌렸다. 새 import가 생기면 이 테스트가 실패한다.
import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';
import { dirname, posix } from 'node:path';
import { test } from 'node:test';
import { fileURLToPath } from 'node:url';

const ROOT = posix.join(dirname(fileURLToPath(import.meta.url)).replace(/\\/g, '/'), '..', '..');
const read = (p) => readFileSync(posix.join(ROOT, p), 'utf8');

/** ci.yml changes job의 filters 블록 → { be: [...], fe: [...] } */
function ciFilters() {
  const yml = read('.github/workflows/ci.yml');
  const block = /filters: \|\n([\s\S]*?)\n\n/.exec(yml)?.[1] || '';
  const out = {};
  let cur = null;
  for (const line of block.split('\n')) {
    const key = /^\s+(\w+):\s*$/.exec(line);
    if (key) out[(cur = key[1])] = [];
    const item = /^\s+-\s+'([^']+)'/.exec(line);
    if (item && cur) out[cur].push(item[1]);
  }
  return out;
}

/** 상대 경로 import(static·dynamic)를 따라간 파일 집합 (저장소 기준 경로) */
function importClosure(entries) {
  const seen = new Set();
  const stack = [...entries];
  while (stack.length) {
    const f = stack.pop();
    if (seen.has(f)) continue;
    seen.add(f);
    const src = read(f);
    for (const m of src.matchAll(/(?:from\s+|import\s*\(\s*)['"](\.{1,2}\/[^'"]+)['"]/g)) {
      const dep = posix.normalize(posix.join(posix.dirname(f), m[1]));
      if (existsSync(posix.join(ROOT, dep))) stack.push(dep);
    }
  }
  return seen;
}

const globRe = (g) => new RegExp(`^${g.replace(/[.+^${}()|[\]\\]/g, '\\$&').replace(/\*\*/g, '\0').replace(/\*/g, '[^/]*').replace(/\0/g, '.*')}$`);
const matches = (patterns, f) => patterns.some((p) => globRe(p).test(f));

const BE_RUNS = importClosure(['scripts/verify.mjs']);
const FE_RUNS = importClosure(['scripts/verify.mjs', 'scripts/seed-check.mjs', 'scripts/test/seed-examples.test.mjs']);
const HARNESS_ONLY = ['scripts/roadmap.mjs', 'scripts/lib/peer.mjs', 'scripts/test/roadmap.test.mjs', '.claude/hooks/session-start.mjs', '.githooks/pre-push', '.github/workflows/roadmap.yml', '.github/workflows/codex-status.yml'];

test('HP-4 AC7: BE·FE job이 실행하는 하네스 파일(import 닫힘)은 모두 필터에 있다', () => {
  const { be, fe } = ciFilters();
  assert.ok(be?.length && fe?.length, 'ci.yml에서 be·fe 필터를 찾지 못함');
  for (const f of BE_RUNS) assert.ok(matches(be, f), `be 필터에 없음: ${f}`);
  for (const f of FE_RUNS) assert.ok(matches(fe, f), `fe 필터에 없음: ${f}`);
  for (const [name, list] of Object.entries({ be, fe })) assert.ok(list.includes('.github/workflows/ci.yml'), `${name}: ci.yml`);
});

test('HP-4 AC7: 필터의 scripts 항목은 실행하는 파일만 나열하고 하네스 전용 파일은 넣지 않는다', () => {
  const { be, fe } = ciFilters();
  for (const [name, list, runs] of [['be', be, BE_RUNS], ['fe', fe, FE_RUNS]]) {
    const scripts = list.filter((p) => p.startsWith('scripts/'));
    assert.deepEqual([...scripts].sort(), [...runs].sort(), `${name}: scripts 항목 = import 닫힘`);
    assert.ok(!list.some((p) => p.includes('*') && /^(scripts|\.github|\.claude)\//.test(p)), `${name}: 하네스 쪽 넓은 glob 금지`);
    for (const f of HARNESS_ONLY) assert.ok(!matches(list, f), `${name} 필터가 하네스 전용 파일을 포함: ${f}`);
  }
  // 앱 코드·시드 카드는 그대로
  assert.ok(matches(be, 'backend/src/main/java/A.java') && matches(be, 'data/seed/concepts/java/x.md') && matches(be, 'data/datasets.json'));
  assert.ok(matches(fe, 'frontend/src/A.vue') && matches(fe, 'data/seed/concepts/java/x.md') && matches(fe, 'backend/src/main/java/com/vibenote/analysis/concept/ConceptCategories.java'));
});
