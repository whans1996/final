// CI changes job: 직전 성공 이후 문서만 바뀐 push는 BE·FE를 건너뛴다 (HP-4 AC8, #224)
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';
import { compareFiles, decideChanges, isDocsOnly, lastSuccessSha } from '../lib/ci-changes.mjs';

const S = 's'.repeat(40);
const H = 'h'.repeat(40);
const base = { event: 'pull_request', action: 'synchronize', filter: { be: true, fe: true }, headSha: H, lastSuccess: S };
const docs = { status: 'ahead', files: ['docs/handoff/NOW-dh.md', 'docs/작업로그.md', 'docs/specs/HP-4-verify-ci-bugs.md'] };

test('HP-4 AC8: 직전 성공 S 이후 문서만 바뀐 synchronize는 be·fe false', () => {
  const r = decideChanges({ ...base, compare: docs });
  assert.equal(r.be, false);
  assert.equal(r.fe, false);
  assert.match(r.reason, new RegExp(`직전 성공 ${S.slice(0, 7)} 이후 문서만 변경`));
});

test('HP-4 AC8: openapi.json·문서 밖 파일이 섞이면, 직전 성공이 없거나 S가 조상이 아니면 PR 전체 diff 판별 그대로', () => {
  const keep = (over) => {
    const r = decideChanges({ ...base, ...over });
    return [r.be, r.fe];
  };
  assert.deepEqual(keep({ compare: { status: 'ahead', files: ['docs/a.md', 'docs/api/openapi.json'] } }), [true, true]);
  assert.deepEqual(keep({ compare: { status: 'ahead', files: ['docs/a.md', 'backend/A.java'] } }), [true, true]);
  assert.deepEqual(keep({ lastSuccess: null, compare: docs }), [true, true]);
  // 브랜치 재작성(S가 head의 조상 아님), 같은 커밋, 비교 실패
  assert.deepEqual(keep({ compare: { ...docs, status: 'diverged' } }), [true, true]);
  assert.deepEqual(keep({ compare: { ...docs, status: 'identical' } }), [true, true]);
  assert.deepEqual(keep({ compare: null }), [true, true]);
  // 비교 결과가 잘렸으면(파일 300개 넘음) 모두 보지 못했으므로 그대로
  assert.deepEqual(keep({ compare: { ...docs, truncated: true } }), [true, true]);
  // 원래 false인 영역은 false
  assert.deepEqual(keep({ filter: { be: false, fe: true }, compare: { status: 'ahead', files: ['frontend/a.vue'] } }), [false, true]);
});

test('HP-4 AC8: push(develop)·opened·reopened는 기존 판별 그대로', () => {
  for (const over of [{ event: 'push', action: '' }, { action: 'opened' }, { action: 'reopened' }, { event: 'workflow_dispatch', action: '' }]) {
    const r = decideChanges({ ...base, ...over, compare: docs });
    assert.deepEqual([r.be, r.fe], [true, true], JSON.stringify(over));
  }
});

test('HP-4 AC8: 문서만 = docs/ 아래이고 openapi.json이 아니며 하나 이상', () => {
  assert.equal(isDocsOnly(['docs/a.md', 'docs/산출물/b.md']), true);
  assert.equal(isDocsOnly(['docs/api/openapi.json']), false);
  assert.equal(isDocsOnly(['README.md']), false);
  assert.equal(isDocsOnly([]), false);
});

test('HP-4 AC8: 이름 바꾸기는 원래 경로도 바뀐 파일로 센다 (코드를 docs/로 옮겨도 건너뛰지 않음)', () => {
  const files = compareFiles([{ filename: 'docs/X.java', previous_filename: 'backend/src/main/java/X.java' }, { filename: 'docs/a.md' }]);
  assert.deepEqual(files, ['docs/X.java', 'backend/src/main/java/X.java', 'docs/a.md']);
  const r = decideChanges({ ...base, compare: { status: 'ahead', files } });
  assert.deepEqual([r.be, r.fe], [true, true]);
  assert.deepEqual(compareFiles(undefined), []);
  // ci-changes.mjs는 compare API의 previous_filename까지 받아 compareFiles로 편다
  const src = readFileSync(new URL('../ci-changes.mjs', import.meta.url), 'utf8');
  assert.match(src, /previous_filename/);
  assert.match(src, /compareFiles\(/);
});

test('HP-4 AC8: 직전 성공 = 지금 head가 아닌 가장 최근 성공 실행의 head', () => {
  const runs = [{ headSha: H }, { headSha: S }, { headSha: 'o'.repeat(40) }];
  assert.equal(lastSuccessSha(runs, H), S);
  assert.equal(lastSuccessSha([{ headSha: H }], H), null);
  assert.equal(lastSuccessSha([], H), null);
});

test('HP-4 AC8: ci.yml changes job이 ci-changes로 be·fe를 내보낸다', () => {
  const yml = readFileSync(new URL('../../.github/workflows/ci.yml', import.meta.url), 'utf8');
  assert.match(yml, /node scripts\/ci-changes\.mjs/);
  assert.match(yml, /be: \$\{\{ steps\.decide\.outputs\.be \}\}/);
  assert.match(yml, /fe: \$\{\{ steps\.decide\.outputs\.fe \}\}/);
});
